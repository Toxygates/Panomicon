package otgviewer.shared;

import java.util.HashMap;
import java.util.Map;

import t.common.shared.SampleClass;
import t.common.shared.sample.Sample;

public class OTGSample extends Sample implements OTGColumn {
		
	private String individual = "";
	private String dose = "";
	private String time = "";
	private String compound = "";
	private BUnit unit;	
	
	public OTGSample() { super(); }
	
	public OTGSample(String _code, SampleClass _sampleClass) {
		super(_code, _sampleClass);		
		individual = sampleClass.get("individual_id");
		dose = sampleClass.get("dose_level");
		time = sampleClass.get("exposure_time");
		compound = sampleClass.get("compound_name");
		unit = new BUnit(this, sampleClass);
	}
	
	public String getTitle() {
		return getShortTitle() + " (" + id() + ")";
	}
	
	public String getShortTitle() {
		return dose + "/" + time + "/"+ individual;
	}
	
	/**
	 * Obtain a short string that identifies the compound/dose/time combination
	 * for this sample.
	 * @return
	 */
	public String getParamString() {
		return unit.toString();
	}
	
	public BUnit getUnit() {
		return unit;
	}
	
	public String getCode() {
		return id();
	}
	
	public String getIndividual() {
		return individual;
	}
	
	public String getDose() {
		return dose;
	}
	
	public String getTime() {
		return time;
	}
	
	public String toString() {
		return getShortTitle();
	}
	
	public String getCompound() { 
		return compound;
	}
	
	public OTGSample[] getSamples() { 
		return new OTGSample[] { this };
	}
	
	public String[] getCompounds() {
		return new String[] { compound };
	}
	
	public static OTGSample unpack(String s) {
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
	
	public String pack() {
		final String sep = "$$$";
		StringBuilder sb = new StringBuilder();
		sb.append("Barcode").append(sep);
		sb.append(id()).append(sep);
		sb.append(individual).append(sep);
		sb.append(dose).append(sep);
		sb.append(time).append(sep);
		sb.append(compound).append(sep);
		if (unit.getCellType() != null) {
			sb.append(unit.getCellType()).append(sep);
			sb.append(unit.getOrgan()).append(sep);
			sb.append(unit.getRepeatType()).append(sep);
			sb.append(unit.getOrganism()).append(sep);
		}
		return sb.toString();
	}

}
