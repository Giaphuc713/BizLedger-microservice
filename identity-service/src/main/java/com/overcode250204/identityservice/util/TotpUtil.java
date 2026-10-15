package com.overcode250204.identityservice.util;

import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@UtilityClass
public class TotpUtil {
    private static final int TIME_WINDOW_MS = 30000; // 30 seconds

    public static String generateSecretKey() {
        byte[] buffer = new byte[10]; // 80 bits is enough for TOTP
        new SecureRandom().nextBytes(buffer);
        return encodeBase32(buffer);
    }

    public static boolean verifyOtp(String secret, String otp) {
        if (secret == null || otp == null || otp.length() != 6) {
            return false;
        }
        try {
            int code = Integer.parseInt(otp);
            byte[] decodedKey = decodeBase32(secret);
            long currentWindow = System.currentTimeMillis() / TIME_WINDOW_MS;
            // Check current, previous (-1), and next (+1) windows to allow for time drift
            for (int i = -1; i <= 1; i++) {
                if (calculateCode(decodedKey, currentWindow + i) == code) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Suppress exception
        }
        return false;
    }

    private static int calculateCode(byte[] key, long time) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = ByteBuffer.allocate(8).putLong(time).array();
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        int truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;
        return truncatedHash;
    }

    private static byte[] decodeBase32(String base32) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        base32 = base32.toUpperCase().replaceAll("[^" + alphabet + "]", "");
        int numBytes = base32.length() * 5 / 8;
        byte[] bytes = new byte[numBytes];
        int i = 0, index = 0, offset = 0;
        int buffer = 0;
        while (i < base32.length()) {
            int charVal = alphabet.indexOf(base32.charAt(i));
            buffer = (buffer << 5) | charVal;
            offset += 5;
            if (offset >= 8) {
                bytes[index++] = (byte) ((buffer >> (offset - 8)) & 0xFF);
                offset -= 8;
            }
            i++;
        }
        return bytes;
    }

    private static String encodeBase32(byte[] bytes) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        int i = 0, index = 0, digit = 0;
        int currentByte, nextByte;
        while (i < bytes.length) {
            currentByte = (bytes[i] >= 0) ? bytes[i] : (bytes[i] + 256);
            if (index > 3) {
                if (i + 1 < bytes.length) {
                    nextByte = (bytes[i + 1] >= 0) ? bytes[i + 1] : (bytes[i + 1] + 256);
                } else {
                    nextByte = 0;
                }
                digit = currentByte & (0xFF >> index);
                index = (index + 5) % 8;
                digit <<= index;
                digit |= (nextByte >> (8 - index));
                i++;
            } else {
                digit = (currentByte >> (8 - (index + 5))) & 0x1F;
                index = (index + 5) % 8;
                if (index == 0) {
                    i++;
                }
            }
            sb.append(alphabet.charAt(digit));
        }
        return sb.toString();
    }
}
