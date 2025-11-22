package library.controls;

import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import static org.junit.jupiter.api.Assertions.*;

class ManageProfileControlTest {

    private Repository repository;
    private ManageProfileControl manageProfileControl;
    private UserValidator validator;

    @BeforeEach
    void setUp() {
        repository = new Repository(DBMaker.memoryDirectDB());
        manageProfileControl = new ManageProfileControl(repository);
        validator = UserValidator.DEFAULT;
    }

    // Login Tests

    @Test
    void login_Success() throws TransactionException {
        // Create a user first
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password123", "Test User");
        repository.userOps.create(user, userData);

        ManageProfileControl.LoginResult result = manageProfileControl.login(User.Role.STUDENT_STAFF, "testuser", "password123");

        assertInstanceOf(ManageProfileControl.LoginResult.Success.class, result);
        ManageProfileControl.LoginResult.Success success = (ManageProfileControl.LoginResult.Success) result;
        assertEquals(user, success.user());
        assertEquals(userData, success.data());
    }

    @Test
    void login_InvalidUsername() {
        ManageProfileControl.LoginResult result = manageProfileControl.login(User.Role.STUDENT_STAFF, "nonexistent", "password");

        assertInstanceOf(ManageProfileControl.LoginResult.InvalidUsername.class, result);
        assertEquals("Invalid username", ((ManageProfileControl.LoginResult.InvalidUsername) result).getMessage());
    }

    @Test
    void login_WrongRole() throws TransactionException {
        // Create a student user
        User user = new User("studentuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password123", "Student User");
        repository.userOps.create(user, userData);

        // Try to login as librarian
        ManageProfileControl.LoginResult result = manageProfileControl.login(User.Role.LIBRARIAN, "studentuser", "password123");

        assertInstanceOf(ManageProfileControl.LoginResult.WrongRole.class, result);
        ManageProfileControl.LoginResult.WrongRole wrongRole = (ManageProfileControl.LoginResult.WrongRole) result;
        assertEquals(User.Role.STUDENT_STAFF, wrongRole.actualRole());
        assertEquals("Logging in as wrong role: student/staff", wrongRole.getMessage());
    }

