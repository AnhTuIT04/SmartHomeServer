package hcmut.smart_home.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ConflictException extends ResponseStatusException {

	public ConflictException(final String reason) {
		super(HttpStatus.CONFLICT, reason);
	}

}