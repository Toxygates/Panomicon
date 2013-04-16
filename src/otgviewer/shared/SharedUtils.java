package otgviewer.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import bioweb.shared.array.DataColumn;

public class SharedUtils {

	public static List<BarcodeColumn> asColumns(List<Group> groups) {		
		List<BarcodeColumn> r = new ArrayList<BarcodeColumn>(groups.size());	
		for (Group g: groups) {
			r.add(g);
		}		
		return r;
	}
	
	
}
