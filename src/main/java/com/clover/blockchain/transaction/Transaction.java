package com.clover.blockchain.transaction;

import com.clover.blockchain.block.Blockchain;
import com.clover.blockchain.kryo.KryoSerializer;
import com.clover.blockchain.util.BtcAddressUtils;
import com.clover.blockchain.wallet.Wallet;
import com.clover.blockchain.wallet.WalletUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private static final int SUBSIDY = 10;

    private byte[] txId;
    private TXInput[] inputs;
    private TXOutput[] outputs;
    private long createTime;

    // calculate hash value of transaction details
    public byte[] hash() {
        // Use serialization to deep copy the Transaction object
        byte[] serializeBytes = KryoSerializer.serialize(this);
        Transaction copyTx = (Transaction) KryoSerializer.deserialize(serializeBytes);
        copyTx.setTxId(new byte[]{});
        return DigestUtils.sha256(KryoSerializer.serialize(copyTx));
    }

    // create a coinbase transaction
    public static Transaction newCoinbaseTX(String to, String data) {
        if (StringUtils.isBlank(data)) {
            data = String.format("Reward to '%s'", to);
        }
        // // create txn input
        TXInput txInput = new TXInput(new byte[]{}, -1, null, data.getBytes());
        // create txn output
        TXOutput txOutput = TXOutput.newTXOutput(SUBSIDY, to);
        // create txn
        Transaction tx = new Transaction(null, new TXInput[]{txInput},
                new TXOutput[]{txOutput}, System.currentTimeMillis());
        // set txn id
        tx.setTxId(tx.hash());
        return tx;
    }

    public boolean isCoinbase() {
        return this.getInputs().length == 1
                && this.getInputs()[0].getTxId().length == 0
                && this.getInputs()[0].getTxOutputIndex() == -1;
    }

    // Pay a certain amount from from to to
    public static Transaction newUTXOTransaction(String from, String to, int amount, Blockchain blockchain) throws Exception {
        Wallet senderWallet = WalletUtils.getInstance().getWallet(from);
        byte[] pubKey = senderWallet.getPublicKey();
        byte[] pubKeyHash = BtcAddressUtils.ripeMD160Hash(pubKey);

        SpendableOutputResult result = new UTXOSet().blockchain(blockchain).findSpendableOutputs(pubKeyHash, amount);
        int accumulated = result.getAccumulated();
        Map<String, int[]> unspentOuts = result.getUnspentOuts();

        if (accumulated < amount) {
            log.error("ERROR: Not enough funds ! accumulated=" + accumulated + ", amount=" + amount);
            throw new RuntimeException("ERROR: Not enough funds ! ");
        }
        Iterator<Map.Entry<String, int[]>> iterator = unspentOuts.entrySet().iterator();

        TXInput[] txInputs = {};
        while (iterator.hasNext()) {
            Map.Entry<String, int[]> entry = iterator.next();
            String txIdStr = entry.getKey();
            int[] outIds = entry.getValue();
            byte[] txId = Hex.decodeHex(txIdStr);
            for (int outIndex : outIds) {
                txInputs = ArrayUtils.add(txInputs, new TXInput(txId, outIndex, null, pubKey));
            }
        }

        TXOutput[] txOutput = {};
        txOutput = ArrayUtils.add(txOutput, TXOutput.newTXOutput(amount, to));
        if (accumulated > amount) {
            txOutput = ArrayUtils.add(txOutput, TXOutput.newTXOutput((accumulated - amount), from));
        }

        Transaction newTx = new Transaction(null, txInputs, txOutput, System.currentTimeMillis());
        newTx.setTxId(newTx.hash());

        blockchain.signTransaction(newTx, senderWallet.getPrivateKey());

        return newTx;
    }

    // Create a copy of the transaction data for signing, the signature and pubKey of the transaction input need to be set to null
    public Transaction trimmedCopy() {
        TXInput[] tmpTXInputs = new TXInput[this.getInputs().length];
        for (int i = 0; i < this.getInputs().length; i++) {
            TXInput txInput = this.getInputs()[i];
            tmpTXInputs[i] = new TXInput(txInput.getTxId(), txInput.getTxOutputIndex(), null, null);
        }

        TXOutput[] tmpTXOutputs = new TXOutput[this.getOutputs().length];
        for (int i = 0; i < this.getOutputs().length; i++) {
            TXOutput txOutput = this.getOutputs()[i];
            tmpTXOutputs[i] = new TXOutput(txOutput.getValue(), txOutput.getPubKeyHash());
        }

        return new Transaction(this.getTxId(), tmpTXInputs, tmpTXOutputs, this.getCreateTime());
    }

    public void sign(BCECPrivateKey privateKey, Map<String, Transaction> prevTxMap) throws Exception {
        // The coinbase transaction information does not need to be signed, because there is no transaction input information
        if (this.isCoinbase()) {
            return;
        }
        // Verify again whether the transaction input in the transaction information is correct, that is, whether you can find the corresponding transaction data
        for (TXInput txInput : this.getInputs()) {
            if (prevTxMap.get(Hex.encodeHexString(txInput.getTxId())) == null) {
                throw new RuntimeException("ERROR: Previous transaction is not correct");
            }
        }

        // Create a copy of the transaction information used for signing
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME);
        ecdsaSign.initSign(privateKey);

        for (int i = 0; i < txCopy.getInputs().length; i++) {
            TXInput txInputCopy = txCopy.getInputs()[i];
            // Get the transaction data corresponding to the transaction input TxID
            Transaction prevTx = prevTxMap.get(Hex.encodeHexString(txInputCopy.getTxId()));
            // Get the transaction output in the previous transaction corresponding to the transaction input
            TXOutput prevTxOutput = prevTx.getOutputs()[txInputCopy.getTxOutputIndex()];
            txInputCopy.setPubKey(prevTxOutput.getPubKeyHash());
            txInputCopy.setSignature(null);
            // Get the data to be signed, that is, the transaction ID
            txCopy.setTxId(txCopy.hash());
            txInputCopy.setPubKey(null);

            // Only sign the entire transaction information, that is, sign the transaction ID
            ecdsaSign.update(txCopy.getTxId());
            byte[] signature = ecdsaSign.sign();

            // Assign the signature of the entire transaction data to the transaction input, because the transaction input needs to contain the signature of the entire transaction information
            // Note that the obtained signature is assigned to the transaction input in the original transaction information
            this.getInputs()[i].setSignature(signature);
        }
    }

    public boolean verify(Map<String, Transaction> prevTxMap) throws Exception {
        // Coinbase transaction information does not need to be signed, so it does not need to be verified
        if (this.isCoinbase()) {
            return true;
        }

        // Verify again whether the transaction input in the transaction information is correct, that is, whether you can find the corresponding transaction data
        for (TXInput txInput : this.getInputs()) {
            if (prevTxMap.get(Hex.encodeHexString(txInput.getTxId())) == null) {
                throw new RuntimeException("ERROR: Previous transaction is not correct");
            }
        }

        // Create a copy of transaction information for signature verification
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        ECParameterSpec ecParameters = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME);

        for (int i = 0; i < this.getInputs().length; i++) {
            TXInput txInput = this.getInputs()[i];
            // Get the transaction data corresponding to the transaction input TxID
            Transaction prevTx = prevTxMap.get(Hex.encodeHexString(txInput.getTxId()));
            // Get the transaction output in the previous transaction corresponding to the transaction input
            TXOutput prevTxOutput = prevTx.getOutputs()[txInput.getTxOutputIndex()];

            TXInput txInputCopy = txCopy.getInputs()[i];
            txInputCopy.setSignature(null);
            txInputCopy.setPubKey(prevTxOutput.getPubKeyHash());
            // Get the data to be signed, that is, the transaction ID
            txCopy.setTxId(txCopy.hash());
            txInputCopy.setPubKey(null);

            // Use elliptic curve x, y points to generate public key Key
            BigInteger x = new BigInteger(1, Arrays.copyOfRange(txInput.getPubKey(), 1, 33));
            BigInteger y = new BigInteger(1, Arrays.copyOfRange(txInput.getPubKey(), 33, 65));
            ECPoint ecPoint = ecParameters.getCurve().createPoint(x, y);

            ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, ecParameters);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(txCopy.getTxId());
            if (!ecdsaVerify.verify(txInput.getSignature())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "txId=" + Arrays.toString(txId) +
                ", inputs=" + Arrays.toString(inputs) +
                ", outputs=" + Arrays.toString(outputs) +
                ", createTime=" + createTime +
                '}';
    }
}
