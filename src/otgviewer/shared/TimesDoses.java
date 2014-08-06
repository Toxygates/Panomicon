package otgviewer.shared;

import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.Unit;

public class TimesDoses extends DataSchema {	
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

	public void sortDoses(String[] doses) throws Exception {		
		sort("dose_level", doses);		
	}

	@Override
	public String majorParameter() { 
		return "compound_name";
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
	public Unit selectionControlUnitFor(Unit u) {
		Unit u2 = new Unit(u, new OTGSample[] {});
		u2.put("dose_level", "Control");
		return u2;
	}
}
