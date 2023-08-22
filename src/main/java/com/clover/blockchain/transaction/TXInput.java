package com.clover.blockchain.transaction;

import com.clover.blockchain.util.BtcAddressUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TXInput {
    private byte[] txId;
    private int txOutputIndex;
    private byte[] signature;
    private byte[] pubKey;

    public boolean usesKey(byte[] pubKeyHash) {
        byte[] lockingHash = BtcAddressUtils.ripeMD160Hash(this.getPubKey());
        return Arrays.equals(lockingHash, pubKeyHash);
    }


    @Override
    public String toString() {
        return "TXInput{" +
                "txId=" + Arrays.toString(txId) +
                ", txOutputIndex=" + txOutputIndex +
                ", signature=" + Arrays.toString(signature) +
                ", pubKey=" + Arrays.toString(pubKey) +
                '}';
    }
}
