package t.common.shared.sample;

import java.io.Serializable;

/**
 * A single entry in an ExprMatrix or ExpressionRow.
 * Future: consider using Doubles instead if possible
 * (if call can be handled separately)
 */
public class ExpressionValue implements Serializable {
	private double _value = 0;
	private char _call = 'A';
	private String _tooltip = "";
	
	public ExpressionValue() { }

	/**
	 * Construct a present ExpressionValue.
	 * @param value
	 */
	public ExpressionValue(double value) {
		this(value, 'P');		
	}
	
	public ExpressionValue(double value, char call) {
		this(value, call, "");
	}
	
	/**
	 * Construct an ExpressionValue with a given call and tooltip.
	 * @param value
	 * @param call
	 * @param tooltip
	 */
	public ExpressionValue(double value, char call, String tooltip) {
		_value = value;
		_call = call;
		_tooltip = tooltip;
	}
	
	public double getValue() { return _value; }
	
	public boolean getPresent() { return _call != 'A'; }
	
	public char getCall() { return _call; }
	
	public String getTooltip() { return _tooltip; }
	
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
