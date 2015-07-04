package uk.co.spookypeanut.loveandtag;

import java.io.IOException;

public class InvalidCredentialsException extends IOException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
    public InvalidCredentialsException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
