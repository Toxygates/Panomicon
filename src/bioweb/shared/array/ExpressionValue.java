package bioweb.shared.array;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * A single entry in an ExprMatrix or ExpressionRow.
 * Future: consider using Doubles instead if possible
 * (if call can be handled separately)
 */
public class ExpressionValue implements Serializable {
	private double _value = 0;
	private char _call = 'A';
	
	public ExpressionValue() { }

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
		_value = value;
		_call = call;
	}
	
	public double getValue() { return _value; }
	
	public boolean getPresent() { return _call != 'A'; }
	
	public char getCall() { return _call; }
	
	@Override
	public String toString() {
		return "(" + _value + ", " + _call + ")";
	}
	
	/**
	 * equals and hashCode need to be overridden for
	 * unit testing to work properly.
	 */	
	@Override 
	public boolean equals(Object o) {
		if (o instanceof ExpressionValue) {
			ExpressionValue other = (ExpressionValue) o;
			return other.getValue() == _value &&
					other.getCall() == _call;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return ((Double)_value).hashCode() + _call;			
	}
}
