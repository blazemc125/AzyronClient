package java.util;
public final class Locale {
    public static final Locale ROOT = new Locale("");
    public static final Locale ENGLISH = new Locale("en");
    private final String language;
    public Locale(String language) { this.language = language; }
    public String getLanguage() { return language; }
    public String toString() { return language; }
}
