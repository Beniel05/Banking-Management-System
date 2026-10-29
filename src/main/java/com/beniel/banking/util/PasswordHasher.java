package com.beniel.banking.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Prevent instantiation
    }

    /**
     * Hashes a valid four-digit numeric PIN.
     *
     * @param pin the PIN as a character array
     * @return iterations$salt$derivedKey
     */
    public static String hash(char[] pin) {

        if (!isValidPin(pin)) {
            if (pin != null) {
                Arrays.fill(pin, '\0');
            }

            throw new IllegalArgumentException(
                    "PIN must contain exactly 4 digits."
            );
        }

        byte[] salt = new byte[SALT_BYTES];
        byte[] derivedKey = null;

        try {
            RANDOM.nextBytes(salt);

            derivedKey = derive(pin, salt, ITERATIONS);

            return ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(derivedKey);

        } finally {
            Arrays.fill(pin, '\0');
            Arrays.fill(salt, (byte) 0);

            if (derivedKey != null) {
                Arrays.fill(derivedKey, (byte) 0);
            }
        }
    }

    /**
     * Verifies a supplied PIN against its stored hash.
     *
     * @param pin the PIN to verify
     * @param storedHash the previously stored hash
     * @return true if the PIN matches; otherwise false
     */
    public static boolean verify(char[] pin, String storedHash) {

        if (pin == null) {
            return false;
        }

        byte[] salt = null;
        byte[] expected = null;
        byte[] actual = null;

        try {
            if (!isValidPin(pin)) {
                return false;
            }

            if (storedHash == null) {
                return false;
            }

            String[] parts = storedHash.split("\\$", -1);

            if (parts.length != 3) {
                throw new IllegalStateException(
                        "Stored PIN hash is malformed."
                );
            }

            int iterations = Integer.parseInt(parts[0]);

            salt = Base64.getDecoder().decode(parts[1]);
            expected = Base64.getDecoder().decode(parts[2]);

            if (iterations < 1 || iterations > 1000000
                    || salt.length != SALT_BYTES
                    || expected.length != KEY_BITS / 8) {

                throw new IllegalStateException(
                        "Stored PIN hash is malformed."
                );
            }

            actual = derive(pin, salt, iterations);

            return MessageDigest.isEqual(expected, actual);

        } catch (IllegalArgumentException e) {

            throw new IllegalStateException(
                    "Stored PIN hash is malformed.", e
            );

        } finally {
            Arrays.fill(pin, '\0');

            if (salt != null) {
                Arrays.fill(salt, (byte) 0);
            }

            if (expected != null) {
                Arrays.fill(expected, (byte) 0);
            }

            if (actual != null) {
                Arrays.fill(actual, (byte) 0);
            }
        }
    }

    /**
     * Checks whether the supplied PIN contains exactly four digits.
     */
    private static boolean isValidPin(char[] pin) {

        if (pin == null || pin.length != 4) {
            return false;
        }

        for (char digit : pin) {
            if (digit < '0' || digit > '9') {
                return false;
            }
        }

        return true;
    }

    /**
     * Derives a cryptographic key from a PIN and salt.
     */
    private static byte[] derive(
            char[] pin,
            byte[] salt,
            int iterations) {

        PBEKeySpec spec = new PBEKeySpec(
                pin,
                salt,
                iterations,
                KEY_BITS
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(ALGORITHM);

            return factory.generateSecret(spec).getEncoded();

        } catch (GeneralSecurityException e) {

            throw new IllegalStateException(
                    "Could not process PIN securely.", e
            );

        } finally {
            spec.clearPassword();
        }
    }
}