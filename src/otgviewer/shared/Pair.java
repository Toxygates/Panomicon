package otgviewer.shared;

import java.io.Serializable;

public class Pair<T, U> implements Serializable {

	public Pair() { }
		
	public Pair(T t, U u) {
		this._t = t;
		this._u = u;
	}
	
	private T _t;
	public T first() { return _t; }
	
	private U _u;
	public U second() { return _u; }
	
	@Override
	public String toString() {
		return "(" + _t.toString() + ", " + _u.toString() + ")";
	}
}
