package codex.mmxxvi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AppExceptions {

    private AppExceptions() {
    }

    public static class ResourceNotFoundException extends ResponseStatusException {
        public ResourceNotFoundException(String message){
            super(HttpStatus.NOT_FOUND, message);
        }

        public ResourceNotFoundException(String message, Throwable cause){
            super(HttpStatus.NOT_FOUND, message, cause);
        }
    }

    public static class BadRequestException extends ResponseStatusException {
        public BadRequestException(String message) {
            super(HttpStatus.BAD_REQUEST, message);
        }

        public BadRequestException(String message, Throwable cause) {
            super(HttpStatus.BAD_REQUEST, message, cause);
        }
    }

    public static class UnauthorizedException extends ResponseStatusException {
        public UnauthorizedException(String message) {
            super(HttpStatus.UNAUTHORIZED, message);
        }

        public UnauthorizedException(String message, Throwable cause) {
            super(HttpStatus.UNAUTHORIZED, message, cause);
        }
    }

    public static class ForbiddenException extends ResponseStatusException {
        public ForbiddenException(String message) {
            super(HttpStatus.FORBIDDEN, message);
        }

        public ForbiddenException(String message, Throwable cause) {
            super(HttpStatus.FORBIDDEN, message, cause);
        }
    }

    public static class ServiceUnavailableException extends ResponseStatusException {
        public ServiceUnavailableException(String message) {
            super(HttpStatus.SERVICE_UNAVAILABLE, message);
        }

        public ServiceUnavailableException(String message, Throwable cause) {
            super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
        }
    }

    public static class InternalServerErrorException extends ResponseStatusException {
        public InternalServerErrorException(String message){
            super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }

        public InternalServerErrorException(String message, Throwable cause){
            super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        }
    }

    public static class ConflictException extends ResponseStatusException {
        public ConflictException(String message) {
            super(HttpStatus.CONFLICT, message);
        }

        public ConflictException(String message, Throwable cause) {
            super(HttpStatus.CONFLICT, message, cause);
        }
    }

    public static class BadGatewayException extends ResponseStatusException {
        public BadGatewayException(String message) {
            super(HttpStatus.BAD_GATEWAY, message);
        }

        public BadGatewayException(String message, Throwable cause) {
            super(HttpStatus.BAD_GATEWAY, message, cause);
        }
    }
}
