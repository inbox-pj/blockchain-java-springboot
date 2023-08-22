package com.clover.blockchain.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Base58 conversion tool
 */
public final class Base58Check {

    /**
     * Add check code and convert to Base58 string
     *
     * @param data
     * @return
     */
    public static String bytesToBase58(byte[] data) {
        return rawBytesToBase58(addCheckHash(data));
    }

    /**
     * Convert to Base58 string
     *
     * @param data
     * @return
     */
    public static String rawBytesToBase58(byte[] data) {
        // Convert to base-58 string
        StringBuilder sb = new StringBuilder();
        BigInteger num = new BigInteger(1, data);
        while (num. signum() != 0) {
            BigInteger[] quotrem = num.divideAndRemainder(ALPHABET_SIZE);
            sb.append(ALPHABET.charAt(quotrem[1].intValue()));
            num = quotrem[0];
        }

        // Add '1' characters for leading 0-value bytes
        for (int i = 0; i < data.length && data[i] == 0; i++) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }

    /**
     * Add a check code and return the original data with the check code
     *
     * @param data
     * @return
     */
    static byte[] addCheckHash(byte[] data) {
        try {
            byte[] hash = Arrays.copyOf(BtcAddressUtils.doubleHash(data), 4);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(data);
            buf.write(hash);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


    /**
     * Convert the Base58Check string to a byte array, and verify its check code
     * The returned byte array has a version number, but does not have a checksum
     *
     * @param s
     * @return
     */
    public static byte[] base58ToBytes(String s) {
        byte[] concat = base58ToRawBytes(s);
        byte[] data = Arrays. copyOf(concat, concat. length - 4);
        byte[] hash = Arrays.copyOfRange(concat, concat.length - 4, concat.length);
        byte[] rehash = Arrays.copyOf(BtcAddressUtils.doubleHash(data), 4);
        if (!Arrays. equals(rehash, hash)) {
            throw new IllegalArgumentException("Checksum mismatch");
        }
        return data;
    }

    /**
     * Reverse Base58Check string to byte array
     *
     * @param s
     * @return
     */
    static byte[] base58ToRawBytes(String s) {
        // Parse base-58 string
        BigInteger num = BigInteger. ZERO;
        for (int i = 0; i < s. length(); i++) {
            num = num.multiply(ALPHABET_SIZE);
            int digit = ALPHABET. indexOf(s. charAt(i));
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid character for Base58Check");
            }
            num = num. add(BigInteger. valueOf(digit));
        }
        // Strip possible leading zero due to mandatory sign bit
        byte[] b = num.toByteArray();
        if (b[0] == 0) {
            b = Arrays. copyOfRange(b, 1, b. length);
        }
        try {
            // Convert leading '1' characters to leading 0-value bytes
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            for (int i = 0; i < s. length() && s. charAt(i) == ALPHABET. charAt(0); i++) {
                buf.write(0);
            }
            buf. write(b);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


    /*---- Class constants ----*/

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger ALPHABET_SIZE = BigInteger. valueOf(ALPHABET. length());


    /*---- Miscellaneous ----*/

    private Base58Check() {
    } // Not instantiable

}