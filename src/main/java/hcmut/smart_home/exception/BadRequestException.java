package hcmut.smart_home.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BadRequestException extends ResponseStatusException {
	
	private static final String DEFAULT_MESSAGE = "Bad Request";

	public BadRequestException() {
		super(HttpStatus.BAD_REQUEST, DEFAULT_MESSAGE);
	}

	public BadRequestException(final String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}

}