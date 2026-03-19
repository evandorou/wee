package com.evandorou.bets.api;

import com.evandorou.bets.service.BetDomainException;
import com.evandorou.bets.service.BetNotFoundException;
import com.evandorou.bets.service.ForbiddenBetAccessException;
import com.evandorou.bets.service.InsufficientBalanceException;
import com.evandorou.bets.service.InvalidBetException;
import com.evandorou.bets.service.ResultNotAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BetsController.class)
public class BetsExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorBody> insufficientBalance(InsufficientBalanceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ResultNotAvailableException.class)
    public ResponseEntity<ApiErrorBody> resultNotReady(ResultNotAvailableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidBetException.class)
    public ResponseEntity<ApiErrorBody> invalidBet(InvalidBetException e) {
        return ResponseEntity.badRequest().body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(BetNotFoundException.class)
    public ResponseEntity<ApiErrorBody> notFound(BetNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenBetAccessException.class)
    public ResponseEntity<ApiErrorBody> forbidden(ForbiddenBetAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorBody> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(new ApiErrorBody("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(BetDomainException.class)
    public ResponseEntity<ApiErrorBody> genericBet(BetDomainException e) {
        return ResponseEntity.badRequest().body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }
}
