package com.clover.blockchain.transaction;

import com.clover.blockchain.block.Block;
import com.clover.blockchain.block.Blockchain;
import com.clover.blockchain.config.RocksDBConfig;
import com.clover.blockchain.kryo.KryoSerializer;
import com.clover.blockchain.service.RocksDBService;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Pool of unspent transaction outputs
 */
@Slf4j
@Component
@AllArgsConstructor
@NoArgsConstructor
public class UTXOSet {

    private Blockchain blockchain;

    private static RocksDBConfig rocksDBConfig;

    private static RocksDBService rocksDBService;

    @Autowired
    public UTXOSet(RocksDBService rocksDBService, RocksDBConfig rocksDBConfig) {
        this.rocksDBService = rocksDBService;
        this.rocksDBConfig = rocksDBConfig;
    }

    public UTXOSet blockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
        return this;
    }

    // Find deals to spend
    public SpendableOutputResult findSpendableOutputs(byte[] pubKeyHash, int amount) {
        Map<String, int[]> unspentOuts = Maps.newHashMap();
        int accumulated = 0;
        Map<String, byte[]> chainstateBucket = rocksDBConfig.getChainStateBucket();
        for (Map.Entry<String, byte[]> entry : chainstateBucket.entrySet()) {
            String txId = entry.getKey();
            TXOutput[] txOutputs = (TXOutput[]) KryoSerializer.deserialize(entry.getValue());

            for (int outId = 0; outId < txOutputs.length; outId++) {
                TXOutput txOutput = txOutputs[outId];
                if (txOutput.isLockedWithKey(pubKeyHash) && accumulated < amount) {
                    accumulated += txOutput.getValue();

                    int[] outIds = unspentOuts.get(txId);
                    if (outIds == null) {
                        outIds = new int[]{outId};
                    } else {
                        outIds = ArrayUtils.add(outIds, outId);
                    }
                    unspentOuts.put(txId, outIds);
                    if (accumulated >= amount) {
                        break;
                    }
                }
            }
        }
        return new SpendableOutputResult(accumulated, unspentOuts);
    }


    // Find all UTXOs corresponding to the wallet address
    public TXOutput[] findUTXOs(byte[] pubKeyHash) {
        TXOutput[] utxos = {};
        Map<String, byte[]> chainstateBucket = rocksDBConfig.getChainStateBucket();
        if (chainstateBucket.isEmpty()) {
            return utxos;
        }
        for (byte[] value : chainstateBucket.values()) {
            TXOutput[] txOutputs = (TXOutput[]) KryoSerializer.deserialize(value);
            for (TXOutput txOutput : txOutputs) {
                if (txOutput.isLockedWithKey(pubKeyHash)) {
                    utxos = ArrayUtils.add(utxos, txOutput);
                }
            }
        }
        return utxos;
    }

    // Rebuild the UTXO pool index
    @Synchronized
    public void reIndex() {
        log.info("Start to reIndex UTXO set !");
        rocksDBService.cleanChainStateBucket();
        Map<String, TXOutput[]> allUTXOs = blockchain.findAllUTXOs();
        for (Map.Entry<String, TXOutput[]> entry : allUTXOs.entrySet()) {
            rocksDBService.putUTXOs(entry.getKey(), entry.getValue());
        }
        log.info("ReIndex UTXO set finished ! ");
    }

    /**
     * Update UTXO pool
     * When a new block is generated, two things need to be done:
     * 1) Remove the spent transaction output from the UTXO pool;
     * 2) Save the new unspent transaction output;
     *
     * @param tipBlock latest block
     */
    @Synchronized
    public void update(Block tipBlock) {
        if (tipBlock == null) {
            log.error("Fail to update UTXO set ! tipBlock is null !");
            throw new RuntimeException("Fail to update UTXO set ! ");
        }
        for (Transaction transaction : tipBlock.getTransactions()) {

            // Find out the remaining unused transaction outputs based on transaction inputs
            if (!transaction.isCoinbase()) {
                for (TXInput txInput : transaction.getInputs()) {
                    // Remaining unspent transaction outputs
                    TXOutput[] remainderUTXOs = {};
                    String txId = Hex.encodeHexString(txInput.getTxId());
                    TXOutput[] txOutputs = rocksDBService.getUTXOs(txId);

                    if (txOutputs == null) {
                        continue;
                    }

                    for (int outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                        if (outIndex != txInput.getTxOutputIndex()) {
                            remainderUTXOs = ArrayUtils.add(remainderUTXOs, txOutputs[outIndex]);
                        }
                    }

                    // Delete if there is no remaining, otherwise update
                    if (remainderUTXOs.length == 0) {
                        rocksDBService.deleteUTXOs(txId);
                    } else {
                        rocksDBService.putUTXOs(txId, remainderUTXOs);
                    }
                }
            }

            // The new transaction output is saved to DB
            TXOutput[] txOutputs = transaction.getOutputs();
            String txId = Hex.encodeHexString(transaction.getTxId());
            rocksDBService.putUTXOs(txId, txOutputs);
        }


    }


}