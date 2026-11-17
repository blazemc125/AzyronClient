package org.slf4j;
public interface Logger {
    void trace(String msg, Object... args);
    void debug(String msg, Object... args);
    void info(String msg, Object... args);
    void warn(String msg, Object... args);
    void error(String msg, Object... args);
    void trace(String msg, Throwable t);
    void debug(String msg, Throwable t);
    void info(String msg, Throwable t);
    void warn(String msg, Throwable t);
    void error(String msg, Throwable t);
}
