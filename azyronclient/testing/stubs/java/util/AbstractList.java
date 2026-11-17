package java.util;
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected AbstractList() {}
    public abstract E get(int index);
    public abstract int size();
    public E set(int index, E element) { return null; }
    public void add(int index, E element) {}
    public E remove(int index) { return null; }
    public List<E> subList(int fromIndex, int toIndex) { return new ArrayList<E>(); }
    public Iterator<E> iterator() { return new Iterator<E>() {
        @Override public boolean hasNext() { return false; }
        @Override public E next() { return null; }
        @Override public void remove() {}
    }; }
}
