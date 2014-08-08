package otgviewer.shared;

import java.util.HashMap;
import java.util.Map;

import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.sample.Sample;

public class OTGSample extends Sample {

	public OTGSample() { super(); }
	
	public OTGSample(String _code, SampleClass _sampleClass) {
		super(_code, _sampleClass);			 
	}
	
	public String getTitle(DataSchema schema) {
		return getShortTitle(schema) + " (" + id() + ")";
	}
	
	public String getShortTitle(DataSchema schema) {
		return get(schema.mediumParameter()) + "/" + get(schema.minorParameter());
	}
	
	/**
	 * Obtain a short string that identifies the compound/dose/time combination
	 * for this sample.
	 * @return
	 */

	public String getCode() {
		return id();
	}
	
	public String get(String parameter) {
		return sampleClass.get(parameter);
	}
	
	public String toString() {
		return sampleClass.toString();
	}

	//TODO remove or upgrade to use DataSchema 
	public static OTGSample unpack_v1_2(String s) {
//		Window.alert(s + " as barcode");
		String[] s1 = s.split("\\$\\$\\$");
		
		Map<String, String> sc = new HashMap<String, String>();
		String id = s1[1];
		sc.put("individual_id", s1[2]);
		sc.put("dose_level", s1[3]);
		sc.put("exposure_time", s1[4]);
		sc.put("compound_name", s1[5]);
		
		if (s1.length == 6) {		
			//Version 1
			return new OTGSample(id, new SampleClass(sc));
		} else if (s1.length == 10) {
			//Version 2
			sc.put("test_type", s1[6]);
			sc.put("organ_id", s1[7]);
			sc.put("sin_rep_type", s1[8]);
			sc.put("organism", s1[9]);
			return new OTGSample(id, new SampleClass(sc));						
		} else {			
			return null;
		}
	}
	
	public static OTGSample unpack(String s) {
		String[] spl = s.split("\\$\\$\\$");
		String v = spl[0];
		if (!v.equals("Barcode_v3")) {
			return unpack_v1_2(s);
		} 
		String id = spl[1];
		SampleClass sc = SampleClass.unpack(spl[2]);
		return new OTGSample(id, sc);
		
	}
	
	//V 1/2 packing function
//	
//	public String pack() {
//		final String sep = "$$$";
//		StringBuilder sb = new StringBuilder();
//		sb.append("Barcode").append(sep);
//		sb.append(id()).append(sep);
//		sb.append(individual).append(sep);
//		sb.append(dose).append(sep);
//		sb.append(time).append(sep);
//		sb.append(compound).append(sep);
//		
//		if (sampleClass.get("test_type") != null) {
//			sb.append(sampleClass.get("test_type")).append(sep);
//			sb.append(sampleClass.get("organ_id")).append(sep);
//			sb.append(sampleClass.get("sin_rep_type")).append(sep);
//			sb.append(sampleClass.get("organism")).append(sep);
//		}
//		return sb.toString();
//	}
	
	public String pack() {
		final String sep = "$$$";
		StringBuilder sb = new StringBuilder();
		sb.append("Barcode_v3").append(sep);
		sb.append(id()).append(sep);
		sb.append(sampleClass.pack()).append(sep);
		return sb.toString();
	}
}
