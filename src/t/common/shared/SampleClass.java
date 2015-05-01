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

/**
 * A sample class identifies a group of samples.
 * 
 * Standard keys for OTG: time, dose, organism, organ_id, test_type, sin_rep_type
 * Optional keys: compound_name, exposure_time, dose_level
 * DataSchema should be used to identify keys, rather than hardcoding strings.
 */
public class SampleClass implements Serializable, Packable {

	/*
	 * TODO: carry reference to schema?
	 */
	
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
	
	public SampleClass copyWith(String key, String value) {
		SampleClass sc = copy();
		sc.put(key, value);
		return sc;
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
	public boolean compatible(HasClass hc) {
		return compatible(hc.sampleClass());
	}
	
	/**
	 * Is this SampleClass compatible with the other one?
	 * True iff shared keys have the same values.
	 * @param other
	 * @return
	 */
	public boolean compatible(SampleClass other) {
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
	
	/**
	 * Produce a new SampleClass that contains only those keys
	 * that were shared between the two classes and had the 
	 * same values.
	 * @param other
	 * @return
	 */
	public SampleClass intersection(SampleClass other) {
		Set<String> k1 = other.getMap().keySet();
		Set<String> k2 = getMap().keySet();
		Set<String> keys = new HashSet<String>();		
		keys.addAll(k1);
		keys.addAll(k2);
		Map<String, String> r = new HashMap<String, String>();
		for (String k: keys) {
			if (k2.contains(k) && k1.contains(k) &&			
					get(k).equals(other.get(k))) {
				r.put(k, get(k));
			}
		}
		return new SampleClass(r);		
	}
	
	public static SampleClass intersection(List<? extends SampleClass> from) {
		if (from.size() == 0) {
			//This is technically an error, but let's be forgiving.
			return new SampleClass();			
		} else if (from.size() == 1) {
			return from.get(0);
		} 
		SampleClass r = from.get(0);
		for (int i = 1; i < from.size(); i++) {			
			r = r.intersection(from.get(i));
		}
		return r;		
	}
	
	public static List<SampleClass> classes(List<? extends HasClass> from) {
		List<SampleClass> r = new ArrayList<SampleClass>();
		for (HasClass hc: from) {
			r.add(hc.sampleClass());
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
	
	public <T extends HasClass> List<T> filter(List<T> from) {
		List<T> r = new ArrayList<T>();
		for (T t: from) {
			if (compatible(t)) {
				r.add(t);
			}
		}
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
	
	public String label(DataSchema schema) {
		StringBuilder sb = new StringBuilder();
		for (String p: schema.macroParameters()) {
			sb.append(get(p)).append("/");
		}
		return sb.toString(); 
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
