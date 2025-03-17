package hcmut.smart_home.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AccountAlreadyExistsException extends ResponseStatusException {

	public AccountAlreadyExistsException(final String reason) {
		super(HttpStatus.CONFLICT, reason);
	}

}