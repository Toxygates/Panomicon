package otgviewer.shared;

import java.io.Serializable;

public class ExpressionValue implements Serializable {

	private double value;
	private char call;
	
	public ExpressionValue() {
		value = 0;
	    call = 'A';
	}
	
	public ExpressionValue(double _value, char _call) {
		value = _value;
		call = _call;
	}
	
	public double value() {
		return value;
	}
	
	public boolean present() {
		return call != 'A';
	}
}
