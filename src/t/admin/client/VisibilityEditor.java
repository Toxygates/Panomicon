package t.admin.client;

import static t.admin.client.Utils.makeButtons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.common.client.components.SelectionTable;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * GUI for editing the visibility of a batch.
 * @author johan
 */
public class VisibilityEditor extends Composite {

	private SelectionTable<Instance> st;
	public VisibilityEditor(Batch batch, Collection<Instance> instances) {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		
		Label l = new Label("Please select the instances in which this batch should be\n visible. Changes are saved immediately.");
		vp.add(l);
		
		st = new SelectionTable<Instance>("", true) {
			@Override
			protected void initTable(CellTable<Instance> table) {
				TextColumn<Instance> tc = new TextColumn<Instance>() {
					@Override
					public String getValue(Instance object) {
						return object.getTitle();
					}
				};
				table.addColumn(tc, "ID");				
			}
		};
		st.setItems(new ArrayList<Instance>(instances));
		Set<Instance> initSel = new HashSet<Instance>();
		for (Instance i: instances) {
			if (batch.getEnabledInstances().contains(i.getTitle())) {
				initSel.add(i);
			}
		}
		st.setSelection(initSel);
		
//		st.setSize("100%", "100%");
		
		List<Command> commands = new ArrayList<Command>();
		commands.add(new Command("OK") {
			@Override 
			void run() { onOK(); }
		});
		commands.add(new Command("Cancel") {
			@Override 
			void run() { onCancel(); }
		});
		
		vp.add(st);
		vp.add(makeButtons(commands));	
	}
	
	public Set<Instance> getSelection() {
		return st.getSelection();
	}
	
	/**
	 * TODO extract shared library w. otgviewer
	 */
	public void onOK() {
		
	}
	
	public void onCancel() {
		
	}

}
