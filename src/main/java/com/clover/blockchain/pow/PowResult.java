package com.clover.blockchain.pow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// workload calculation result
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PowResult {
    private long nonce;

    private String hash;

}
