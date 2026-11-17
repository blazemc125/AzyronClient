package java.nio.charset;
public class Charset {
    private final String name;
    protected Charset(String name) { this.name = name; }
    public String name() { return name; }
}
