package java.nio.file;
import java.io.IOException;
public class AtomicMoveNotSupportedException extends IOException {
    public AtomicMoveNotSupportedException(String source, String target, String reason) { super(reason); }
}
