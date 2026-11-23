package com.jiggneshgaming.azyron.util;

import com.jiggneshgaming.azyron.AzyronConstants;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AZYRON CLIENT V2 — update checker.
 *
 * <p>Checks the official GitHub release for a newer AZYRON CLIENT tag.
 * Runs entirely off-thread; results are cached and read by the UI
 * non-blockingly. This class NEVER downloads or installs anything — it
 * only reports whether an update exists and where to get it (the release
 * page). The user decides; nothing is fetched beyond one small API call.</p>
 */
public final class UpdateChecker {

    public enum State {
        IDLE,
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        ERROR
    }

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("azyronclient/updatecheck");

    /** Official releases endpoint — always ask the user to confirm on the page. */
    public static final String RELEASES_PAGE = "https://github.com/blazemc125/AzyronClient/releases";
    private static final String LATEST_API = "https://api.github.com/repos/blazemc125/AzyronClient/releases/latest";
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private static volatile State state = State.IDLE;
    private static volatile String latestVersion = "";
    private static volatile String detail = "";
    private static volatile long lastCheckedAt;

    private UpdateChecker() {
    }

    public static State state() {
        return state;
    }

    public static String latestVersion() {
        return latestVersion;
    }

    public static String detail() {
        return detail;
    }

    public static long lastCheckedAt() {
        return lastCheckedAt;
    }

    /** Kick off an asynchronous check. Re-entrant; only one check runs at a time. */
    public static void checkAsync() {
        if (state == State.CHECKING) {
            return;
        }
        state = State.CHECKING;
        detail = "Checking for updates…";
        Thread thread = new Thread(UpdateChecker::doCheck, "azyron-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private static void doCheck() {
        long start = System.currentTimeMillis();
        HttpClient client = null;
        try {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_API))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "azyronclient-update-checker")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                state = State.UP_TO_DATE;
                latestVersion = AzyronConstants.VERSION;
                detail = "No published release yet — you are running a pre-release build.";
                lastCheckedAt = System.currentTimeMillis();
                LOGGER.info("Update check: no releases found ({} ms)", System.currentTimeMillis() - start);
                return;
            }
            if (response.statusCode() != 200) {
                state = State.ERROR;
                detail = "Update check failed (HTTP " + response.statusCode() + ").";
                lastCheckedAt = System.currentTimeMillis();
                return;
            }
            Matcher tagMatcher = TAG_PATTERN.matcher(response.body());
            if (!tagMatcher.find()) {
                state = State.ERROR;
                detail = "Update check: unexpected response (no tag_name).";
                lastCheckedAt = System.currentTimeMillis();
                return;
            }
            String tag = tagMatcher.group(1);
            Matcher versionMatcher = VERSION_PATTERN.matcher(tag);
            if (!versionMatcher.find()) {
                state = State.ERROR;
                detail = "Update check: could not parse tag \"" + tag + "\".";
                lastCheckedAt = System.currentTimeMillis();
                return;
            }
            latestVersion = versionMatcher.group();
            int comparison = compareVersions(latestVersion, AzyronConstants.VERSION);
            if (comparison > 0) {
                state = State.UPDATE_AVAILABLE;
                detail = "Update available: v" + latestVersion + " (you have v" + AzyronConstants.VERSION + ")";
            } else {
                state = State.UP_TO_DATE;
                detail = "You are on the latest version (v" + AzyronConstants.VERSION + ").";
            }
            lastCheckedAt = System.currentTimeMillis();
            LOGGER.info("Update check: {} ({} ms)", detail, System.currentTimeMillis() - start);
        } catch (java.net.http.HttpConnectTimeoutException | java.net.SocketTimeoutException t) {
            state = State.ERROR;
            detail = "Update check timed out — check your connection.";
            lastCheckedAt = System.currentTimeMillis();
        } catch (Throwable t) {
            state = State.ERROR;
            detail = "Update check failed: " + t.getClass().getSimpleName();
            lastCheckedAt = System.currentTimeMillis();
            LOGGER.debug("Update check failed", t);
        } finally {
            if (client != null) {
                // Close idle keep-alive connections so nothing lingers.
                try {
                    // java.net.http has no close() on the client; dropping the reference
                    // lets the daemon thread exit and GC reclaim it.
                } catch (Throwable ignored) {
                    // nothing to do
                }
            }
        }
    }

    /** -1 when a < b, 0 when equal, 1 when a > b. Tolerates missing segments. */
    static int compareVersions(String a, String b) {
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        int length = Math.max(aParts.length, bParts.length);
        for (int i = 0; i < length; i++) {
            int aPart = i < aParts.length ? parseOrZero(aParts[i]) : 0;
            int bPart = i < bParts.length ? parseOrZero(bParts[i]) : 0;
            if (aPart != bPart) {
                return aPart < bPart ? -1 : 1;
            }
        }
        return 0;
    }

    private static int parseOrZero(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    /** One-line status for the UI, derived from the cached state. */
    public static String statusLine() {
        switch (state) {
            case CHECKING:
                return "Checking for updates…";
            case UP_TO_DATE:
                return detail;
            case UPDATE_AVAILABLE:
                return detail + " — tap “Get update” to open the release page. Nothing downloads automatically.";
            case ERROR:
                return detail;
            default:
                return "Current version: v" + AzyronConstants.VERSION + " (MC " + AzyronConstants.MINECRAFT_VERSION + ")";
        }
    }
}
