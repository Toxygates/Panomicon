package bioweb.shared.array;

import java.io.Serializable;

import javax.annotation.Nullable;

public class ExpressionValue implements Serializable {
	private double _value = 0;
	private char _call = 'A';
	private Double _pValue = null;
	
	public ExpressionValue() { }

	/**
	 * Construct an ExpressionValue with an associated p-value.
	 * @param value
	 */
	public ExpressionValue(double value, char call, 
			@Nullable Double pValue) {
		this._value = value;
		this._call = call;
		this._pValue = pValue;
	}
	
	/**
	 * Construct a present ExpressionValue with no associated p-value.
	 * @param value
	 */
	public ExpressionValue(double value) {
		this(value, 'P');		
	}
	
	/**
	 * Construct an ExpressionValue with no associated p-value.
	 * @param value
	 * @param call
	 */
	public ExpressionValue(double value, char call) {
		this(value, call, null);
	}
	
	public double getValue() { return _value; }
	
	public boolean getPresent() { return _call != 'A'; }
	
	public char getCall() { return _call; }
	
	/**
	 * @return the p-value associated with this value,
	 * or null if none exists.
	 */
	public @Nullable Double getPValue() { return _pValue; }
	
	@Override
	public String toString() {
		return "(" + _value + ", " + _call + ")";
	}
}
