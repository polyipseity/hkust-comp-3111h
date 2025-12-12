package library;

import javafx.application.Platform;

public enum PlatformTestUtil {
	;

	private static boolean supported = true;

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static synchronized boolean startup() {
		if (!supported) {
			return false;
		}
		try {
			Platform.startup(() -> {
			});   // starts the toolkit
		} catch (IllegalStateException _) {
		} catch (UnsupportedOperationException _) {
			// java.lang.UnsupportedOperationException: Unable to open DISPLAY
			supported = false;
		}
		return supported;
	}
}
