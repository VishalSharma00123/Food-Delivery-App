package com.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ProfileNotFoundException.class)
	public ResponseEntity<Map<String, String>> notFound(ProfileNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(AddressNotFoundException.class)
	public ResponseEntity<Map<String, String>> addressNotFound(AddressNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(DuplicateProfileException.class)
	public ResponseEntity<Map<String, String>> conflict(DuplicateProfileException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, String>> badRequest(BadRequestException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.findFirst()
				.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", msg));
	}
}
