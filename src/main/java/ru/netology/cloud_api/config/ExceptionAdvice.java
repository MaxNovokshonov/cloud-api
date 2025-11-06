package ru.netology.cloud_api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.netology.cloud_api.dto.ErrorResponse;
import ru.netology.cloud_api.exception.BadCredentials400Exception;
import ru.netology.cloud_api.exception.BadRequest400Exception;
import ru.netology.cloud_api.exception.Unauthorized401Exception;

@ControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(BadCredentials400Exception.class)
    public ResponseEntity<ErrorResponse> badCreds(BadCredentials400Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(1001, ex.getMessage()));
    }

    @ExceptionHandler(Unauthorized401Exception.class)
    public ResponseEntity<ErrorResponse> unauthorized(Unauthorized401Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(1002, ex.getMessage()));
    }

    @ExceptionHandler(BadRequest400Exception.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequest400Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(2000, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(1003, msg));
    }
}


