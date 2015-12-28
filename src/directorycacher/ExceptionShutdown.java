package directorycacher;

public class ExceptionShutdown {

	public static void shutdown(String message, Throwable exception) {
		System.err.println("[ERROR] "+message);
		exception.printStackTrace();
		System.exit(0);
	}

	public static void err(String message) {
		System.err.println("[WARING] "+message);
	}

	public static void log(String message) {
		System.out.println("[INFO] "+message);
	}

}
