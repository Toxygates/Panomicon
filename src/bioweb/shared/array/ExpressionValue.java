package bioweb.shared.array;

import java.io.Serializable;

public class ExpressionValue implements Serializable {

	private double _value = 0;
	private char _call = 'A';
	
	public ExpressionValue() { }
		
	public ExpressionValue(double value, char call) {
		_value = value;
		_call = call;
	}
	
	public ExpressionValue(double value) {
		this(value, 'P');		
	}
	
	//TODO remove duplicated methods
	public double getValue() { return _value; }
	public double value() { return _value; }
	
	public boolean getPresent() {
		return _call != 'A';
	}
	
	public char getCall() { return _call; }
	
}
