package java.util;
public interface List<E> extends Collection<E> {
    E get(int index);
    int indexOf(Object o);
    List<E> subList(int fromIndex, int toIndex);
}
