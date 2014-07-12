package otgviewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A sample class identifies a group of samples.
 * 
 * Standard keys for OTG: time, dose, organism, organ, testType, repType
 * @author johan
 */
public class SampleClass implements Serializable {

	public SampleClass() { }
	
	private Map<String, String> data = new HashMap<String, String>();
	
	public SampleClass(Map<String, String> data) {
		this.data = data;
	}
	
	public String get(String key) {
		return data.get(key);
	}
	
	public void put(String key, String value) {
		data.put(key, value);
	}
	
	public static List<String> collect(List<SampleClass> from, String key) {
		List<String> r = new ArrayList<String>();
		for (SampleClass sc: from) {
			r.add(sc.get(key));
		}
		return r;
	}
	
	public static List<SampleClass> filter(List<SampleClass>from, 
			String key, String constraint) {
		List<SampleClass> r = new ArrayList<SampleClass>();
		for (SampleClass sc: from) {
			if (sc.get(key).equals(constraint)) {
				r.add(sc);
			}
		}	
		return r;
	}
}
