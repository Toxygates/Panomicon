package t.admin.shared;

import java.io.Serializable;

public class OperationResults implements Serializable {

	public OperationResults() { }

	public OperationResults(String taskName, boolean successful, String[] infoStrings) {
		_taskName = taskName;
		_infoStrings = infoStrings;
		_successful = successful;
	}
	
	private String _taskName;
	private boolean _successful;
	private String[] _infoStrings;
	
	public String taskName() { return _taskName; }
	public boolean successful() { return _successful; }
	public String[] infoStrings() { return _infoStrings; }	
}
