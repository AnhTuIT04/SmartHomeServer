package hcmut.smart_home.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ForbiddenException extends ResponseStatusException {
	
	private static final String DEFAULT_MESSAGE = "Forbidden";

	public ForbiddenException() {
		super(HttpStatus.FORBIDDEN, DEFAULT_MESSAGE);
	}

	public ForbiddenException(final String message) {
		super(HttpStatus.FORBIDDEN, message);
	}

}