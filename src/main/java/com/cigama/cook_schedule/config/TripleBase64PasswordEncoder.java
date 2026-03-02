package com.cigama.cook_schedule.config;

import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Base64;

public class TripleBase64PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        String encoded = rawPassword.toString();
        for (int i = 0; i < 3; i++) {
            encoded = Base64.getEncoder().encodeToString(encoded.getBytes());
        }
        return encoded;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
