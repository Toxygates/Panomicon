package otgviewer.shared;

import java.io.Serializable;

public class ServerError extends Exception implements Serializable {
	
	public ServerError() {}
	
	public ServerError(String reason) { super(reason); }
}
