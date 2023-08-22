package com.clover.blockchain.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpendableOutputResult {

    private int accumulated;
    private Map<String, int[]> unspentOuts;

}
