package t.common.shared;

/**
 * A Pair whose equality and hashCode only depend on the first member.
 * @author johan
 *
 * @param <T>
 * @param <U>
 */
public class FirstKeyedPair<T, U> extends Pair<T, U> {

	//GWT serialization constructor
	public FirstKeyedPair() { }
	
	public FirstKeyedPair(T t, U u) {
		super(t, u);
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other instanceof FirstKeyedPair) {
			return _t.equals(((FirstKeyedPair<T, U>) other).first());
		} else {
			return false;
		}
	}
	
	@Override 
	public int hashCode() {
		return _t.hashCode();
	}

}
