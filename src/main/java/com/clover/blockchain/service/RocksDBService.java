package com.clover.blockchain.service;

import com.clover.blockchain.block.Block;
import com.clover.blockchain.config.RocksDBConfig;
import com.clover.blockchain.kryo.KryoSerializer;
import com.clover.blockchain.transaction.TXOutput;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RocksDBService {

    private final RocksDB rocksDB;

    private final RocksDBConfig rocksDBConfig;

    @Autowired
    public RocksDBService(RocksDB rocksDB, RocksDBConfig rocksDBConfig) {
        this.rocksDB = rocksDB;
        this.rocksDBConfig = rocksDBConfig;
    }

    // save the hash value of the latest block
    public void putLastBlockHash(String tipBlockHash) {
        try {
            rocksDBConfig.getBlocksBucket().put(rocksDBConfig.getLastBlockKey(), KryoSerializer.serialize(tipBlockHash));
            rocksDB.put(KryoSerializer.serialize(rocksDBConfig.getBlockBucketKey()), KryoSerializer.serialize(rocksDBConfig.getBlocksBucket()));
        } catch (RocksDBException e) {
            log.error("Fail to put last block hash ! tipBlockHash=" + tipBlockHash, e);
            throw new RuntimeException("Fail to put last block hash ! tipBlockHash=" + tipBlockHash, e);
        }
    }

    // query the hash value of latest block
    public String getLastBlockHash() {
        byte[] lastBlockHashBytes = rocksDBConfig.getBlocksBucket().get(rocksDBConfig.getLastBlockKey());
        if (lastBlockHashBytes != null) {
            return (String) KryoSerializer.deserialize(lastBlockHashBytes);
        }
        return "";
    }

    // save block
    public void putBlock(Block block) {
        try {
            rocksDBConfig.getBlocksBucket().put(block.getHash(), KryoSerializer.serialize(block));
            rocksDB.put(KryoSerializer.serialize(rocksDBConfig.getBlockBucketKey()), KryoSerializer.serialize(rocksDBConfig.getBlocksBucket()));
        } catch (RocksDBException e) {
            log.error("Fail to put block ! block=" + block.toString(), e);
            throw new RuntimeException("Fail to put block ! ", e);
        }
    }

    // query block
    public Block getBlock(String blockHash) {
        byte[] blockBytes = rocksDBConfig.getBlocksBucket().get(blockHash);
        if (blockBytes != null) {
            return (Block) KryoSerializer.deserialize(blockBytes);
        }
        throw new RuntimeException("Fail to get block ! blockHash=" + blockHash);
    }

    // empty the chainstate bucket
    public void cleanChainStateBucket() {
        try {
            rocksDBConfig.getChainstateBucket().clear();
        } catch (Exception e) {
            log.error("Fail to clear chainstate bucket ! ", e);
            throw new RuntimeException("Fail to clear chainstate bucket ! ", e);
        }
    }

    public void putUTXOs(String key, TXOutput[] utxos) {
        try {
            rocksDBConfig.getChainstateBucket().put(key, KryoSerializer.serialize(utxos));
            rocksDB.put(KryoSerializer.serialize(rocksDBConfig.getChainstateBucketKey()), KryoSerializer.serialize(rocksDBConfig.getChainstateBucket()));
        } catch (Exception e) {
            log.error("Fail to put UTXOs into chainstate bucket ! key=" + key, e);
            throw new RuntimeException("Fail to put UTXOs into chainstate bucket ! key=" + key, e);
        }
    }

    // save UTXO data
    public TXOutput[] getUTXOs(String key) {
        byte[] utxosByte = rocksDBConfig.getChainstateBucket().get(key);
        if (utxosByte != null) {
            return (TXOutput[]) KryoSerializer.deserialize(utxosByte);
        }
        return null;
    }

    // remove UTXO datra
    public void deleteUTXOs(String key) {
        try {
            rocksDBConfig.getChainstateBucket().remove(key);
            rocksDB.put(KryoSerializer.serialize(rocksDBConfig.getChainstateBucketKey()), KryoSerializer.serialize(rocksDBConfig.getChainstateBucket()));
        } catch (Exception e) {
            log.error("Fail to delete UTXOs by key ! key=" + key, e);
            throw new RuntimeException("Fail to delete UTXOs by key ! key=" + key, e);
        }
    }






}
