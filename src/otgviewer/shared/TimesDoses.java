package otgviewer.shared;

import java.util.logging.Level;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import t.common.shared.DataSchema;

public class TimesDoses extends DataSchema {
	Logger logger = Utils.getLogger("dataschema");
	public final static String[] allTimes = new String[] { "2 hr", "3 hr", "6 hr", "8 hr", "9 hr", "24 hr", "4 day", "8 day", "15 day", "29 day" };
	public final static String[] allDoses = new String[] { "Control", "Low", "Middle", "High" };
	
	public String[] sortedValues(String parameter) throws Exception {
		if (parameter.equals("exposure_time")) {
			return allTimes;
		} else if (parameter.equals("dose_level")) {
			return allDoses;
		} else {
			throw new Exception("Invalid parameter (not sortable): " + parameter);
		}
	}
	
	public void sortTimes(String[] times) {
		try {
			sort("exposure_time", times);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unable to sort times", e);
		}
	}

	public void sortDoses(String[] doses) {
		try {
			sort("dose_level", doses);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unable to sort doses", e);
		}
	}

}
