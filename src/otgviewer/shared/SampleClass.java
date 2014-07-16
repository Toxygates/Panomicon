package otgviewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	public static Set<String> collect(List<SampleClass> from, String key) {
		Set<String> r = new HashSet<String>();
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
	
	// TODO this is temporary - DataFilter is to be removed
	public DataFilter asDataFilter() {
		Organ o = Organ.valueOf(get("organ"));
		Organism s = Organism.valueOf(get("organism"));
		RepeatType r = RepeatType.valueOf(get("repType"));
		CellType c = get("testType").equals("in vivo") ? CellType.Vivo : CellType.Vitro;
		return new DataFilter(c, o, r, s);
	}
}
