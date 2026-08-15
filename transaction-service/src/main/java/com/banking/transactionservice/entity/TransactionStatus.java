package com.banking.transactionservice.entity;


/**
 * PENDING -> PROCESSING -> COMPLETED (clean transaction)
 *                          -> PENDING VERIFICATION (suspicious detected)
 *                              -> COMPLETED (verified)
 *                              -> FLAGGED (SAGA REFUND)
 *                          -> FAILED
 *                          -> FLAGGED
 */
public enum TransactionStatus {

    PENDING,
    PROCESSING,
    COMPLETED,
    PENDING_VERIFICATION,
    FAILED,
    FLAGGED
}
