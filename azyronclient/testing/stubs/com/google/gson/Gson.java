package com.google.gson;
import java.io.Reader;
import java.io.Writer;
public class Gson {
    public <T> T fromJson(Reader json, Class<T> classOfT) { return null; }
    public void toJson(Object src, Writer writer) {}
    public String toJson(Object src) { return "{}"; }
}
