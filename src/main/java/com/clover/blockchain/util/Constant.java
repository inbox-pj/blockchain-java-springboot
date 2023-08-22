package com.clover.blockchain.util;

import org.apache.commons.codec.binary.Hex;

public final class Constant {

    // DIFFICULTY_TARGET_BITS- the bigger it is, targetValue the smaller it will be, and the Hash required to be calculated is getting smaller and smaller,
    // that is, the difficulty of mining is getting bigger and bigger.
    public static final int DIFFICULTY_TARGET_BITS = 20;

    public static final byte[] EMPTY_BYTES = new byte[32];

    public static final String ZERO_HASH = Hex.encodeHexString(EMPTY_BYTES);

    public static final String GENESIS_COINBASE_DATA = "Genesis Block Data";

}
