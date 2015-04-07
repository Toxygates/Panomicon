package otgviewer.shared;

import java.util.ArrayList;

import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.Unit;
import t.viewer.shared.AType;

public class OTGSchema extends DataSchema {	
	public static String[] allTimes = new String[] { "2 hr", "3 hr", "6 hr", "8 hr", "9 hr", "24 hr", "4 day", "8 day", "15 day", "29 day" };
	public static String[] allDoses = new String[] { "Control", "Low", "Middle", "High" };
	
	public String[] sortedValues(String parameter) throws Exception {
		if (parameter.equals("exposure_time")) {
			return allTimes;
		} else if (parameter.equals("dose_level")) {
			return allDoses;
		} else {
			throw new Exception("Invalid parameter (not sortable): " + parameter);
		}
	}
	
	@Override
	public String[] filterValuesForDisplay(ValueType vt, String parameter, String[] from) {
		if (parameter.equals("dose_level") && (vt == null || vt == ValueType.Folds)) {
			ArrayList<String> r = new ArrayList<String>();
			for (String s : from) {
				if (!isControlValue(s)) {
					r.add(s);
				}
			}
			return r.toArray(new String[0]);
		} else {
			return super.filterValuesForDisplay(vt, parameter, from);
		}
	}

	public void sortDoses(String[] doses) throws Exception {		
		sort("dose_level", doses);		
	}

	@Override
	public String majorParameter() { 
		return "compound_name";
	}
	
	@Override
	public boolean isMajorParamSharedControl(String value) {
		return value.toLowerCase().startsWith("shared_control");
	}
	
	@Override
	public String mediumParameter() {
		return "dose_level";
	}

	@Override
	public String minorParameter() {
		return "exposure_time"; 
	}

	@Override
	public String timeParameter() {
		return "exposure_time";
	}

	@Override
	public String timeGroupParameter() {
		return "dose_level";
	}

	@Override
	public String title(String parameter) {
		if (parameter.equals("exposure_time")) {
			return "Time";
		} else if (parameter.equals("compound_name")) {
			return "Compound";
		} else if (parameter.equals("dose_level")) {
			return "Dose";
		} else {
			return "???";
		}
	}
	
	private String[] macroParams = new String[] { "organism", 
			"test_type", "organ_id", "sin_rep_type" };
	
	public String[] macroParameters() { return macroParams; }

	@Override
	public boolean isSelectionControl(SampleClass sc) {
		return sc.get("dose_level").equals("Control");
	}
	
	@Override
	public boolean isControlValue(String value) {
		return (value != null) && ("Control".equals(value));
	}
	
	@Override
	public Unit selectionControlUnitFor(Unit u) {
		Unit u2 = new Unit(u, new OTGSample[] {});
		u2.put("dose_level", "Control");
		return u2;
	}
	
	private static AType[] associations = new AType[] {
		AType.Chembl, AType.Drugbank, AType.Enzymes,
		AType.GOBP, AType.GOCC, AType.GOMF, AType.Homologene,
		AType.KEGG, AType.OrthProts, AType.Uniprot
	};
	
	public AType[] associations() {
		return associations;
	}
	
	/**
	 * TODO: this is brittle, given that platform names may
	 * change externally - think about a better way of doing this
	 * long term
	 */
	public String platformOrganism(String platform) {
		if (platform.startsWith("HG")) {
			return "Human";
		} else if (platform.startsWith("Rat")) {
			return "Rat";
		} else if (platform.startsWith("Mouse")) {
			return "Mouse";
		} else {
			return super.platformSpecies(platform);			
		}
	}
	
	//TODO as above.
	public String organismPlatform(String organism) {
		if (organism.equals("Human")) {
			return "HG-U133_Plus_2";
		} else if (organism.equals("Rat")) {
			return "Rat230_2";			
		} else if (organism.equals("Mouse")) {
			return "Mouse430_2";
		} else {
			return null;
		}
	}
	
	/**
	 * TODO this is brittle
	 */
	public int numDataPointsInSeries(SampleClass sc) {
		if (sc.get("test_type") != null &&
				sc.get("test_type").equals("in vitro")) {
			return 3;
		} 
		return 4;
	}
}
