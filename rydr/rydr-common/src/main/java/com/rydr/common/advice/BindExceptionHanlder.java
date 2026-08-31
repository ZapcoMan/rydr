package com.rydr.common.advice;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class BindExceptionHanlder {

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {BindException.class,ValidationException.class,MethodArgumentNotValidException.class})
    public ResponseResult handleBindException(HttpServletRequest request, Exception exception) {

        String exceptionClassName = exception.getClass().getName();
        String message = "Parameter error";
        switch (exceptionClassName){
            case "org.springframework.validation.BindException":
                message = resolveFieldError(((BindException) exception).getBindingResult());
                break;
            case "jakarta.validation.ValidationException":
                ValidationException validationException = (ValidationException) exception;
                message = validationException.getMessage();
                break;
            case "org.springframework.web.bind.MethodArgumentNotValidException":
                MethodArgumentNotValidException methodArgumentNotValidException =
                        (MethodArgumentNotValidException) exception;
                message = resolveFieldError(methodArgumentNotValidException.getBindingResult());
                break;
            default:
                break;
        }

        log.warn("Parameter binding rejected for {}: {}", request.getRequestURI(), message);

        // BusinessInterfaceStatus.FAIL == 1. Returning 0 here would be read as SUCCESS by
        // every caller that keys off ResponseResult, so the failure code must be explicit.
        return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), message);
    }

    /**
     * @return the message of the first field error, or the generic message when there is none
     */
    private String resolveFieldError(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        return fieldError == null ? "Parameter error" : fieldError.getDefaultMessage();
    }
}
