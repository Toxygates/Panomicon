package t.common.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	
	public int hashCode() {
		return _t.hashCode() * 41 + _u.hashCode();
	}
	
	public boolean equals(Object other) {
		if (other instanceof Pair<?, ?>) {
			Pair<T, U> o = (Pair<T, U>) other;
			return (o.first().equals(_t) && o.second().equals(_u));
		}
		return false;
	}
	
	public static <A, B> List<A> mapFirst(Collection<Pair<A, B>> pairs) {
		List<A> r = new ArrayList<A>();
		for (Pair<A, B> p: pairs) {
			r.add(p.first());
		}
		return r;
	}
	
	public static <A, B> List<B> mapSecond(Collection<Pair<A, B>> pairs) {
		List<B> r = new ArrayList<B>();
		for (Pair<A, B> p: pairs) {
			r.add(p.second());
		}
		return r;
	}
	
	public static <A> List<Pair<A, A>> duplicate(Collection<A> items) {
		List<Pair<A, A>> r = new ArrayList<Pair<A, A>>();
		for (A a: items) {
			r.add(new Pair<A, A>(a, a));
		}
		return r;
	}
	
}
