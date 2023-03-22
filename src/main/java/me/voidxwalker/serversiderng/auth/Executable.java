package me.voidxwalker.serversiderng.auth;

public interface Executable<T> {
    T get() throws Exception;
}
