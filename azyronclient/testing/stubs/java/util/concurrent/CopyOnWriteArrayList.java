package java.util.concurrent;
import java.util.ArrayList;
import java.util.Collection;
public class CopyOnWriteArrayList<E> extends ArrayList<E> {
    public CopyOnWriteArrayList() {}
    public CopyOnWriteArrayList(Collection<? extends E> c) { super(c); }
}
