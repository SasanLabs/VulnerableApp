package org.sasanlabs.internal.utility;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.sasanlabs.internal.utility.exception.EncryptionException;

/** This class contains methods related to encryption. */
public class EncryptionUtils {

    private EncryptionUtils() {}

    /**
     * INSECURE: Caesar Cipher shifts alphabetic characters positions to the right overflowing to
     * the beginning of the alphabet. 'z' will shift to 'a' and so on.
     *
     * @param rawPassword plaintext password to encrypt
     * @param shift how many shifts right
     */
    public static String caesarCipher(String rawPassword, int shift) throws EncryptionException {

        if (rawPassword == null) {
            throw new EncryptionException("Raw password cannot be null ");
        }

        // Technically shift can be any non-zero integer, for clarity it should be between 0-25
        // inclusive
        if (shift < 0 || shift >= 26) {
            throw new EncryptionException("Shift value must be between 0 and 25 inclusive.");
        }

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

    /**
     * INSECURE: Custom cipher that obscures the texts by reversing it then Base64 encodes it.
     *
     * @param rawPassword password to encrypt
     */
    public static String customCipher(String rawPassword) throws EncryptionException {
        if (rawPassword == null) {
            throw new EncryptionException("Raw password cannot be null ");
        }
        String reversed = new StringBuilder(rawPassword).reverse().toString();
        return EncodingUtils.encodeBase64(reversed);
    }

    private static final byte[] salt = new byte[16];

    static {
        new SecureRandom().nextBytes(salt);
    }

    public static SecretKey getKeyFromPassword(String password) throws EncryptionException {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1, 128);

            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new EncryptionException("Error generating AES key from password", e);
        }
    }

    public static String encrypt(String plaintext, SecretKey key) throws EncryptionException {
        try {
            // VULNERABILITY NOTE: ECB mode does not use an IV and reveals patterns (CWE-327)
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encrypted);

        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new EncryptionException("AES configuration not found ", e);
        } catch (InvalidKeyException e) {
            throw new EncryptionException("The provided key is invalid for AES encryption", e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new EncryptionException(
                    "AES encryption failed due to block size or padding issues", e);
        }
    }
}
