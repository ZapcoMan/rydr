package com.rydr.exception;

import com.rydr.constatnt.BusinessInterfaceStatus;
import com.rydr.dto.ResponseResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Single application-wide exception handler.
 *
 * <p>Replaces the previous pair of {@code GlobalExceptionHandler} + {@code BindExceptionHanlder}
 * so every error travels back to the caller as a {@link ResponseResult} with a consistent
 * code: {@link BusinessInterfaceStatus#FAIL} (=1) on failure, never the silent 0 that callers
 * interpret as success.</p>
 *
 * @author oi
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * Validation / binding errors -> 400 with the field message.
	 */
	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	@ExceptionHandler({BindException.class, ValidationException.class, MethodArgumentNotValidException.class})
	public ResponseResult handleBindException(HttpServletRequest request, Exception exception) {
		String message = "Parameter error";
		if (exception instanceof BindException bind) {
			message = resolveFieldError(bind.getBindingResult());
		} else if (exception instanceof MethodArgumentNotValidException method) {
			message = resolveFieldError(method.getBindingResult());
		} else if (exception instanceof ValidationException validation) {
			message = validation.getMessage();
		}
		log.warn("Parameter binding rejected for {}: {}", request.getRequestURI(), message);
		return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), message);
	}

	/**
	 * Any other uncaught exception -> 500 with a generic message (details stay in the log).
	 */
	@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	public ResponseResult handleException(HttpServletRequest request, Exception exception) {
		log.error("Unhandled exception at {}: {}", request.getRequestURI(), exception.getMessage(), exception);
		return ResponseResult.fail(BusinessInterfaceStatus.FAIL.getCode(), "Internal server error");
	}

	private String resolveFieldError(BindingResult bindingResult) {
		FieldError fieldError = bindingResult.getFieldError();
		return fieldError == null ? "Parameter error" : fieldError.getDefaultMessage();
	}
}
