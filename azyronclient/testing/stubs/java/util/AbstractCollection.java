package java.util;
public abstract class AbstractCollection<E> implements Collection<E> {
    protected AbstractCollection() {}
    public boolean isEmpty() { return size() == 0; }
    public boolean contains(Object o) { return false; }
    public boolean add(E e) { return false; }
    public boolean remove(Object o) { return false; }
    public void clear() {}
}
