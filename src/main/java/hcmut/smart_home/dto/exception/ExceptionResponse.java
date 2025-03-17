package hcmut.smart_home.dto.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "Error", accessMode = Schema.AccessMode.READ_ONLY)
public class ExceptionResponse<T> {

	private String status;
	private T description;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getDescription() {
        return description;
    }

    public void setDescription(T description) {
        this.description = description;
    }

}