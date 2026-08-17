package com.msp.enums;

public enum ERefundStatus {
    /** Customer asked for a refund; waiting for cashier to confirm goods returned. */
    PENDING_RETURN,
    /** Cashier confirmed return, inventory restocked, refund issued. */
    COMPLETED,
    /** Cashier rejected the return / refund request. */
    REJECTED
}
