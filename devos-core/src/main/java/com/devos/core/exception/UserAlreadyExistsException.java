package com.devos.core.exception;

public class UserAlreadyExistsException extends DevosException {
    
    public UserAlreadyExistsException(String username) {
        super("User with username '" + username + "' already exists");
    }
    
    public UserAlreadyExistsException(String username, String email) {
        super("User with username '" + username + "' or email '" + email + "' already exists");
    }
}
