package com.clover.blockchain.block;

import com.clover.blockchain.service.RocksDBService;
import com.clover.blockchain.transaction.TXInput;
import com.clover.blockchain.transaction.TXOutput;
import com.clover.blockchain.transaction.Transaction;
import com.clover.blockchain.util.Constant;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@Slf4j
public class Blockchain {

    private String lastBlockHash;

    private static RocksDBService rocksDBService;

    @Autowired
    public Blockchain(RocksDBService rocksDBService) {
        this.rocksDBService = rocksDBService;
    }

    // restore blockchain data from database
    public static Blockchain initBlockchainFromDB() throws Exception {
        String lastBlockHash = rocksDBService.getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to init blockchain from db. ");
        }
        return new Blockchain(lastBlockHash);
    }

    // create a blockchain on wallet address
    public static Blockchain createBlockchain(String address) {
        String lastBlockHash = rocksDBService.getLastBlockHash();
        log.info("Create BlockChain - lastBlockHash ->" + lastBlockHash);
        if (StringUtils.isBlank(lastBlockHash)) {
            // create coinbase transaction
            Transaction coinbaseTX = Transaction.newCoinbaseTX(address, Constant.GENESIS_COINBASE_DATA);
            Block genesisBlock = Block.newGenesisBlock(coinbaseTX);
            lastBlockHash = genesisBlock.getHash();
            rocksDBService.putBlock(genesisBlock);
            rocksDBService.putLastBlockHash(lastBlockHash);
        }
        return new Blockchain(lastBlockHash);
    }

    // verify and mine transaction
    public Block mineBlock(Transaction[] transactions) throws Exception {
        for (Transaction tx : transactions) {
            if (!this.verifyTransactions(tx)) {
                log.error("ERROR: Fail to mine block ! Invalid transaction ! tx=" + tx.toString());
                throw new Exception("ERROR: Fail to mine block ! Invalid transaction ! ");
            }
        }
        String lastBlockHash = rocksDBService.getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to get last block hash ! ");
        }

        Block block = Block.newBlock(lastBlockHash, transactions);
        this.addBlock(block);
        return block;
    }

    public void addBlock(Block block) {
        rocksDBService.putLastBlockHash(block.getHash());
        rocksDBService.putBlock(block);
        this.lastBlockHash = block.getHash();
    }

    public class BlockchainIterator {

        private String currentBlockHash;

        public BlockchainIterator(String currentBlockHash) {
            this.currentBlockHash = currentBlockHash;
        }

        public boolean hashNext() {
            if (StringUtils.isBlank(currentBlockHash) || currentBlockHash.equals(Constant.ZERO_HASH)) {
                return false;
            }
            Block lastBlock = rocksDBService.getBlock(currentBlockHash);
            if (lastBlock == null) {
                return false;
            }
            // check if it is genesis block
            if (lastBlock.getHash().equals(Constant.ZERO_HASH)) {
                return false;
            }
            return rocksDBService.getBlock(lastBlock.getHash()) != null;
        }


        public Block next() {
            Block currentBlock = rocksDBService.getBlock(currentBlockHash);
            if (currentBlock != null) {
                this.currentBlockHash = currentBlock.getPrevBlockHash();
                return currentBlock;
            }
            return null;
        }
    }

    public BlockchainIterator getBlockchainIterator() {
        log.info("lastBlockHash -> " + lastBlockHash);
        return new BlockchainIterator(lastBlockHash);
    }

    // find all unspend transaction outputs
    public Map<String, TXOutput[]> findAllUTXOs() {
        Map<String, int[]> allSpentTXOs = this.getAllSpentTXOs();
        Map<String, TXOutput[]> allUTXOs = Maps.newHashMap();
        // iterate over transaction output in all blocks
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();
            for (Transaction transaction : block.getTransactions()) {

                String txId = Hex.encodeHexString(transaction.getTxId());

                int[] spentOutIndexArray = allSpentTXOs.get(txId);
                TXOutput[] txOutputs = transaction.getOutputs();
                for (int outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                    if (spentOutIndexArray != null && ArrayUtils.contains(spentOutIndexArray, outIndex)) {
                        continue;
                    }
                    TXOutput[] UTXOArray = allUTXOs.get(txId);
                    if (UTXOArray == null) {
                        UTXOArray = new TXOutput[]{txOutputs[outIndex]};
                    } else {
                        UTXOArray = ArrayUtils.add(UTXOArray, txOutputs[outIndex]);
                    }
                    allUTXOs.put(txId, UTXOArray);
                }
            }
        }
        return allUTXOs;
    }

    // query all spent transaction outputs in the blockchain from transaction inputs and return transaction id and it's corrosponding transaction output subscript address
    private Map<String, int[]> getAllSpentTXOs() {
        // Define TxId ——> spentOutIndex[], store the transaction ID and the index value of the spent transaction output array
        Map<String, int[]> spentTXOs = Maps.newHashMap();
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();

            for (Transaction transaction : block.getTransactions()) {
                // If it is a coinbase transaction, skip it directly, because there is no transaction output referencing the previous block
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (TXInput txInput : transaction.getInputs()) {
                    String inTxId = Hex.encodeHexString(txInput.getTxId());
                    int[] spentOutIndexArray = spentTXOs.get(inTxId);
                    if (spentOutIndexArray == null) {
                        spentOutIndexArray = new int[]{txInput.getTxOutputIndex()};
                    } else {
                        spentOutIndexArray = ArrayUtils.add(spentOutIndexArray, txInput.getTxOutputIndex());
                    }
                    spentTXOs.put(inTxId, spentOutIndexArray);
                }
            }
        }
        return spentTXOs;
    }


    // Query transaction information based on transaction ID
    private Transaction findTransaction(byte[] txId) throws Exception {
        for (BlockchainIterator iterator = this.getBlockchainIterator(); iterator.hashNext(); ) {
            Block block = iterator.next();
            for (Transaction tx : block.getTransactions()) {
                if (Arrays.equals(tx.getTxId(), txId)) {
                    return tx;
                }
            }
        }
        throw new Exception("ERROR: Can not found tx by txId ! ");
    }

    // sign the transaction
    public void signTransaction(Transaction tx, BCECPrivateKey privateKey) throws Exception {
        // First find the data of the previous multiple transactions referenced by the transaction input in this new transaction
        Map<String, Transaction> prevTxMap = Maps.newHashMap();
        for (TXInput txInput : tx.getInputs()) {
            Transaction prevTx = this.findTransaction(txInput.getTxId());
            prevTxMap.put(Hex.encodeHexString(txInput.getTxId()), prevTx);
        }
        tx.sign(privateKey, prevTxMap);
    }

    // transaction signature verification
    private boolean verifyTransactions(Transaction tx) throws Exception {
        if (tx.isCoinbase()) {
            return true;
        }
        Map<String, Transaction> prevTx = Maps.newHashMap();
        for (TXInput txInput : tx.getInputs()) {
            Transaction transaction = this.findTransaction(txInput.getTxId());
            prevTx.put(Hex.encodeHexString(txInput.getTxId()), transaction);
        }
        try {
            return tx.verify(prevTx);
        } catch (Exception e) {
            log.error("Fail to verify transaction ! transaction invalid ! ", e);
            throw new RuntimeException("Fail to verify transaction ! transaction invalid ! ", e);
        }
    }
}