    @Test
    void login_InvalidPassword() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "correctpassword", "Test User");
        repository.userOps.create(user, userData);

        ManageProfileControl.LoginResult result = manageProfileControl.login(User.Role.STUDENT_STAFF, "testuser", "wrongpassword");

        assertInstanceOf(ManageProfileControl.LoginResult.InvalidPassword.class, result);
        assertEquals("Invalid password", ((ManageProfileControl.LoginResult.InvalidPassword) result).getMessage());
    }

    @Test
    void login_DeactivatedAccount() throws TransactionException {
        User user = new User("inactiveuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, false, "password123", "Inactive User");
        repository.userOps.create(user, userData);

        ManageProfileControl.LoginResult result = manageProfileControl.login(User.Role.STUDENT_STAFF, "inactiveuser", "password123");

        assertInstanceOf(ManageProfileControl.LoginResult.DeactivatedAccount.class, result);
        assertEquals("Deactivated account", ((ManageProfileControl.LoginResult.DeactivatedAccount) result).getMessage());
    }

    @Test
    void login_AllRoles() throws TransactionException {
        // Test login with different roles
        User.Role[] roles = {User.Role.STUDENT_STAFF, User.Role.LIBRARIAN, User.Role.AUTHOR};

        for (User.Role role : roles) {
            User user = new User("user_" + role.name);
            User.Data userData = new User.Data(role, true, "password123", "Test User");
            repository.userOps.create(user, userData);

            ManageProfileControl.LoginResult result = manageProfileControl.login(role, "user_" + role.name, "password123");

            assertInstanceOf(ManageProfileControl.LoginResult.Success.class, result);
        }
    }

    // Register Tests

    @Test
    void register_Success() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "newuser", "password123", "New User"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.Success.class, result);
        ManageProfileControl.RegisterResult.Success success = (ManageProfileControl.RegisterResult.Success) result;
        assertEquals("newuser", success.user().username());
        assertEquals("New User", success.data().fullName());
        assertEquals(User.Role.STUDENT_STAFF, success.data().role());
        assertTrue(success.data().active());
        assertEquals("password123", success.data().password());
        assertEquals("Registration successful", success.getMessage());

        // Verify user was actually created in repository
        User.Data storedData = repository.userOps.read(new User("newuser")).orElse(null);
        assertNotNull(storedData);
        assertEquals("New User", storedData.fullName());
    }

    @Test
    void register_UsernameExists() throws TransactionException {
        // Create user first
        User existingUser = new User("existinguser");
        User.Data existingData = new User.Data(User.Role.STUDENT_STAFF, true, "password123", "Existing User");
        repository.userOps.create(existingUser, existingData);

        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "existinguser", "password123", "New User"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.UsernameExists.class, result);
        assertEquals("Username exists", ((ManageProfileControl.RegisterResult.UsernameExists) result).getMessage());
    }

    @Test
    void register_InvalidDetails_EmptyUsername() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "", "password123", "Full Name"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.InvalidDetails.class, result);
        ManageProfileControl.RegisterResult.InvalidDetails invalidDetails = (ManageProfileControl.RegisterResult.InvalidDetails) result;
        assertEquals("Invalid details: Empty username", invalidDetails.getMessage());
    }

    @Test
    void register_InvalidDetails_ShortPassword() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "newuser", "short", "Full Name"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.InvalidDetails.class, result);
        ManageProfileControl.RegisterResult.InvalidDetails invalidDetails = (ManageProfileControl.RegisterResult.InvalidDetails) result;
        assertEquals("Invalid details: Password too short", invalidDetails.getMessage());
    }

    @Test
    void register_InvalidDetails_BadUsernameCharacters() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "user\nname", "password123", "Full Name"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.InvalidDetails.class, result);
        ManageProfileControl.RegisterResult.InvalidDetails invalidDetails = (ManageProfileControl.RegisterResult.InvalidDetails) result;
        assertEquals("Invalid details: Bad characters in username", invalidDetails.getMessage());
    }

    @Test
    void register_InvalidDetails_BadPasswordCharacters() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "newuser", "pass\u0000word", "Full Name"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.InvalidDetails.class, result);
        ManageProfileControl.RegisterResult.InvalidDetails invalidDetails = (ManageProfileControl.RegisterResult.InvalidDetails) result;
        assertEquals("Invalid details: Bad characters in password", invalidDetails.getMessage());
    }

    @Test
    void register_InvalidDetails_EmptyFullName() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "newuser", "password123", ""
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.InvalidDetails.class, result);
        ManageProfileControl.RegisterResult.InvalidDetails invalidDetails = (ManageProfileControl.RegisterResult.InvalidDetails) result;
        assertEquals("Invalid details: Empty full name", invalidDetails.getMessage());
    }

    @Test
    void register_InvalidDetails_BadFullNameCharacters() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "newuser", "password123", "Name\u0000WithControl"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.InvalidDetails.class, result);
        ManageProfileControl.RegisterResult.InvalidDetails invalidDetails = (ManageProfileControl.RegisterResult.InvalidDetails) result;
        assertEquals("Invalid details: Bad characters in full name", invalidDetails.getMessage());
    }

    @Test
    void register_AllRoles() throws TransactionException {
        User.Role[] roles = {User.Role.STUDENT_STAFF, User.Role.LIBRARIAN, User.Role.AUTHOR};

        for (User.Role role : roles) {
            String username = "user_" + role.name.toLowerCase();
            ManageProfileControl.RegisterResult result = manageProfileControl.register(
                    validator, role, username, "password123", "Test User"
            );

            assertInstanceOf(ManageProfileControl.RegisterResult.Success.class, result);
            ManageProfileControl.RegisterResult.Success success = (ManageProfileControl.RegisterResult.Success) result;
            assertEquals(role, success.data().role());
        }
    }

    @Test
    void register_UserActiveByDefault() throws TransactionException {
        ManageProfileControl.RegisterResult result = manageProfileControl.register(
                validator, User.Role.STUDENT_STAFF, "activeuser", "password123", "Active User"
        );

        assertInstanceOf(ManageProfileControl.RegisterResult.Success.class, result);
        ManageProfileControl.RegisterResult.Success success = (ManageProfileControl.RegisterResult.Success) result;
        assertTrue(success.data().active());
    }

    // Result Hierarchy Tests

    @Test
    void testLoginResultHierarchy() {
        // Test that all login result types implement the proper interfaces
        ManageProfileControl.LoginResult success = new ManageProfileControl.LoginResult.Success(new User("test"), new User.Data(User.Role.STUDENT_STAFF, true, "pass", "name"));
        ManageProfileControl.LoginResult invalidUsername = new ManageProfileControl.LoginResult.InvalidUsername();
        ManageProfileControl.LoginResult wrongRole = new ManageProfileControl.LoginResult.WrongRole(User.Role.STUDENT_STAFF);
        ManageProfileControl.LoginResult invalidPassword = new ManageProfileControl.LoginResult.InvalidPassword();
        ManageProfileControl.LoginResult deactivated = new ManageProfileControl.LoginResult.DeactivatedAccount();

        assertInstanceOf(ManageProfileControl.LoginResult.class, success);
        assertInstanceOf(ManageProfileControl.LoginResult.class, invalidUsername);
        assertInstanceOf(ManageProfileControl.LoginResult.class, wrongRole);
        assertInstanceOf(ManageProfileControl.LoginResult.class, invalidPassword);
        assertInstanceOf(ManageProfileControl.LoginResult.class, deactivated);

        assertInstanceOf(HasMessage.class, invalidUsername);
        assertInstanceOf(HasMessage.class, wrongRole);
        assertInstanceOf(HasMessage.class, invalidPassword);
        assertInstanceOf(HasMessage.class, deactivated);
    }

    @Test
    void testRegisterResultHierarchy() {
        // Test that all register result types implement the proper interfaces
        ManageProfileControl.RegisterResult success = new ManageProfileControl.RegisterResult.Success(new User("test"), new User.Data(User.Role.STUDENT_STAFF, true, "pass", "name"));
        ManageProfileControl.RegisterResult invalidDetails = new ManageProfileControl.RegisterResult.InvalidDetails(new UserValidator.Result.BadUsername("test"));
        ManageProfileControl.RegisterResult usernameExists = new ManageProfileControl.RegisterResult.UsernameExists();

        assertInstanceOf(ManageProfileControl.RegisterResult.class, success);
        assertInstanceOf(ManageProfileControl.RegisterResult.class, invalidDetails);
        assertInstanceOf(ManageProfileControl.RegisterResult.class, usernameExists);

        assertInstanceOf(HasMessage.class, success);
        assertInstanceOf(HasMessage.class, invalidDetails);
        assertInstanceOf(HasMessage.class, usernameExists);
    }

    @Test
    void testRegisterResultInvalidDetailsMessage() {
        // Test the InvalidDetails message formatting
        UserValidator.Result.BadUsername badUsername = new UserValidator.Result.BadUsername("Bad username");
        ManageProfileControl.RegisterResult.InvalidDetails invalidDetails = new ManageProfileControl.RegisterResult.InvalidDetails(badUsername);

        assertEquals("Invalid details: Bad username", invalidDetails.getMessage());
    }
}