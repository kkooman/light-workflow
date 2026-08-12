package com.kkooman.lightworkflow.service;

public interface TokenStore {
    void store(String token);
    boolean exists(String token);
    void remove(String token);
}
