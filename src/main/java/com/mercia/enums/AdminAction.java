package com.mercia.enums;

public enum AdminAction {
    FREEZE_WALLET,      // froze a user's wallet
    UNFREEZE_WALLET,    // unfroze a user's wallet
    BLOCK_TRANSACTION,  // blocked a high-value transaction via fraud alert
    DISMISS_ALERT,      // dismissed a fraud alert
    MANUAL_SETTLE       // manually triggered settlement for a transaction
}
