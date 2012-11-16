package otgviewer.client;

import java.util.HashMap;
import java.util.Map;

import otgviewer.client.components.ScreenManager;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.visualization.client.VisualizationUtils;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class OTGViewer implements EntryPoint, ScreenManager {

	private RootPanel rootPanel;
	private VerticalPanel mainVertPanel;
	private MenuBar menuBar;

	private Map<String, Screen> screens = new HashMap<String, Screen>();
	
	private MenuBar setupMenu() {
		MenuBar menuBar = new MenuBar(false);
		menuBar.setWidth("100%");		
		menuBar.addItem(new MenuItem("Toxygates", new Command() {
			public void execute() {}
		}));		
		return menuBar;
	}

	private void resizeInterface(int newHeight) {
		// this is very fiddly and must be tested on all the browsers.
		// Note that simply setting height = 100% won't work.
		String h = (newHeight - rootPanel.getAbsoluteTop() - 20) + "px";

		if (currentScreen != null) {
			currentScreen.resizeInterface(newHeight);
		}	
	}	
	private Screen currentScreen;
	
	/**
	 * Pick the appropriate screen to display.
	 * @return
	 */
	private Screen pickScreen(String token) {		
		if (!screens.containsKey(token)) {
			return screens.get(DatasetScreen.key); //default			
		}
		return screens.get(token);		
	}
	
	private void initScreens() {
		Screen s = new DatasetScreen(null, this);
		screens.put(s.key(), s);
//		s = new CompoundScreen(s, this);
//		screens.put(s.key(), s);
		s = new ColumnScreen(s, this);
		screens.put(s.key(), s);
		s = new ProbeScreen(s, this);
		screens.put(s.key(), s);
		s = new DataScreen(s, this);
		screens.put(s.key(), s);
		s = new PathologyScreen(s, this);
		screens.put(s.key(), s);
	}

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		Runnable onLoadChart = new Runnable() {
			public void run() {

//				 DataTable data = DataTable.create();
//		         data.addColumn(ColumnType.STRING, "Gene Name");
//		         data.addColumn(ColumnType.NUMBER, "chip_XXX_XXX_600");
//		         data.addColumn(ColumnType.NUMBER, "chip2");
//		         data.addColumn(ColumnType.NUMBER, "chip3");
//		         data.addColumn(ColumnType.NUMBER, "chip4");
//		         data.addColumn(ColumnType.NUMBER, "chip5");
//		         data.addColumn(ColumnType.NUMBER, "chip6");
//		         data.addRows(2);         
//		         data.setValue(0, 0, "ATF3");
//		         data.setValue(0, 1, 0);
//		         data.setValue(0, 2, 0.5);
//		         data.setValue(0, 3, 1);
//		         data.setValue(0, 4, 1.5);
//		         data.setValue(0, 5, 2);
//		         data.setValue(0, 6, 2.5);
//		         data.setValue(1, 0, "INS");
//		         data.setValue(1, 1, 3);
//		         data.setValue(1, 2, 3.5);
//		         data.setValue(1, 3, 4);
//		         data.setValue(1, 4, 4.5);
//		         data.setValue(1, 5, 5);
//		         data.setValue(1, 6, 5.5);
//		         
//		         bhm.draw(data);
			}
		};

		VisualizationUtils
				.loadVisualizationApi("1.1", onLoadChart, "corechart");
	
		menuBar = setupMenu();
		initScreens();
		
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			public void onValueChange(ValueChangeEvent<String> vce) {				
				setScreenForToken(vce.getValue());
			}
		});
		
		rootPanel = RootPanel.get("rootPanelContainer");
		rootPanel.setSize("100%", "100%");
		rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);

		Window.addResizeHandler(new ResizeHandler() {
			public void onResize(ResizeEvent event) {
				resizeInterface(event.getHeight());
			}
		});

		mainVertPanel = new VerticalPanel();
		mainVertPanel.setBorderWidth(0);
		rootPanel.add(mainVertPanel);
		mainVertPanel.setSize("100%", "100%");
		mainVertPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		
		mainVertPanel.add(menuBar);
		
		if ("".equals(History.getToken())) {
			History.newItem(DatasetScreen.key);
		} else {
			setScreenForToken(History.getToken());		
		}		
	}
	
	private void setScreenForToken(String token) {
		Screen s = pickScreen(token);
		showScreen(s);
	}
	
	private void showScreen(Screen s) {
		if (currentScreen != null) {
			mainVertPanel.remove(currentScreen);
			currentScreen.hide();
		}
		currentScreen = s;
		currentScreen.show();					
		mainVertPanel.add(currentScreen);
		resizeInterface(Window.getClientHeight()); 
	}
	
	public void showTemporary(Screen s) 
	{
		screens.put(s.key(), s);		
		History.newItem(s.key());
	}
	
	public MenuBar getMenuBar() { 
		return menuBar;
	}
}
