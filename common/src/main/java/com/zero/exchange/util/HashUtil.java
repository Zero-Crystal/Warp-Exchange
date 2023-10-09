package com.zero.exchange.util;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * Generate SHA-256 as hex string (all lower-case).
     *
     * @param input Input as bytes.
     * @return Hex string.
     */
    public static String sha256(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(input);
        byte[] digest = md.digest();
        return ByteUtil.toHexString(digest);
    }

    /**
     * Generate SHA-256 as hex string (all lower-case).
     *
     * @param input Input as String.
     * @return Hex string.
     */
    public static String sha256(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return Hex string.
     */
    public static byte[] hmacSha256AsBytes(byte[] data, byte[] key) {
        SecretKey secretKey = new SecretKeySpec(key, "HmacSHA256");
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        mac.update(data);
        return mac.doFinal();
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return Hex string.
     */
    public static String hmacSha256(byte[] data, byte[] key) {
        return ByteUtil.toHexString(hmacSha256AsBytes(data, key));
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return Hex string.
     */
    public static String hmacSha256(String data, String key) {
        return ByteUtil.toHexString(hmacSha256AsBytes(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8)));
    }
}
