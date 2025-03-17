package hcmut.smart_home.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NotFoundException extends ResponseStatusException {
	
	private static final String DEFAULT_MESSAGE = "Not Found";

	public NotFoundException() {
		super(HttpStatus.NOT_FOUND, DEFAULT_MESSAGE);
	}

	public NotFoundException(final String message) {
		super(HttpStatus.NOT_FOUND, message);
	}

}