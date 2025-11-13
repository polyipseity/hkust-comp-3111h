package library.controllers;

import library.models.User;
import org.jetbrains.annotations.NotNull;

public interface DependsOnRole {
	@NotNull User.Role getRole();

	void setRole(@NotNull User.Role role);
}
