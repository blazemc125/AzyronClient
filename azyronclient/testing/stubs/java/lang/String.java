package java.lang;
import java.util.Locale;
public final class String implements CharSequence {
    public int length() { return 0; }
    public char charAt(int index) { return '\0'; }
    public CharSequence subSequence(int start, int end) { return substring(start, end); }
    public String() {}
    public boolean isEmpty() { return false; }
    public String substring(int beginIndex) { return null; }
    public String substring(int beginIndex, int endIndex) { return null; }
    public int indexOf(int ch) { return -1; }
    public int indexOf(String str) { return -1; }
    public boolean contains(CharSequence s) { return false; }
    public boolean startsWith(String prefix) { return false; }
    public boolean endsWith(String suffix) { return false; }
    public String trim() { return null; }
    public String toLowerCase() { return null; }
    public String toUpperCase() { return null; }
    public String toLowerCase(Locale locale) { return null; }
    public String toUpperCase(Locale locale) { return null; }
    public String replace(char oldChar, char newChar) { return null; }
    public String replace(CharSequence target, CharSequence replacement) { return null; }
    public boolean equals(Object anObject) { return false; }
    public int hashCode() { return 0; }
    public String toString() { return null; }
    public static String valueOf(int i) { return null; }
    public static String valueOf(long l) { return null; }
    public static String valueOf(double d) { return null; }
    public static String valueOf(boolean b) { return null; }
    public static String valueOf(Object obj) { return null; }
    public static String format(String format, Object... args) { return null; }
    public static String format(Locale locale, String format, Object... args) { return null; }
    public int lastIndexOf(int ch) { return -1; }
    public int lastIndexOf(String str) { return -1; }
    public String[] split(String regex) { return new String[0]; }
}
