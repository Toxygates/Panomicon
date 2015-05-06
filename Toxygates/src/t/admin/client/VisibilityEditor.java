package t.admin.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.common.client.DataRecordSelector;

/**
 * GUI for editing the visibility of a batch.
 * @author johan
 */
public class VisibilityEditor extends DataRecordSelector<Instance> {

	static final String message =
			"Please select the instances in which this batch should be\n visible. Changes are saved immediately.";
	
	public VisibilityEditor(Batch batch, Collection<Instance> instances) {
		super(instances, message);
		
		Set<Instance> initSel = new HashSet<Instance>();
		for (Instance i: instances) {
			if (batch.getEnabledInstances().contains(i.getTitle())) {
				initSel.add(i);
			}
		}
		st.setSelection(initSel);
		
//		st.setSize("100%", "100%");
	
	}
	


}
