package otgviewer.shared;

import java.util.Arrays;
import java.util.Comparator;

public class SampleTimes {
	public final static String[] allTimes = new String[] { "2 hr", "3 hr", "6 hr", "8 hr", "9 hr", "24 hr", "4 day", "8 day", "15 day", "29 day" };
	
	public static void sortTimes(String[] times) {
		Arrays.sort(times, new Comparator<String>() {
			public int compare(String e1, String e2) {
				Integer i1 = SharedUtils.indexOf(allTimes, e1);
				Integer i2 = SharedUtils.indexOf(allTimes, e2);
				return i1.compareTo(i2);
			}
		});
	}
}
