package library.controllers.common;

import library.models.User;
import org.jetbrains.annotations.NotNull;

public interface DependsOnRole {
	@NotNull User.Role getRole();

	void setRole(@NotNull User.Role role);
}
