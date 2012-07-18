package otgviewer.shared;

import java.io.Serializable;

public class ExpressionValue implements Serializable {

	private double value = 0;
	private char call = 'A';
	
	public ExpressionValue() { }
		
	public ExpressionValue(double _value, char _call) {
		value = _value;
		call = _call;
	}
	
	public double getValue() {
		return value;
	}
	
	public boolean getPresent() {
		return call != 'A';
	}
}
