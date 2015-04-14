package otgviewer.shared;

public class NoSuchProbeException extends ServerError {

	public NoSuchProbeException() {}
	
	public NoSuchProbeException(String probe) {
		super("No such probe or gene: " + probe);
	}
}
