package library.controllers.common;

import library.models.User;

public interface DependsOnRole {
	User.Role getRole();

	void setRole(User.Role role);
}
