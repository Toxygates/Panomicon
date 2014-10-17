package otgviewer.shared;

public class DBUnavailableException extends ServerError {

	public DBUnavailableException() { 
		super("The database is currently unavailable, possibly for maintenance reasons. Please try again later.");
	}

}
