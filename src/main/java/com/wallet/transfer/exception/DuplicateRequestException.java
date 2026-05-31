package com.wallet.transfer.exception;

public class DuplicateRequestException extends RuntimeException {

    public DuplicateRequestException(String idempotencyKey){
        super(
                "Duplicate request detected for idempotency key : " + idempotencyKey
        );
    }

}
