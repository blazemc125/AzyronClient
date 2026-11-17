package java.io;
public abstract class Writer implements AutoCloseable {
    public void write(int c) throws IOException {}
    public void write(String s) throws IOException {}
    public void flush() throws IOException {}
    public void close() throws IOException {}
}
