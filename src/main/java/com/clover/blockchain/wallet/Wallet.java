package com.clover.blockchain.wallet;

import com.clover.blockchain.util.Base58Check;
import com.clover.blockchain.util.BtcAddressUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;

@Data
@AllArgsConstructor
@Slf4j
public class Wallet implements Serializable {

    private static final long serialVersionUID = 166249065006236265L;

    /**
     * checksum length
     */
    private static final int ADDRESS_CHECKSUM_LEN = 4;
    /**
     * private key
     */
    private BCECPrivateKey privateKey;
    /**
     * public key
     */
    private byte[] publicKey;


    public Wallet() {
        initWallet();
    }

    /**
     * Initialize wallet
     */
    private void initWallet() {
        try {
            KeyPair keyPair = newECKeyPair();
            BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
            BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();

            byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);

            this.setPrivateKey(privateKey);
            this.setPublicKey(publicKeyBytes);
        } catch (Exception e) {
            log. error("Fail to init wallet ! ", e);
            throw new RuntimeException("Fail to init wallet ! ", e);
        }
    }

    /**
     * Create a new key pair
     *
     * @return
     * @throws Exception
     */
    private KeyPair newECKeyPair() throws Exception {
        // Register BC Provider
        Security. addProvider(new BouncyCastleProvider());
        // Create a key pair generator for the elliptic curve algorithm, the algorithm is ECDSA
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        // elliptic curve (EC) domain parameter setting
        ECParameterSpec ecSpec = ECNamedCurveTable. getParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        return keyPairGenerator. generateKeyPair();
    }


    /**
     * Get wallet address
     *
     * @return
     */
    public String getAddress() {
        try {
            // 1. Get ripemdHashedKey
            byte[] ripemdHashedKey = BtcAddressUtils.ripeMD160Hash(this.getPublicKey());

            // 2. Add version 0x00
            ByteArrayOutputStream addrStream = new ByteArrayOutputStream();
            addrStream. write((byte) 0);
            addrStream.write(ripemdHashedKey);
            byte[] versionedPayload = addrStream.toByteArray();

            // 3. Calculate the checksum
            byte[] checksum = BtcAddressUtils.checksum(versionedPayload);

            // 4. Get the combination of version + paylod + checksum
            addrStream.write(checksum);
            byte[] binaryAddress = addrStream.toByteArray();

            // 5. Execute Base58 conversion processing
            return Base58Check.rawBytesToBase58(binaryAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Fail to get wallet address ! ");
    }
}
