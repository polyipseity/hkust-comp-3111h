package library.controls;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserValidatorTest {

    private final UserValidator validator = UserValidator.DEFAULT;

    @Test
    void testDefaultValidatorIsNotNull() {
        assertNotNull(UserValidator.DEFAULT);
    }

    @Test
    void testValidInputReturnsSuccess() {
        UserValidator.Result result = validator.apply("john_doe", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testEmptyUsernameReturnsBadUsername() {
        UserValidator.Result result = validator.apply("", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadUsername);
        assertEquals("Empty username", ((UserValidator.Result.BadUsername) result).message());
    }

    @Test
    void testUsernameWithControlCharactersReturnsBadUsername() {
        UserValidator.Result result = validator.apply("user\0name", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadUsername);
        assertEquals("Bad characters in username", ((UserValidator.Result.BadUsername) result).message());
    }

    @Test
    void testUsernameWithTabCharacterReturnsBadUsername() {
        UserValidator.Result result = validator.apply("user\tname", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadUsername);
        assertEquals("Bad characters in username", ((UserValidator.Result.BadUsername) result).message());
    }

    @Test
    void testUsernameWithNewlineReturnsBadUsername() {
        UserValidator.Result result = validator.apply("user\nname", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadUsername);
        assertEquals("Bad characters in username", ((UserValidator.Result.BadUsername) result).message());
    }

    @Test
    void testUsernameWithNonAsciiCharactersReturnsBadUsername() {
        UserValidator.Result result = validator.apply("usérnàme", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadUsername);
        assertEquals("Bad characters in username", ((UserValidator.Result.BadUsername) result).message());
    }

    @Test
    void testShortPasswordReturnsBadPassword() {
        UserValidator.Result result = validator.apply("john_doe", "short", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadPassword);
        assertEquals("Password too short", ((UserValidator.Result.BadPassword) result).message());
    }

    @Test
    void testEmptyFullNameReturnsBadFullName() {
        UserValidator.Result result = validator.apply("john_doe", "password123", "");
        assertTrue(result instanceof UserValidator.Result.BadFullName);
        assertEquals("Empty full name", ((UserValidator.Result.BadFullName) result).message());
    }

    @Test
    void testFullNameWithControlCharactersReturnsBadFullName() {
        UserValidator.Result result = validator.apply("john_doe", "password123", "John\0Doe");
        assertTrue(result instanceof UserValidator.Result.BadFullName);
        assertEquals("Bad characters in full name", ((UserValidator.Result.BadFullName) result).message());
    }

    @Test
    void testFullNameWithNonAsciiButAbove32IsValid() {
        UserValidator.Result result = validator.apply("john_doe", "password123", "José García");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testValidUsernameWithNumbersAndUnderscore() {
        UserValidator.Result result = validator.apply("user_123", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testValidUsernameWithDots() {
        UserValidator.Result result = validator.apply("user.name", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testValidUsernameWithHyphens() {
        UserValidator.Result result = validator.apply("user-name", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testValidPasswordExactly8Characters() {
        UserValidator.Result result = validator.apply("john_doe", "12345678", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testValidPasswordWithSpecialCharacters() {
        UserValidator.Result result = validator.apply("john_doe", "pass!@#$%", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testValidPasswordWithNonASCII() {
        UserValidator.Result result = validator.apply("john_doe", "usérnàme", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadPassword);
        assertEquals("Bad characters in password", ((UserValidator.Result.BadPassword) result).message());
    }

    @Test
    void testValidFullName() {
        UserValidator.Result result = validator.apply("john_doe", "password123", "Anne-Marie Smith");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testResultHierarchy() {
        UserValidator.Result success = new UserValidator.Result.Success();
        UserValidator.Result badUsername = new UserValidator.Result.BadUsername("test");
        UserValidator.Result badPassword = new UserValidator.Result.BadPassword("test");
        UserValidator.Result badFullName = new UserValidator.Result.BadFullName("test");

        assertTrue(success instanceof UserValidator.Result);
        assertTrue(badUsername instanceof UserValidator.Result);
        assertTrue(badPassword instanceof UserValidator.Result);
        assertTrue(badFullName instanceof UserValidator.Result);

        assertTrue(badUsername instanceof library.utils.HasMessage);
        assertTrue(badPassword instanceof library.utils.HasMessage);
        assertTrue(badFullName instanceof library.utils.HasMessage);

        assertEquals("test", ((library.utils.HasMessage) badUsername).getMessage());
        assertEquals("test", ((library.utils.HasMessage) badPassword).getMessage());
        assertEquals("test", ((library.utils.HasMessage) badFullName).getMessage());
    }

    @Test
    void testValidationOrder_UsernameFirst() {
        UserValidator.Result result = validator.apply("", "short", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadUsername);
        assertEquals("Empty username", ((UserValidator.Result.BadUsername) result).message());
    }

    @Test
    void testValidationOrder_PasswordAfterUsername() {
        UserValidator.Result result = validator.apply("validuser", "short", "John Doe");
        assertTrue(result instanceof UserValidator.Result.BadPassword);
        assertEquals("Password too short", ((UserValidator.Result.BadPassword) result).message());
    }

    @Test
    void testValidationOrder_FullNameLast() {
        UserValidator.Result result = validator.apply("validuser", "validpassword123", "");
        assertTrue(result instanceof UserValidator.Result.BadFullName);
        assertEquals("Empty full name", ((UserValidator.Result.BadFullName) result).message());
    }

    @Test
    void testMultipleValidationFailures_ReturnsFirstFailure() {
        UserValidator.Result result = validator.apply("", "short", "");
        assertTrue(result instanceof UserValidator.Result.BadUsername);
        assertEquals("Empty username", ((UserValidator.Result.BadUsername) result).message());
    }

    @Test
    void testRecordGetters() {
        UserValidator.Result.BadUsername badUsername = new UserValidator.Result.BadUsername("Username error");
        UserValidator.Result.BadPassword badPassword = new UserValidator.Result.BadPassword("Password error");
        UserValidator.Result.BadFullName badFullName = new UserValidator.Result.BadFullName("Full name error");

        assertEquals("Username error", badUsername.message());
        assertEquals("Password error", badPassword.message());
        assertEquals("Full name error", badFullName.message());
    }

    @Test
    void testUsernameWithSpaceIsValid() {
        UserValidator.Result result = validator.apply("user name", "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testPasswordWithSpaceIsValid() {
        UserValidator.Result result = validator.apply("john_doe", "pass word123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testAllValidCharactersInUsername() {
        String validUsername = "abcDEF123._- ";
        UserValidator.Result result = validator.apply(validUsername, "password123", "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testAllValidCharactersInPassword() {
        String validPassword = "ABCdef123!@#$%^&*() ";
        UserValidator.Result result = validator.apply("john_doe", validPassword, "John Doe");
        assertTrue(result instanceof UserValidator.Result.Success);
    }

    @Test
    void testAllValidCharactersInFullName() {
        String validFullName = "John Michael O'Doe-Smith Jr.";
        UserValidator.Result result = validator.apply("john_doe", "password123", validFullName);
        assertTrue(result instanceof UserValidator.Result.Success);
    }
}