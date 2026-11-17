package org.slf4j;
public final class LoggerFactory {
    private LoggerFactory() {}
    public static Logger getLogger(String name) { return new NoopLogger(); }
    public static Logger getLogger(Class<?> type) { return new NoopLogger(); }
    static final class NoopLogger implements Logger {
        public void trace(String msg, Object... args) {}
        public void debug(String msg, Object... args) {}
        public void info(String msg, Object... args) {}
        public void warn(String msg, Object... args) {}
        public void error(String msg, Object... args) {}
        public void trace(String msg, Throwable t) {}
        public void debug(String msg, Throwable t) {}
        public void info(String msg, Throwable t) {}
        public void warn(String msg, Throwable t) {}
        public void error(String msg, Throwable t) {}
    }
}
