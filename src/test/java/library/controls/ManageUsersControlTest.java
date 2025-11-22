package library.controls;

import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import static org.junit.jupiter.api.Assertions.*;

class ManageUsersControlTest {
    private Repository repository;
    private ManageUsersControl manageUsersControl;

    @BeforeEach
    void setUp() {
        repository = new Repository(DBMaker.memoryDirectDB());
        manageUsersControl = new ManageUsersControl(repository);
    }

    @Test
    void activateUser_Success() throws TransactionException {
        // Arrange - Create an inactive user
        User inactiveUser = new User("inactive_user");
        User.Data inactiveUserData = new User.Data(User.Role.STUDENT_STAFF, false, "password", "Inactive User");
        repository.userOps.create(inactiveUser, inactiveUserData);

        // Act
        var result = manageUsersControl.activateUser(inactiveUser);

        // Assert
        assertInstanceOf(ManageUsersControl.ActivateResult.Success.class, result);
        var successResult = (ManageUsersControl.ActivateResult.Success) result;
        assertTrue(successResult.data().active(), "User should be active after activation");

        // Verify the user is actually updated in the repository
        var updatedData = repository.userOps.readOrThrow(inactiveUser);
        assertTrue(updatedData.active(), "User should be active in repository");
    }

    @Test
    void activateUser_AlreadyActive_ThrowsException() throws TransactionException {
        // Arrange - Create an active user
        User activeUser = new User("active_user");
        User.Data activeUserData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Active User");
        repository.userOps.create(activeUser, activeUserData);

        // Act & Assert
        TransactionException exception = assertThrows(TransactionException.class, () -> {
            manageUsersControl.activateUser(activeUser);
        });

        // Verify the cause is IllegalArgumentException
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("User is active"));
    }

    @Test
    void deactivateUser_Success() throws TransactionException {
        // Arrange - Create an active user
        User activeUser = new User("active_user");
        User.Data activeUserData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Active User");
        repository.userOps.create(activeUser, activeUserData);

        // Act
        var result = manageUsersControl.deactivateUser(activeUser);

        // Assert
        assertInstanceOf(ManageUsersControl.DeactivateResult.Success.class, result);
        var successResult = (ManageUsersControl.DeactivateResult.Success) result;
        assertFalse(successResult.data().active(), "User should be inactive after deactivation");

        // Verify the user is actually updated in the repository
        var updatedData = repository.userOps.readOrThrow(activeUser);
        assertFalse(updatedData.active(), "User should be inactive in repository");
    }

    @Test
    void deactivateUser_AlreadyInactive_ThrowsException() throws TransactionException {
        // Arrange - Create an inactive user
        User inactiveUser = new User("inactive_user");
        User.Data inactiveUserData = new User.Data(User.Role.STUDENT_STAFF, false, "password", "Inactive User");
        repository.userOps.create(inactiveUser, inactiveUserData);

        // Act & Assert
        TransactionException exception = assertThrows(TransactionException.class, () -> {
            manageUsersControl.deactivateUser(inactiveUser);
        });

        // Verify the cause is IllegalArgumentException
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("User is not active"));
    }

    @Test
    void activateUser_NonExistentUser_ThrowsException() {
        // Arrange - User that doesn't exist in repository
        User nonExistentUser = new User("non_existent_user");

        // Act & Assert
        assertThrows(TransactionException.class, () -> {
            manageUsersControl.activateUser(nonExistentUser);
        });
    }

    @Test
    void deactivateUser_NonExistentUser_ThrowsException() {
        // Arrange - User that doesn't exist in repository
        User nonExistentUser = new User("non_existent_user");

        // Act & Assert
        assertThrows(TransactionException.class, () -> {
            manageUsersControl.deactivateUser(nonExistentUser);
        });
    }

    @Test
    void activateUser_WithDifferentUserRoles_Success() throws TransactionException {
        // Test with different user roles to ensure role doesn't affect activation
        User.Role[] roles = {User.Role.STUDENT_STAFF, User.Role.LIBRARIAN, User.Role.AUTHOR};

        for (User.Role role : roles) {
            // Arrange
            User user = new User("user_" + role.name());
            User.Data userData = new User.Data(role, false, "password", "User " + role);
            repository.userOps.create(user, userData);

            // Act
            var result = manageUsersControl.activateUser(user);

            // Assert
            assertInstanceOf(ManageUsersControl.ActivateResult.Success.class, result);
            var successResult = (ManageUsersControl.ActivateResult.Success) result;
            assertTrue(successResult.data().active());
            assertEquals(role, successResult.data().role()); // Role should remain unchanged
        }
    }

    @Test
    void deactivateUser_WithDifferentUserRoles_Success() throws TransactionException {
        // Test with different user roles to ensure role doesn't affect deactivation
        User.Role[] roles = {User.Role.STUDENT_STAFF, User.Role.LIBRARIAN, User.Role.AUTHOR};

        for (User.Role role : roles) {
            // Arrange
            User user = new User("user_" + role.name());
            User.Data userData = new User.Data(role, true, "password", "User " + role);
            repository.userOps.create(user, userData);

            // Act
            var result = manageUsersControl.deactivateUser(user);

            // Assert
            assertInstanceOf(ManageUsersControl.DeactivateResult.Success.class, result);
            var successResult = (ManageUsersControl.DeactivateResult.Success) result;
            assertFalse(successResult.data().active());
            assertEquals(role, successResult.data().role()); // Role should remain unchanged
        }
    }
}
