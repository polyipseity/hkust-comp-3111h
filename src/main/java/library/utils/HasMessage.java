package library.utils;

public interface HasMessage {
	String getMessage();

	default String getLocalizedMessage() {
		return getMessage();
	}
}
