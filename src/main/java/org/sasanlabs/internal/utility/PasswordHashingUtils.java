package org.sasanlabs.internal.utility;

import org.springframework.security.crypto.password.Md4PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHashingUtils {

    private static final String HASH_SEPARATOR = ":";
    private static final String HASH_ALGORITHM = "SHA-256";

    //
    private static final Md4PasswordEncoder MD4_ENCODER = new Md4PasswordEncoder();

    private PasswordHashingUtils() {}
    /**
     * Caesar Cipher shifts alphabetic characters positions to the right overflowing to the beginning of the alphabet.
     */
    public static String caesarCipher(String rawPassword, int shift){
        StringBuilder builder = new StringBuilder();
        for (char ch : rawPassword.toCharArray()) {
            if (Character.isLetter(ch)) {
                char base = Character.isUpperCase(ch) ? 'A' : 'a';
                builder.append((char) ((ch - base + shift) % 26 + base));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String md4Hash(String rawPassword){
       return MD4_ENCODER.encode(rawPassword);
    }

    public static boolean isValidSaltedSha256(String rawPassword, String storedPassword) {
        if (storedPassword == null || rawPassword == null) {
            return false;
        }

        String[] saltAndHash = storedPassword.split(HASH_SEPARATOR, 2);
        if (saltAndHash.length != 2) {
            // Backward compatibility for old plaintext test data.
            return storedPassword.equals(rawPassword);
        }

        String calculatedHash = sha256Hex(saltAndHash[0], rawPassword);
        return saltAndHash[1].equalsIgnoreCase(calculatedHash);
    }

    public static String sha256Hex(String salt, String rawPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digest =
                    messageDigest.digest((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to compute password hash", exception);
        }
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte value : data) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
