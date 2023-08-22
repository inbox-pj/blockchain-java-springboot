package com.clover.blockchain.config;

import com.clover.blockchain.kryo.KryoSerializer;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
@Configuration
public class RocksDBConfig {

    @Value("${rocksdb.directory}")
    private String rocksDBDirectory;

    @Value("${rocksdb.create-if-missing}")
    private boolean createIfMissing;

    @Value("${rocksdb.block-bucket-key}")
    private String blockBucketKey;

    @Value("${rocksdb.last-bucket-key}")
    private String lastBlockKey;

    @Value("${rocksdb.chainstate-bucket-key}")
    private String chainstateBucketKey;

    private Map<String, byte[]> blocksBucket;

    @Getter
    private Map<String, byte[]> chainStateBucket;

    @Autowired
    private KryoSerializer serializer;

    @Bean(destroyMethod = "close")
    public RocksDB rocksDB() {
        RocksDB.loadLibrary();
        Options options = new Options()
                .setCreateIfMissing(createIfMissing)
                .setDbLogDir(rocksDBDirectory);
        try {
            RocksDB db = RocksDB.open(options, rocksDBDirectory);
            return initBlockBucket(db);
        } catch (RocksDBException e) {
            throw new RuntimeException("Error opening RocksDB", e);
        }
    }

    private RocksDB initBlockBucket(RocksDB db) {
        try {
            byte[] blockBucketKeyByte = serializer.serialize(blockBucketKey);
            byte[] blockBucketBytes = db.get(blockBucketKeyByte);
            if (blockBucketBytes != null) {
                blocksBucket = (Map) serializer.deserialize(blockBucketBytes);
            } else {
                blocksBucket = new HashMap<>();
                db.put(blockBucketKeyByte, serializer.serialize(blocksBucket));
            }
            return initChainStateBucket(db);
        } catch (RocksDBException e) {
            log.error("Fail to init block bucket ! ", e);
            throw new RuntimeException("Fail to init block bucket ! ", e);
        }
    }

    private RocksDB initChainStateBucket(RocksDB db) {
        try {
            byte[] chainstateBucketKeyByte = serializer.serialize(chainstateBucketKey);
            byte[] chainstateBucketBytes = db.get(chainstateBucketKeyByte);
            if (chainstateBucketBytes != null) {
                chainStateBucket = (Map) serializer.deserialize(chainstateBucketBytes);
            } else {
                chainStateBucket = Maps.newHashMap();
                db.put(chainstateBucketKeyByte, serializer.serialize(chainStateBucket));
            }
        } catch (RocksDBException e) {
            log.error("Fail to init chainstate bucket ! ", e);
            throw new RuntimeException("Fail to init chainstate bucket ! ", e);
        }
        return db;
    }

}
