package otgviewer.client;

import java.util.Arrays;
import java.util.List;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

public class DataScreen extends Screen {

	public static final String key = "data";
	private ExpressionTable et;
	
	private DataFilter lastFilter;
	private String[] lastProbes;
	private List<DataColumn> lastColumns;
	
	public DataScreen(ScreenManager man) {
		super("View data", key, true, man,
				resources.dataDisplayHTML(), resources.dataDisplayHelp());		
	}
	
	public Widget content() {
		et = new ExpressionTable((Window.getClientHeight() - 70) + "px");
		addListener(et);
		
		MenuItem[] mis = et.menuItems();
		for (MenuItem mi: mis) {
			addMenu(mi);
		}
		
//		HorizontalPanel hp = new HorizontalPanel();
//		hp.add(new Button("View pathologies", new ClickHandler() {
//			public void onClick(ClickEvent event) {				
//				configuredProceed(PathologyScreen.key);				
//			}
//		}));
//		
//		hp.add(new Button("View biochemical data", new ClickHandler() {
//			public void onClick(ClickEvent event) {
//				configuredProceed(SampleDetailScreen.key);				
//			}
//		}));
//		
//		dockPanel.add(hp, DockPanel.SOUTH);
		
		return et;		
	}
	
	@Override
	public boolean enabled() {
		return manager.isConfigured(ProbeScreen.key) && manager.isConfigured(ColumnScreen.key); 
	}

	public void show() {
		super.show();
		//state has finished loading
		
		//Attempt to avoid reloading the data
		if (lastFilter == null || !lastFilter.equals(chosenDataFilter) ||
				//lastProbes == null || !Arrays.deepEquals(chosenProbes, lastProbes) ||
				lastColumns == null || !chosenColumns.equals(lastColumns)) {			
			et.getExpressions(chosenProbes); //false		
		} else if (!Arrays.equals(chosenProbes, lastProbes)) {				
			et.refilterData();			
		}
		
		lastProbes = chosenProbes;
		lastFilter = chosenDataFilter;
		lastColumns = chosenColumns;
	}
}
