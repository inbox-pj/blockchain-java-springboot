package com.clover.blockchain.transaction;

import com.clover.blockchain.util.Base58Check;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TXOutput {
    private int value;
    private byte[] pubKeyHash;

    // create transaction output
    public static TXOutput newTXOutput(int value, String address) {
        // reserv conversion to byte array
        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        return new TXOutput(value, pubKeyHash);
    }

    // Check whether the transaction output can use the specified public key
    public boolean isLockedWithKey(byte[] pubKeyHash) {
        return Arrays.equals(this.getPubKeyHash(), pubKeyHash);
    }


    @Override
    public String toString() {
        return "TXOutput{" +
                "value=" + value +
                ", pubKeyHash='" + pubKeyHash + '\'' +
                '}';
    }
}
