package com.clover.blockchain.controller;

import com.clover.blockchain.block.Block;
import com.clover.blockchain.block.Blockchain;
import com.clover.blockchain.pow.ProofOfWork;
import com.clover.blockchain.transaction.TXOutput;
import com.clover.blockchain.transaction.Transaction;
import com.clover.blockchain.transaction.UTXOSet;
import com.clover.blockchain.util.Base58Check;
import com.clover.blockchain.wallet.Wallet;
import com.clover.blockchain.wallet.WalletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/")
@Slf4j
public class BlockchainController {

    @Autowired
    private Blockchain blockChain;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("/create_wallet")
    public ResponseEntity createWaller() {
        Wallet wallet = WalletUtils.getInstance().createWallet();
        HttpHeaders headers = new HttpHeaders();

        return new ResponseEntity<>(wallet.getAddress(), new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/get_addresses")
    public ResponseEntity getAddress() {
        Set<String> addresses = WalletUtils.getInstance().getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            new ResponseEntity<>("There isn't address", new HttpHeaders(), HttpStatus.NO_CONTENT);
        }
        Set<String> rs = new HashSet<>();

        for (String address : addresses) {
            rs.add(address);
        }

        return new ResponseEntity<>(rs, new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping("/get_balance")
    public ResponseEntity testGetBalance(String wallet_address) {
        // Check if the wallet address is valid
        try {
            Base58Check.base58ToBytes(wallet_address);
        } catch (Exception e) {
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
        Map<String, String> balanceMap = new HashMap<>();
        balanceMap.put("Wallet_Address", wallet_address);
        balanceMap.put("Balance", String.valueOf(balance));

        return new ResponseEntity<>(balanceMap, new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/create_blockchain")
    public ResponseEntity testCreateBlockChain(String wallet_address) {
        Blockchain blockchain = Blockchain.createBlockchain(wallet_address);
        UTXOSet utxoSet = new UTXOSet().blockchain(blockchain);
        utxoSet.reIndex();
        return new ResponseEntity<>(blockchain.getLastBlockHash(), new HttpHeaders(), HttpStatus.OK);

    }

    @GetMapping("/print_blockchain")
    public ResponseEntity testPrintBlockChain() throws Exception {
        Map<Block, Boolean> blocks = new HashMap<>();
        Blockchain blockchain = Blockchain.initBlockchainFromDB();

        for (Blockchain.BlockchainIterator iterator = blockchain.getBlockchainIterator(); iterator.hashNext(); ) {
            Block block = iterator.next();
            if (block != null) {
                boolean validate = ProofOfWork.newProofOfWork(block).validate();
                blocks.put(block, validate);
            }
        }
        return new ResponseEntity<>(blocks, new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/send_coin")
    public ResponseEntity testSendCoin(String from_address, String to_address, int amount) throws Exception {
        // Check if the wallet address is valid
        try {
            Base58Check.base58ToBytes(from_address);
        } catch (Exception e) {
            throw new RuntimeException("ERROR: sender address invalid ! address=" + from_address, e);
        }
        // Check if the wallet address is valid
        try {
            Base58Check.base58ToBytes(to_address);
        } catch (Exception e) {
            throw new RuntimeException("ERROR: receiver address invalid ! address=" + to_address, e);
        }
        if (amount < 1) {
            throw new RuntimeException("ERROR: amount invalid ! amount=" + amount);
        }
        Blockchain blockchain = Blockchain.createBlockchain(from_address);
        // new transaction
        Transaction transaction = Transaction.newUTXOTransaction(from_address, to_address, amount, blockchain);
        // award
        Transaction rewardTx = Transaction.newCoinbaseTX(from_address, "");
        Block newBlock = blockchain.mineBlock(new Transaction[]{transaction, rewardTx});
        new UTXOSet().blockchain(blockchain).update(newBlock);

        return new ResponseEntity<>(newBlock, new HttpHeaders(), HttpStatus.OK);
    }
}
