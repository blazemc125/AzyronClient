package com.jiggneshgaming.azyron.core.cps;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * AZYRON CLIENT — CPS (clicks-per-second) tracking.
 *
 * <p>Records press timestamps per logical button and reports a rolling
 * 1-second CPS. Pure Java, dependency-free and unit-testable.</p>
 */
public final class CpsTracker {

    /** Rolling window length in milliseconds. */
    public static final long WINDOW_MS = 1000L;

    private final Deque<Long> leftPresses = new ArrayDeque<Long>();
    private final Deque<Long> rightPresses = new ArrayDeque<Long>();
    private final Deque<Long> middlePresses = new ArrayDeque<Long>();

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int MIDDLE = 2;

    /** Record a press for the given button index (LEFT / RIGHT / MIDDLE). */
    public void press(int button, long nowMs) {
        dequeFor(button).addLast(Long.valueOf(nowMs));
        prune(nowMs);
    }

    /** Rolling CPS for the given button at time {@code nowMs}. */
    public int cps(int button, long nowMs) {
        prune(nowMs);
        return dequeFor(button).size();
    }

    /** Remove all recorded presses (e.g. on world disconnect). */
    public void clear() {
        leftPresses.clear();
        rightPresses.clear();
        middlePresses.clear();
    }

    /** Drop presses that have fallen outside the rolling window. */
    private void prune(long nowMs) {
        pruneDeque(leftPresses, nowMs);
        pruneDeque(rightPresses, nowMs);
        pruneDeque(middlePresses, nowMs);
    }

    private static void pruneDeque(Deque<Long> deque, long nowMs) {
        while (!deque.isEmpty() && nowMs - deque.peekFirst().longValue() > WINDOW_MS) {
            deque.pollFirst();
        }
    }

    private Deque<Long> dequeFor(int button) {
        switch (button) {
            case LEFT:
                return leftPresses;
            case RIGHT:
                return rightPresses;
            case MIDDLE:
                return middlePresses;
            default:
                throw new IllegalArgumentException("Unknown button: " + button);
        }
    }
}
