package library.utils;

/**
 * The interface Has message.
 */
public interface HasMessage {
    /**
     * Gets message.
     *
     * @return the message
     */
    String getMessage();

    /**
     * Gets localized message.
     *
     * @return the localized message
     */
    default String getLocalizedMessage() {
		return getMessage();
	}
}
