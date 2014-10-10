package otgviewer.shared;

import java.io.Serializable;

public class NoSuchProbeException extends ServerError {

	public NoSuchProbeException() {}
	
	public NoSuchProbeException(String probe) {
		super("No such probe or gene: " + probe);
	}
}
