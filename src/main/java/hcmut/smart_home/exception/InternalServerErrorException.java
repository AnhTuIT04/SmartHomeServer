package hcmut.smart_home.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InternalServerErrorException extends ResponseStatusException {
	
	private static final String DEFAULT_MESSAGE = "Internal server error";

	public InternalServerErrorException() {
		super(HttpStatus.INTERNAL_SERVER_ERROR, DEFAULT_MESSAGE);
	}

	public InternalServerErrorException(final String message) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}

}