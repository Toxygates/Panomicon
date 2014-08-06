package t.common.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public class SampleClass implements Serializable, Packable {

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
	
	public SampleClass copyOnly(Collection<String> keys) {
		Map<String, String> data = new HashMap<String, String>();
		for (String k: keys) {
			data.put(k, get(k));			
		}
		return new SampleClass(data);
	}
	
	public void mergeDeferred(SampleClass from) {
		for (String k: from.getMap().keySet()) {
			if (!data.containsKey(k)) {
				data.put(k, from.get(k));
			}
		}
	}
	
	public SampleClass asMacroClass(DataSchema schema) {
		List<String> keys = new ArrayList<String>();
		for (String s: schema.macroParameters()) {
			keys.add(s);
		}		
		return copyOnly(keys);
	}
	
	public SampleClass asUnit(DataSchema schema) {
		List<String> keys = new ArrayList<String>();		
		for (String s: schema.macroParameters()) {
			keys.add(s);
		}
		keys.add(schema.majorParameter());
		keys.add(schema.mediumParameter());
		keys.add(schema.minorParameter());
		return copyOnly(keys);		
	}
 	
	public Map<String, String> getMap() { return new HashMap<String,String> (data); }
	
	/**
	 * Does the HasClass match the constraints specified in this SampleClass?
	 * @param hc
	 * @return
	 */
	public boolean permits(HasClass hc) {
		return subsumes(hc.sampleClass());
	}
	
	/**
	 * Is this SampleClass more specific than the other one?
	 * @param other
	 * @return
	 */
	public boolean subsumes(SampleClass other) {
		for (String k: data.keySet()) {
			if (other.get(k) != null && !other.get(k).equals(get(k))) {
				return false;
			}
		}
		return true;		
	}
	
	public static Set<String> collectInner(List<? extends HasClass> from, String key) {
		Set<String> r = new HashSet<String>();
		for (HasClass hc: from) {
			String x = hc.sampleClass().get(key);
			if (x != null) {
				r.add(x);
			}		
		}
		return r;
	}
	
	public static Set<String> collect(List<? extends SampleClass> from, String key) {
		Set<String> r = new HashSet<String>();
		for (SampleClass sc: from) {
			String x = sc.get(key);
			if (x != null) {
				r.add(x);
			}		
		}
		return r;
	}
	
	public static <T extends SampleClass> List<T> filter(T[] from, 
			String key, String constraint) {
		List<T> ff = Arrays.asList(from);
		return filter(ff, key, constraint);
	}
	
	public static <T extends SampleClass> List<T> filter(List<T>from, 
			String key, String constraint) {
		List<T> r = new ArrayList<T>();
		for (T sc: from) {
			if (sc.get(key).equals(constraint)) {
				r.add(sc);
			}
		}	
		return r;
	}
	
	// TODO this is temporary - DataFilter is to be removed
	@Deprecated
	public DataFilter asDataFilter() {
		try {
			Organ o = Organ.valueOf(get("organ_id"));
			Organism s = Organism.valueOf(get("organism"));
			RepeatType r = RepeatType.valueOf(get("sin_rep_type"));
			CellType c = get("test_type").equals("in vivo") ? CellType.Vivo
					: CellType.Vitro;
			return new DataFilter(c, o, r, s);
		} catch (Exception e) {
			return null;
		}
	}
	
	@Deprecated
	public static SampleClass fromDataFilter(DataFilter df) {
		if (df == null) {
			return null;
		}
		
		SampleClass r = new SampleClass();
		r.put("organ_id", df.organ);
		r.put("organism", df.organism);
		//TODO resolve the handling of this
		if (df.cellType.equals("Vivo")) {
			r.put("test_type", "in vivo");
		} else {
			r.put("test_type", "in vitro");
		}			
		r.put("sin_rep_type", df.repeatType);
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
	
	@Deprecated
	public String label() {
		return get("organism") + "/" + get("test_type") + "/" + 
				get("organ_id") + "/" + get("sin_rep_type"); 
	}
	
	public String tripleString(DataSchema sc) {
		String maj = get(sc.majorParameter());
		String med = get(sc.mediumParameter());
		String min = get(sc.minorParameter());
		return maj + "/" + med + "/" + min;
	}
	
	public String pack() {
		StringBuilder sb = new StringBuilder();
		for (String k : data.keySet()) {
			sb.append(k + ",,,");
			sb.append(data.get(k) + ",,,");
		}
		return sb.toString();
	}
	
	public static SampleClass unpack(String data) {
		String[] spl = data.split(",,,");
		Map<String, String> d = new HashMap<String, String>();
		for (int i = 0; i < spl.length; i+= 2) {
			d.put(spl[i], spl[i+1]);
		}
		return new SampleClass(d);		
	}
}
