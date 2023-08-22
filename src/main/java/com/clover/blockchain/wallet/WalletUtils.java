package com.clover.blockchain.wallet;

import com.clover.blockchain.util.Base58Check;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Slf4j
public class WalletUtils {
    /**
     * Example of wallet tool
     */
    private volatile static WalletUtils instance;

    public static WalletUtils getInstance() {
        if (instance == null) {
            synchronized (WalletUtils.class) {
                if (instance == null) {
                    instance = new WalletUtils();
                }
            }
        }
        return instance;
    }

    private WalletUtils() {
        initWalletFile();
    }

    /**
     * wallet file
     */
    private final static String WALLET_FILE = "wallet.dat";
    /**
     * Encryption Algorithm
     */
    private static final String ALGORITHM = "AES";
    /**
     * ciphertext
     */
    private static final byte[] CIPHER_TEXT = "2oF@5sC%DNf32y!TmiZi!tG9W5rLaniD".getBytes();

    /**
     * Initialize the wallet file
     */
    private void initWalletFile() {
        File file = new File(WALLET_FILE);
        if (!file.exists()) {
            this.saveToDisk(new Wallets());
        } else {
            this.loadFromDisk();
        }
    }

    /**
     * Get all wallet addresses
     *
     * @return
     */
    public Set<String> getAddresses() {
        Wallets wallets = this.loadFromDisk();
        return wallets.getAddresses();
    }

    /**
     * Get wallet data
     *
     * @param address wallet address
     * @return
     */
    public Wallet getWallet(String address) {
        Wallets wallets = this.loadFromDisk();
        return wallets.getWallet(address);
    }

    /**
     * create wallet
     *
     * @return
     */
    public Wallet createWallet() {
        Wallet wallet = new Wallet();
        Wallets wallets = this.loadFromDisk();
        wallets.addWallet(wallet);
        this.saveToDisk(wallets);
        return wallet;
    }

    /**
     * Load wallet data
     */
    private Wallets loadFromDisk() {
        try {
            SecretKeySpec sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, sks);
            @Cleanup CipherInputStream cipherInputStream = new CipherInputStream(
                    new BufferedInputStream(new FileInputStream(WALLET_FILE)), cipher);
            @Cleanup ObjectInputStream inputStream = new ObjectInputStream(cipherInputStream);
            SealedObject sealedObject = (SealedObject) inputStream.readObject();
            return (Wallets) sealedObject.getObject(cipher);
        } catch (Exception e) {
            log.error("Fail to load wallet from disk ! ", e);
            throw new RuntimeException("Fail to load wallet from disk ! ");
        }
    }

    /**
     * Save wallet data
     */
    private void saveToDisk(Wallets wallets) {
        try {
            if (wallets == null) {
                log.error("Fail to save wallet to file ! wallets is null ");
                throw new Exception("ERROR: Fail to save wallet to file!");
            }
            SecretKeySpec sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            //Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            SealedObject sealedObject = new SealedObject(wallets, cipher);
            // Wrap the output stream
            @Cleanup CipherOutputStream cos = new CipherOutputStream(
                    new BufferedOutputStream(new FileOutputStream(WALLET_FILE)), cipher);
            @Cleanup ObjectOutputStream outputStream = new ObjectOutputStream(cos);
            outputStream.writeObject(sealedObject);
        } catch (Exception e) {
            log.error("Fail to save wallet to disk !", e);
            throw new RuntimeException("Fail to save wallet to disk!");
        }
    }

    /**
     * wallet storage object
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Wallets implements Serializable {

        private static final long serialVersionUID = -2542070981569243131L;

        private Map<String, Wallet> walletMap = Maps.newHashMap();

        /**
         * Add wallet
         *
         * @param wallet
         */
        private void addWallet(Wallet wallet) {
            try {
                this.walletMap.put(wallet.getAddress(), wallet);
            } catch (Exception e) {
                log.error("Fail to add wallet ! ", e);
                throw new RuntimeException("Fail to add wallet!");
            }
        }

        /**
         * Get all wallet addresses
         *
         * @return
         */
        Set<String> getAddresses() {
            if (walletMap == null) {
                log.error("Fail to get address ! walletMap is null ! ");
                throw new RuntimeException("Fail to get addresses ! ");
            }
            return walletMap.keySet();
        }

        /**
         * Get wallet data
         *
         * @param address wallet address
         * @return
         */
        Wallet getWallet(String address) {
            // Check if the wallet address is valid
            try {
                Base58Check.base58ToBytes(address);
            } catch (Exception e) {
                log.error("Fail to get wallet ! address invalid ! address=" + address, e);
                throw new RuntimeException("Fail to get wallet ! ");
            }
            Wallet wallet = walletMap.get(address);
            if (wallet == null) {
                log.error("Fail to get wallet ! wallet don`t exist ! address=" + address);
                throw new RuntimeException("Fail to get wallet ! ");
            }
            return wallet;
        }
    }
}
