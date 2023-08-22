<img width="587" alt="image" src="https://github.com/inbox-pj/blockchain-java-springboot/assets/53929164/52771393-4f60-4aac-901d-488b15a6522e">


## Table of Contents
- [About](#about)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Block Structure](#block-structure)
  - [2FA Scenario](#2fa-Scenario)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [License](#license)

## About


## Features


## Getting Started
### Block Structure:

- Block Size
- Block Header
    - Version
    - hash
    - Previous Block Hash
    - Merkle Root
    - Timestamp
    - Difficulty Target
    - Nonce
- Transaction Counter
- Transactions
    - Hash Id
    - Amount
    - Fee
    - From
    - To
    - Status

 ```json
{
    "hash" : "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f",
    "confirmations" : 308321,
    "size" : 285,
    "height" : 0,
    "version" : 1,
    "merkleroot" : "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
    "tx" : [
        "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"
    ],
    "time" : 1231006505,
    "nonce" : 2083236893,
    "bits" : "1d00ffff",
    "difficulty" : 1.00000000,
    "nextblockhash" : "00000000839a8e6886ab5951d76f411475428afc90947ee320161bbf18eb6048"
}

{

	"unspent_outputs":[

		{
			"tx_hash":"186f9f998a5...2836dd734d2804fe65fa35779",
			"tx_index":104810202,
			"tx_output_n": 0,
			"script":"76a9147f9b1a7fb68d60c536c2fd8aeaa53a8f3cc025a888ac",
			"value": 10000000,
			"value_hex": "00989680",
			"confirmations":0
		}

	]
}

```

### 2FA Scenario
Private Key -> (Elliptic Curve) => Public Key -> (Hashing) => Address

## Prerequisites


## Installation


## Usage


## API Documentation


## Contributing


## License


## Contact


---
