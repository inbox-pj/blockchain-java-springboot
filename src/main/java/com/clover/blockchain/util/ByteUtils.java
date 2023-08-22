package com.clover.blockchain.util;


import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

public class ByteUtils {

    public static byte[] merge(byte[]... bytes) {
        Stream<Byte> stream = Stream.of();
        for (byte[] b : bytes) {
            stream = Stream.concat(stream, Arrays.stream(ArrayUtils.toObject(b)));
        }
        return ArrayUtils.toPrimitive(stream.toArray(Byte[]::new));
    }

    public static byte[] toBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).putLong(val).array();
    }
}
