package t.admin.client;

import java.util.Arrays;

import bioweb.client.components.ResizingDockLayoutPanel;
import bioweb.client.components.SelectionTable;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
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
				
		dp.addWest(makeInstanceList(), 200);
		dp.add(makeTabPanel());
	}
	
	private Widget makeInstanceList() {
		ListBox lb = new ListBox();
		lb.setVisibleItemCount(10);
		for (String inst: instances) {
			lb.addItem(inst);
		}
		lb.setWidth("100%");
		return lb;
	}
	
	private Widget makeTabPanel() {
		TabLayoutPanel tlp = new TabLayoutPanel(2, Unit.EM);
		tlp.add(makeBatchEditor(), "Batches");
		tlp.add(makeAccessEditor(), "Access");
		return tlp;
	}
	
	private Widget makeBatchEditor() {
		SelectionTable<String> st = new SelectionTable<String>("Sel", true) {

			@Override
			protected void initTable(CellTable<String> table) {
				setItems(Arrays.asList(batches), false);				
			}
		};
		return st;
	}
	
	private Widget makeAccessEditor() {
		return new SimplePanel();
	}

}
