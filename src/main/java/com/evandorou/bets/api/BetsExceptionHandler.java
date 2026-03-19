package com.evandorou.bets.api;

import com.evandorou.bets.service.BetDomainException;
import com.evandorou.bets.service.BetNotFoundException;
import com.evandorou.bets.service.EventResultConflictException;
import com.evandorou.bets.service.ForbiddenBetAccessException;
import com.evandorou.bets.service.InsufficientBalanceException;
import com.evandorou.bets.service.InvalidBetException;
import com.evandorou.bets.service.ResultNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BetsController.class)
public class BetsExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BetsExceptionHandler.class);

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorBody> insufficientBalance(InsufficientBalanceException e) {
        log.info("Bet rejected: {} — {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ResultNotAvailableException.class)
    public ResponseEntity<ApiErrorBody> resultNotReady(ResultNotAvailableException e) {
        log.info("Settlement blocked: {} — {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(EventResultConflictException.class)
    public ResponseEntity<ApiErrorBody> eventResultConflict(EventResultConflictException e) {
        log.warn("Event result conflict: {} — {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidBetException.class)
    public ResponseEntity<ApiErrorBody> invalidBet(InvalidBetException e) {
        log.debug("Invalid bet request: {} — {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(BetNotFoundException.class)
    public ResponseEntity<ApiErrorBody> notFound(BetNotFoundException e) {
        log.debug("Bet not found: {} — {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenBetAccessException.class)
    public ResponseEntity<ApiErrorBody> forbidden(ForbiddenBetAccessException e) {
        log.warn("Forbidden bet access: {} — {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorBody> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        log.debug("Request validation failed: {}", msg);
        return ResponseEntity.badRequest().body(new ApiErrorBody("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(BetDomainException.class)
    public ResponseEntity<ApiErrorBody> genericBet(BetDomainException e) {
        log.debug("Bet domain error: {} — {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(new ApiErrorBody(e.getCode(), e.getMessage()));
    }
}
