package java.util;
public interface Deque<E> extends Collection<E> {
    void addFirst(E e);
    void addLast(E e);
    E peekFirst();
    E peekLast();
    E pollFirst();
    E pollLast();
    E removeFirst();
    E removeLast();
}
