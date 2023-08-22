package com.clover.blockchain.pow;

import com.clover.blockchain.block.Block;
import com.clover.blockchain.util.ByteUtils;
import com.clover.blockchain.util.Constant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ProofOfWork {

    private Block block;

    private BigInteger target;

    // Create a new proof-of-work, set a difficulty target
    // Do a bit shift by 1, shifting 1 to the left by (256 - TARGET_BITS) bits to get our difficulty target value
    public static ProofOfWork newProofOfWork(Block block) {
        BigInteger targetValue = BigInteger.ONE.shiftLeft((256 - Constant.DIFFICULTY_TARGET_BITS));
        return new ProofOfWork(block, targetValue);
    }

    // Note: When preparing block data, be sure to convert from the original data type to byte[], not directly from string
    private byte[] prepareData(long nonce) {
        byte[] prevBlockHashBytes = {};
        if (StringUtils.isNoneBlank(this.getBlock().getPrevBlockHash())) {
            prevBlockHashBytes = new BigInteger(this.getBlock().getPrevBlockHash(), 16).toByteArray();
        }

        return ByteUtils.merge(
                prevBlockHashBytes,
                this.getBlock().hashTransaction(),
                ByteUtils.toBytes(this.getBlock().getTimeStamp()),
                ByteUtils.toBytes(Constant.DIFFICULTY_TARGET_BITS),
                ByteUtils.toBytes(nonce)
        );

    }

    // Verify that the block is valid
    public boolean validate() {
        byte[] data = this.prepareData(this.getBlock().getNonce());
        return new BigInteger(DigestUtils.sha256Hex(data), 16).compareTo(this.target) == -1;
    }

    // Run the proof of work, start mining, and find a Hash that is less than the difficulty target value
    public PowResult run() {
        long nonce = 0;
        String shaHex = "";
        long startTime = System.currentTimeMillis();
        while (nonce < Long.MAX_VALUE) {
            log.info("POW running, nonce=" + nonce);
            byte[] data = this.prepareData(nonce);
            shaHex = DigestUtils.sha256Hex(data);
            if (new BigInteger(shaHex, 16).compareTo(this.target) == -1) {
                log.info("Elapsed Time: {} seconds \n", new Object[]{(float) (System.currentTimeMillis() - startTime) / 1000});
                log.info("correct hash Hex: {} \n", new Object[]{shaHex});
                break;
            } else {
                nonce++;
            }
        }
        return new PowResult(nonce, shaHex);
    }
}

