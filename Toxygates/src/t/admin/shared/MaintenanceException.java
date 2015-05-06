package t.admin.shared;

public class MaintenanceException extends Exception {

	public MaintenanceException() {
	}

	public MaintenanceException(String message) {
		super(message);
	}

	public MaintenanceException(Throwable cause) {
		super(cause);
	}

	public MaintenanceException(String message, Throwable cause) {
		super(message, cause);
	}

}
