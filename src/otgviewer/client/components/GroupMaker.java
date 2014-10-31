package otgviewer.client.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.GroupInspector;
import otgviewer.shared.Group;
import t.common.shared.DataSchema;
import t.common.shared.Unit;

public class GroupMaker {
	
	/**
	 * Generate suitable groups, one per "major" value, from the given units.
	 * This algorithm simply finds the dose/time combination with the largest number of majors
	 * available, and then creates groups with 1 unit each.
	 * @param schema
	 * @param units
	 * @return
	 */
	public static List<Group> autoGroups(GroupInspector gi, 
			DataSchema schema, List<Unit> units) {
		List<Group> r = new ArrayList<Group>();
		Map<String, List<Unit>> byMedMin = new HashMap<String, List<Unit>>();
		Map<String, Unit> controls = new HashMap<String, Unit>();
		
		if (units.size() == 0) {
			return r;
		}
		
		final String medParam = schema.mediumParameter(), 
				minParam = schema.minorParameter(),
				majParam = schema.majorParameter();
		
		int maxLen = 0;
		String maxKey = "";
		
		for (Unit u: units) {
			String medMin = u.get(medParam) + u.get(minParam);
			if (schema.isControlValue(u.get(medParam))) {
				String majMin = u.get(majParam) + u.get(minParam);
				controls.put(majMin, u);
				continue;
			}
			if (!byMedMin.containsKey(medMin)) {
				byMedMin.put(medMin, new ArrayList<Unit>());				
			}
			byMedMin.get(medMin).add(u);
			
			int len = byMedMin.get(medMin).size();
			if (len > maxLen) {
				maxLen = len;
				maxKey = medMin;
			}
		}
		
		for (Unit u: byMedMin.get(maxKey)) {
			List<Unit> us = new ArrayList<Unit>();
			us.add(u);
			String majMin = u.get(majParam) + u.get(minParam);
			Unit c = controls.get(majMin);
			if (c != null) {
				us.add(c);
			}
			String n = gi.suggestGroupName(us);
			Group g = new Group(schema, n, us.toArray(new Unit[0]));
			r.add(g);
		}
		
		return r;
	}
}
