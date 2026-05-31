package com.wallet.transfer.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ProblemDetail common(HttpStatus status, String msg, String title){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, msg);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ProblemDetail handleWalletNotFound(WalletNotFoundException ex){
        log.warn("Wallet not found : {}", ex.getMessage());
        return common(HttpStatus.NOT_FOUND, ex.getMessage(), "Wallet Not Found");
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex){
        log.warn("Insufficient Balance : {}", ex.getMessage());
        return common(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "Insufficient Balance");
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ProblemDetail handleDuplicateRequest(DuplicateRequestException ex){
        log.warn("Duplicate Reqeust {}", ex.getMessage());
        return common(HttpStatus.CONFLICT, ex.getMessage(), "Duplicate Request");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex){
        log.warn("Optimistic lock conflict : {}", ex.getMessage());
        return common(HttpStatus.CONFLICT, "Concurrent Modification detected. Please retry", "Conflict");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIlegalArgument(IllegalArgumentException ex){
        log.warn("Invalid Argument : {}", ex.getMessage());
        return common(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid Request");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex){
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation Failed");

        return common(HttpStatus.BAD_REQUEST, detail, "Validation Error");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error ", ex);
        return common(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occured", "Internal Server Error");
    }

}
