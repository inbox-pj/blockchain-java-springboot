package com.clover.blockchain.block;

import com.clover.blockchain.pow.PowResult;
import com.clover.blockchain.pow.ProofOfWork;
import com.clover.blockchain.transaction.MerkleTree;
import com.clover.blockchain.transaction.Transaction;
import com.clover.blockchain.util.Constant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Block {
    private String hash;
    private String prevBlockHash;
    private Transaction[] transactions;
    private long timeStamp;
    private long nonce;

    public static Block newGenesisBlock(Transaction coinbase) {
        return Block.newBlock(Constant.ZERO_HASH, new Transaction[]{coinbase});
    }

    // create new block
    public static Block newBlock(String previousHash, Transaction[] transactions) {
        Block block = new Block("", previousHash, transactions, Instant.now().getEpochSecond(), 0);
        ProofOfWork pow = ProofOfWork.newProofOfWork(block);
        PowResult powResult = pow.run();
        block.setHash(powResult.getHash());
        block.setNonce(powResult.getNonce());
        return block;
    }

    // perform hash calculation on the transaction information in the block
    public byte[] hashTransaction() {
        byte[][] txIdArrays = new byte[this.getTransactions().length][];
        for (int i = 0; i < this.getTransactions().length; i++) {
            txIdArrays[i] = this.getTransactions()[i].getTxId();
        }
        return new MerkleTree(txIdArrays).getRoot().getHash();
    }

    @Override
    public String toString() {
        return "Block{" +
                "hash='" + hash + '\'' +
                ", prevBlockHash='" + prevBlockHash + '\'' +
                ", transactions=" + Arrays.toString(transactions) +
                ", timeStamp=" + timeStamp +
                ", nonce=" + nonce +
                '}';
    }
}
