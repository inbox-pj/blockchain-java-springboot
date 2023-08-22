package com.clover.blockchain.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.util.Arrays;

public class BtcAddressUtils {
    /**
     * Double Hash
     *
     * @param data
     * @return
     */
    public static byte[] doubleHash(byte[] data) {
        return DigestUtils.sha256(DigestUtils.sha256(data));
    }

    /**
     * Calculate the RIPEMD160 Hash value of the public key
     *
     * @param pubKey public key
     * @return ipeMD160Hash(sha256 ( pubkey))
     */
    public static byte[] ripeMD160Hash(byte[] pubKey) {
        //1. First do sha256 processing on the public key
        byte[] shaHashedKey = DigestUtils.sha256(pubKey);
        RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
        ripemd160.update(shaHashedKey, 0, shaHashedKey.length);
        byte[] output = new byte[ripemd160.getDigestSize()];
        ripemd160.doFinal(output, 0);
        return output;
    }

    /**
     * Generate the verification code of the public key
     *
     * @param payload
     * @return
     */
    public static byte[] checksum(byte[] payload) {
        return Arrays.copyOfRange(doubleHash(payload), 0, 4);
    }
}
