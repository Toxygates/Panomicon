package t.viewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

/**
 * A sample class identifies a group of samples.
 * 
 * Standard keys for OTG: time, dose, organism, organ_id, test_type, sin_rep_type
 * Optional keys: compound_name, exposure_time, dose_level
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
	
	public SampleClass copy() {
		return new SampleClass(getMap());
	}
 	
	public Map<String, String> getMap() { return new HashMap<String,String> (data); }
	
	public static Set<String> collect(List<SampleClass> from, String key) {
		Set<String> r = new HashSet<String>();
		for (SampleClass sc: from) {
			String x = sc.get(key);
			if (x != null) {
				r.add(x);
			}		
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
	@Deprecated
	public DataFilter asDataFilter() {
		Organ o = Organ.valueOf(get("organ_id"));
		Organism s = Organism.valueOf(get("organism"));
		RepeatType r = RepeatType.valueOf(get("sin_rep_type"));
		CellType c = get("test_type").equals("in vivo") ? CellType.Vivo : CellType.Vitro;
		return new DataFilter(c, o, r, s);
	}
	
	@Deprecated
	public static SampleClass fromDataFilter(DataFilter df) {
		if (df == null) {
			return null;
		}
		
		SampleClass r = new SampleClass();
		r.put("organ_id", df.organ.toString());
		r.put("organism", df.organism.toString());
		if (df.cellType == CellType.Vivo) {
			r.put("test_type", "in vivo");
		} else {
			r.put("test_type", "in vitro");
		}
		r.put("sin_rep_type", df.repeatType.toString());
		return r;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof SampleClass) {
			return data.equals(((SampleClass) other).getMap());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return data.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SC(");
		for (String k : data.keySet()) {
			sb.append(k + ":" + data.get(k) + ",");
		}
		sb.append(")");
		return sb.toString();
	}
	
	public String label() {
		return get("organism") + "/" + get("test_type") + "/" + 
				get("organ_id") + "/" + get("sin_rep_type"); 
	}
}
