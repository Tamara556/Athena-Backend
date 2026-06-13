package com.athena.auth.constants;

public final class AuthConstants {

    public static final int FIRST_NAME_MAX_LENGTH = 100;
    public static final int LAST_NAME_MAX_LENGTH = 100;
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 72;

    public static final String USERNAME_PATTERN = "^[A-Za-z0-9._-]{3,50}$";
    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,72}$";

    public static final String LOGIN_REQUIRED = "login is required (email or username)";
    public static final String PASSWORD_REQUIRED = "password is required";
    public static final String REFRESH_TOKEN_REQUIRED = "refreshToken is required";
    public static final String FIRST_NAME_REQUIRED = "firstName is required";
    public static final String FIRST_NAME_MAX_LENGTH_MESSAGE =
            "firstName must be at most " + FIRST_NAME_MAX_LENGTH + " characters";
    public static final String LAST_NAME_REQUIRED = "lastName is required";
    public static final String LAST_NAME_MAX_LENGTH_MESSAGE =
            "lastName must be at most " + LAST_NAME_MAX_LENGTH + " characters";
    public static final String USERNAME_REQUIRED = "username is required";
    public static final String USERNAME_PATTERN_MESSAGE =
            "username must be " + USERNAME_MIN_LENGTH + "-" + USERNAME_MAX_LENGTH
                    + " characters: letters, digits, dot, underscore or hyphen";
    public static final String EMAIL_REQUIRED = "email is required";
    public static final String EMAIL_INVALID = "email must be a valid address";
    public static final String PASSWORD_SIZE_MESSAGE =
            "password must be between " + PASSWORD_MIN_LENGTH + " and " + PASSWORD_MAX_LENGTH + " characters";
    public static final String PASSWORD_COMPLEXITY_MESSAGE =
            "password must contain at least one uppercase letter, one lowercase letter, "
                    + "one digit and one symbol";

    public static final String JWT_SECRET_REQUIRED = "athena.security.jwt.secret must be configured";

    public static final String INVALID_CREDENTIALS = "Invalid login or password";
    public static final String EMAIL_ALREADY_EXISTS = "An account with this email already exists";
    public static final String USERNAME_ALREADY_TAKEN = "This username is already taken";
    public static final String REFRESH_TOKEN_INVALID = "Refresh token is invalid or expired";
    public static final String ACCOUNT_NO_LONGER_EXISTS = "Account no longer exists";
}
