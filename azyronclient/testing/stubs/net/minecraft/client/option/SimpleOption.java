package net.minecraft.client.option;
public class SimpleOption<T> {
    private T value;
    public SimpleOption() {}
    public SimpleOption(T initial) { this.value = initial; }
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
}
