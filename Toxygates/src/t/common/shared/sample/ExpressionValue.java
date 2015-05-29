/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.common.shared.sample;

import java.io.Serializable;

/**
 * A single entry in an ExprMatrix or ExpressionRow.
 */
public class ExpressionValue implements Serializable {
	private double _value = 0;
	private char _call = 'A';
	private String _tooltip = null;

	public ExpressionValue() { }

	/**
	 * Construct a present ExpressionValue.
	 * @param value
	 */
	public ExpressionValue(double value) {
		this(value, 'P');		
	}
	
	public ExpressionValue(double value, char call) {
		this(value, call, null);
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
	
	public String getTooltip() { 
		return (_tooltip == null) ? "" : _tooltip; 
	}
	
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
