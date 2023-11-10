package com.limvik.econome.global.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(ErrorException e) {
        ErrorResponse response = new ErrorResponse(e.getErrorCode().name(), e.getMessage());
        log.error("ErrorException {}",e.getMessage());
        return new ResponseEntity<>(response, e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse ValidExceptionHandler(BindingResult bindingResult){

        List<ObjectError> errors = bindingResult.getAllErrors();
        for (ObjectError error: errors){
            log.info("error.getDefaultMessage() = {} ", error.getDefaultMessage());
        }

        String errorReason = errors.get(0).getDefaultMessage();
        return new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.name(), errorReason);
    }

    @ExceptionHandler(JsonProcessingException.class)
    protected ResponseEntity<ErrorResponse> jsonCustomException(ErrorException e) {
        ErrorResponse response = new ErrorResponse(e.getErrorCode().name(),e.getMessage());
        log.error("ErrorException {}",e.getMessage());
        return new ResponseEntity<>(response,e.getErrorCode().getHttpStatus());
    }

}
