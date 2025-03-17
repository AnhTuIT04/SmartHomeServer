package hcmut.smart_home.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnauthorizedException extends ResponseStatusException {
	
	private static final String DEFAULT_MESSAGE = "Unauthorized";

	public UnauthorizedException() {
		super(HttpStatus.UNAUTHORIZED, DEFAULT_MESSAGE);
	}

	public UnauthorizedException(final String message) {
		super(HttpStatus.UNAUTHORIZED, message);
	}

}