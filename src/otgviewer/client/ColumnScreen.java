package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.ScreenManager;
import otgviewer.shared.DataColumn;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ColumnScreen extends Screen {
	public static String key = "columns";
	
	private GroupInspector gi;
	
	public ColumnScreen(Screen parent, ScreenManager man) {
		super(parent, "Column definitions", key, true, man);
	}
	
	public Widget content() {
		
		VerticalPanel vp = new VerticalPanel();
		HorizontalPanel hp = new HorizontalPanel();
		vp.add(hp);
		CompoundSelector cs = new CompoundSelector("Compounds");
		this.addListener(cs);
		hp.add(cs);
		
		TabPanel tp = new TabPanel();
		hp.add(tp);		
		
		gi = new GroupInspector(cs);
		this.addListener(gi);
		cs.addListener(gi);
		tp.add(gi, "Groups");
		
		final CompoundRanker cr = new CompoundRanker(cs);
		tp.add(cr, "Compound ranking");
		tp.selectTab(0);
		
		Button b = new Button("Next: Select probes");		
		b.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				if (gi.chosenColumns().size() == 0) {
					Window.alert("Please define and activate at least one group.");
				} else {
					History.newItem(ProbeScreen.key);
				}
			}
		});
		dockPanel.add(b, DockPanel.SOUTH);
		
		return vp;
	}
	
	
	@Override
	public void loadState() {
		super.loadState();
		
		try {
			List<DataColumn> ics = loadColumns("inactiveColumns", 
					new ArrayList<DataColumn>(gi.existingGroupsTable.inverseSelection()));
			if (ics != null) {
				gi.inactiveColumnsChanged(ics);
			}

		} catch (Exception e) {
			Window.alert("Unable to load inactive columns.");
		}
	}
}
