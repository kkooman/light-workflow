package com.kkooman.lightworkflow.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InMemoryTokenStore implements TokenStore {

    private final Set<String> tokens = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void store(String token) {
        tokens.add(token);
    }

    @Override
    public boolean exists(String token) {
        return tokens.contains(token);
    }

    @Override
    public void remove(String token) {
        tokens.remove(token);
    }
}
