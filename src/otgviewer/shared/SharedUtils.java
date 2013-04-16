package otgviewer.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SharedUtils {

	public static List<DataColumn> asColumns(List<Group> groups) {		
		List<DataColumn> r = new ArrayList<DataColumn>(groups.size());	
		for (Group g: groups) {
			r.add(g);
		}		
		return r;
	}
	
	
}
