package t.common.shared;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A SampleMultiFilter is a filter for SampleClass and HasClass.
 * For each key, several permitted values may be specified.
 * @author johan
 */
public class SampleMultiFilter {

	private Map<String, Set<String>> constraints = new HashMap<String, Set<String>>();
	
	public SampleMultiFilter() { }
	
	public SampleMultiFilter(Map<String, Set<String>> constr) {
		constraints = constr;
	}
	
	public boolean contains(String key) { return constraints.containsKey(key); }
	
	public void addPermitted(String key, String value) {
		if (constraints.containsKey(key)) {
			constraints.get(key).add(value);
		} else {
			Set<String> v = new HashSet<String>();
			v.add(value);
			constraints.put(key, v);
		}
	}
	
	public void addPermitted(String key, String[] value) {
		for (String v: value) {
			addPermitted(key, v);
		}
	}
	
	/**
	 * Returns true if and only if the SampleClass contains one of the permitted values
	 * for all keys specified in this multi filter.
	 * @param sc
	 * @return
	 */
	public boolean accepts(SampleClass sc) {
		for (String k: constraints.keySet()) {
			Set<String> vs = constraints.get(k);
			if (!sc.contains(k) || !vs.contains(sc.get(k))) {
				return false;
			}
		}
		return true;
	}
	
	public boolean accepts(HasClass hc) {
		return accepts(hc.sampleClass());
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String k: constraints.keySet()) {
			sb.append(k + ":(");
			for (String v: constraints.get(k)) {
				sb.append(v + ",");
			}
			sb.append(")");
		}
		return "SMF(" + sb.toString() + ")";
	}
}
