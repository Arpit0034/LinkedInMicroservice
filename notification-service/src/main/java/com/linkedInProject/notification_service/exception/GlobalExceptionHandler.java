package com.linkedInProject.notification_service.exception;

import com.linkedInProject.notification_service.exception.ApiError;
import com.linkedInProject.notification_service.exception.BadRequestException;
import com.linkedInProject.notification_service.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException resourceNotFoundException){
        ApiError apiError = new ApiError(resourceNotFoundException.getLocalizedMessage(), HttpStatus.NOT_FOUND) ;
        return new ResponseEntity<>(apiError,HttpStatus.NOT_FOUND) ;
    }

    @ExceptionHandler(BadRequestException.class) // Client side error
    public ResponseEntity<ApiError> handleBadRequestException(BadRequestException badRequestException){
        ApiError apiError = new ApiError(badRequestException.getLocalizedMessage(), HttpStatus.BAD_REQUEST) ;
        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST) ;
    }

    @ExceptionHandler(RuntimeException.class) // Server side error
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException runtimeException){
        ApiError apiError = new ApiError(runtimeException.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR) ;
        return new ResponseEntity<>(apiError,HttpStatus.INTERNAL_SERVER_ERROR) ;
    }

}
