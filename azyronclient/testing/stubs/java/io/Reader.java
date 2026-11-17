package java.io;
public abstract class Reader implements AutoCloseable {
    public int read() throws IOException { return -1; }
    public void close() throws IOException {}
}
