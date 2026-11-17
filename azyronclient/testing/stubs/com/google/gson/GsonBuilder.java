package com.google.gson;
public class GsonBuilder {
    public GsonBuilder setPrettyPrinting() { return this; }
    public GsonBuilder disableHtmlEscaping() { return this; }
    public GsonBuilder excludeFieldsWithoutExposeAnnotation() { return this; }
    public Gson create() { return new Gson(); }
}
