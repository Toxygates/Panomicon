package t.admin.client;

import java.util.Arrays;

import bioweb.client.components.ResizingDockLayoutPanel;
import bioweb.client.components.ResizingListBox;
import bioweb.client.components.SelectionTable;
import bioweb.client.components.StringSelectionTable;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point for the data and instance management tool.
 */
public class AdminConsole implements EntryPoint {

	private RootLayoutPanel rootPanel;
	
	final String[] instances = new String[] {"Toxygates", "Adjuvant", "Private"};
	final String[] batches = new String[] {"Open TG-Gates", "Rat adjuvant", "Mouse adjuvant", "Secret"};
	
	
	@Override
	public void onModuleLoad() {
		rootPanel = RootLayoutPanel.get();
		
		ResizingDockLayoutPanel dp = new ResizingDockLayoutPanel();
		rootPanel.add(dp);
				
		dp.addWest(makeInstanceList(), 250);
		dp.add(makeTabPanel());
	}
	
	private Widget makeInstanceList() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		ListBox lb = new ResizingListBox(135);
		lb.setVisibleItemCount(10);
		for (String inst: instances) {
			lb.addItem(inst);
		}
		lb.setWidth("230px");
		lb.setStylePrimaryName("withMargin");
		
		HorizontalPanel buttons = new HorizontalPanel();
		buttons.setSpacing(4);
		buttons.add(new Button("Add new..."));
		buttons.add(new Button("Delete"));
		dp.addSouth(buttons, 35);
		dp.add(lb);
		return dp; 
	}
	
	private Widget makeTabPanel() {
		TabLayoutPanel tlp = new TabLayoutPanel(2, Unit.EM);
		tlp.add(makeBatchEditor(), "Batches");
		tlp.add(makeAccessEditor(), "Access");		
		return tlp;
	}
	
	private Widget makeBatchEditor() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		
		SelectionTable<String> st = new StringSelectionTable("", "Batch");
		st.setItems(Arrays.asList(batches));
		
		HorizontalPanel buttons = new HorizontalPanel();
		buttons.add(new Button("Upload new..."));
		buttons.add(new Button("Delete"));
		buttons.add(new Button("Publish changes"));
		buttons.setSpacing(4);
		dp.addSouth(buttons, 35);
		dp.add(st);
		return dp;
	}
	
	private Widget makeAccessEditor() {
		return new SimplePanel();
	}

}
