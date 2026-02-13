package com.syntaxnow.identity.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        byte[] bytes = new byte[32]; // 256-bit
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
