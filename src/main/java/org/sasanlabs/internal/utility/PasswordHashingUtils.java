package org.sasanlabs.internal.utility;

import org.springframework.security.crypto.password.Md4PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

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

    public static String bCryptHash(int strength, String rawPassword){
        if (4 > strength || strength > 31) throw new AssertionError("Bcrypt strength must be between 4 and 31");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(strength);
        return encoder.encode(rawPassword);
    }

    public static boolean isValidBcrypt(int strength, String rawPassword, String storedPassword) {
        if (4 > strength || strength > 31) throw new AssertionError("Bcrypt strength must be between 4 and 31");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(strength);
        return encoder.matches(rawPassword, storedPassword);
    }


    /**
     * Computes an LM hash for the given password.
     *
     * <p>Algorithm based on the LAN Manager specification:
     * @see <a href="https://en.wikipedia.org/wiki/LAN_Manager">Wikipedia: LAN Manager</a>
     */
    public static String lmHash(String rawPassword) {
        try {
            // Convert to uppercase and pad to 14 bytes
            String pwd = rawPassword.toUpperCase();
            byte[] keyBytes = new byte[14];
            byte[] passwordBytes = pwd.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(passwordBytes, 0, keyBytes, 0, Math.min(passwordBytes.length, 14));

            // Split into two 7-byte keys
            byte[] tmpKey1 = new byte[7];
            byte[] tmpKey2 = new byte[7];
            System.arraycopy(keyBytes, 0, tmpKey1, 0, 7);
            System.arraycopy(keyBytes, 7, tmpKey2, 0, 7);

            // Encrypt the magic string "KGS!@#$%" using each key
            return bytesToHex(lmDesEncrypt(tmpKey1)) + bytesToHex(lmDesEncrypt(tmpKey2));
        } catch (Exception e) {
            throw new RuntimeException("LM Hashing failed", e);
        }
    }

    private static byte[] lmDesEncrypt(byte[] key7) throws Exception {
        // LM Hash uses a specific parity-bit transformation to turn 7 bytes into an 8-byte DES key
        byte[] key8 = new byte[8];
        key8[0] = (byte) (key7[0] >> 1);
        key8[1] = (byte) (((key7[0] & 0x01) << 6) | (key7[1] >> 2));
        key8[2] = (byte) (((key7[1] & 0x03) << 5) | (key7[2] >> 3));
        key8[3] = (byte) (((key7[2] & 0x07) << 4) | (key7[3] >> 4));
        key8[4] = (byte) (((key7[3] & 0x0F) << 3) | (key7[4] >> 5));
        key8[5] = (byte) (((key7[4] & 0x1F) << 2) | (key7[5] >> 6));
        key8[6] = (byte) (((key7[5] & 0x3F) << 1) | (key7[6] >> 7));
        key8[7] = (byte) (key7[6] & 0x7F);
        for (int i = 0; i < 8; i++) key8[i] = (byte) (key8[i] << 1);

        Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
        des.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key8, "DES"));
        return des.doFinal("KGS!@#$%".getBytes(StandardCharsets.US_ASCII));
    }

    public static String bytesToHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte value : data) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
