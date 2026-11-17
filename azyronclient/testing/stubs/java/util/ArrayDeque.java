package java.util;
public class ArrayDeque<E> implements Deque<E> {
    public ArrayDeque() {}
    public int size() { return 0; }
    public boolean isEmpty() { return false; }
    public boolean contains(Object o) { return false; }
    public Iterator<E> iterator() { return null; }
    public boolean add(E e) { return false; }
    public boolean remove(Object o) { return false; }
    public void clear() {}
    public void addFirst(E e) {}
    public void addLast(E e) {}
    public E peekFirst() { return null; }
    public E peekLast() { return null; }
    public E pollFirst() { return null; }
    public E pollLast() { return null; }
    public E removeFirst() { return null; }
    public E removeLast() { return null; }
}
