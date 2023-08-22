package com.clover.blockchain;

import com.clover.blockchain.block.Block;
import com.clover.blockchain.block.Blockchain;
import com.clover.blockchain.pow.ProofOfWork;
import com.clover.blockchain.transaction.TXOutput;
import com.clover.blockchain.transaction.Transaction;
import com.clover.blockchain.transaction.UTXOSet;
import com.clover.blockchain.util.Base58Check;
import com.clover.blockchain.wallet.Wallet;
import com.clover.blockchain.wallet.WalletUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Set;

@SpringBootTest
@Slf4j
@ExtendWith(SpringExtension.class)
class ApplicationTests {

    private final String wallet_address = "1KxqqpEpUcQdzMa2R3omeCvfkbGZ5dMfEt";

    private final String from_address = "1CceyiwYXh6vL6dLPw6WiNc5ihqVxwYHSA";

    private final String to_address = "1EKacQPNxTd8N7Y83VK11zoqm7bhUZiDHm";

    @Test
    void testCreateWallet() {
        Wallet wallet = WalletUtils.getInstance().createWallet();
        log.info("wallet address : " + wallet.getAddress());
    }

    @Test
    void testPrintAddresses() {
        Set<String> addresses = WalletUtils.getInstance().getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            log.info("There isn't address");
            return;
        }
        for (String address : addresses) {
            log.info("Wallet address: " + address);
        }
    }

    @Test
    void testGetBalance() {
        // Check if the wallet address is valid
        try {
            Base58Check.base58ToBytes(wallet_address);
        } catch (Exception e) {
            log.error("ERROR: invalid wallet address", e);
            throw new RuntimeException("ERROR: invalid wallet address", e);
        }

        // Get the public key Hash value
        byte[] versionedPayload = Base58Check.base58ToBytes(wallet_address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);

        Blockchain blockchain = Blockchain.createBlockchain(wallet_address);
        UTXOSet utxoSet = new UTXOSet().blockchain(blockchain);

        TXOutput[] txOutputs = utxoSet.findUTXOs(pubKeyHash);
        int balance = 0;
        if (txOutputs != null && txOutputs.length > 0) {
            for (TXOutput txOutput : txOutputs) {
                balance += txOutput.getValue();
            }
        }
        log.info("Balance of '{}': {}\n", new Object[]{wallet_address, balance});
    }

    @Test
    void testCreateBlockChain() {
        Blockchain blockchain = Blockchain.createBlockchain(to_address);
        UTXOSet utxoSet = new UTXOSet().blockchain(blockchain);
        utxoSet.reIndex();
        log.info("Done ! " + blockchain);

    }

    @Test
    void testPrintBlockChain() throws Exception {
        Blockchain blockchain = Blockchain.initBlockchainFromDB();
        for (Blockchain.BlockchainIterator iterator = blockchain.getBlockchainIterator(); iterator.hashNext(); ) {
            Block block = iterator.next();
            if (block != null) {
                boolean validate = ProofOfWork.newProofOfWork(block).validate();
                log.info(block + ", validate = " + validate);
            }
        }
    }

    @Test
    void testSendCoin() throws Exception {
        int amount = 1;
        // Check if the wallet address is valid
        try {
            Base58Check.base58ToBytes(from_address);
        } catch (Exception e) {
            log.error("ERROR: sender address invalid ! address=" + from_address, e);
            throw new RuntimeException("ERROR: sender address invalid ! address=" + from_address, e);
        }
        // Check if the wallet address is valid
        try {
            Base58Check.base58ToBytes(to_address);
        } catch (Exception e) {
            log.error("ERROR: receiver address invalid ! address=" + to_address, e);
            throw new RuntimeException("ERROR: receiver address invalid ! address=" + to_address, e);
        }
        if (amount < 1) {
            log.error("ERROR: amount invalid ! amount=" + 1);
            throw new RuntimeException("ERROR: amount invalid ! amount=" + amount);
        }
        Blockchain blockchain = Blockchain.createBlockchain(from_address);
        // new transaction
        Transaction transaction = Transaction.newUTXOTransaction(from_address, to_address, amount, blockchain);
        // award
        Transaction rewardTx = Transaction.newCoinbaseTX(from_address, "");
        Block newBlock = blockchain.mineBlock(new Transaction[]{transaction, rewardTx});
        new UTXOSet().blockchain(blockchain).update(newBlock);
        log.info("Success!");
    }

}
