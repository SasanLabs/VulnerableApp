package org.sasanlabs.internal.utility;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class EncryptionUtils {
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

    public static SecretKey getKeyFromPassword(String password) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] emptySalt = new byte[16];
            KeySpec spec = new PBEKeySpec(password.toCharArray(), emptySalt, 1, 128);

            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error generating AES key from password: " + e.getMessage(), e);
        }
    }

    public static String encrypt(String plaintext, SecretKey key) {
        try {
            // VULNERABILITY NOTE: ECB mode does not use an IV and reveals patterns (CWE-327)
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encrypted);

        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Critical: Cryptographic configuration error", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("The provided key is invalid for AES encryption", e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Encryption failed due to block size or padding issues", e);
        }
    }

}

