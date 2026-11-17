package java.util;
public interface Map<K, V> {
    V put(K key, V value);
    V get(Object key);
    boolean containsKey(Object key);
    boolean containsValue(Object value);
    V remove(Object key);
    int size();
    boolean isEmpty();
    void clear();
    Set<K> keySet();
    Collection<V> values();
    Set<Entry<K, V>> entrySet();
    interface Entry<K, V> {
        K getKey();
        V getValue();
    }
}
