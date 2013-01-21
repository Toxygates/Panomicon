package otgviewer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.visualization.client.VisualizationUtils;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class OTGViewer implements EntryPoint, ScreenManager {
	private static Resources resources = GWT.create(Resources.class);
	
	private RootLayoutPanel rootPanel;
	private DockLayoutPanel mainDockPanel;
//	private FlowPanel mainVertPanel;
	private MenuBar menuBar;
	private HorizontalPanel navPanel;
	private List<Screen> workflow = new ArrayList<Screen>();
	private Map<String, Screen> screens = new HashMap<String, Screen>();
	private Set<String> configuredScreens = new HashSet<String>();
	private Screen currentScreen;
	
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

		
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			public void onValueChange(ValueChangeEvent<String> vce) {				
				setScreenForToken(vce.getValue());
			}
		});
		
		rootPanel = RootLayoutPanel.get();
//		rootPanel.setSize("100%", "100%");
		
		Window.addResizeHandler(new ResizeHandler() {
			public void onResize(ResizeEvent event) {
				Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
					public void execute() {
						resizeInterface();						
					}
				});
			}
		});

		mainDockPanel = new DockLayoutPanel(Unit.EM);		
		rootPanel.add(mainDockPanel);
//		mainDockPanel.setSize("100%", "100%");
		
		mainDockPanel.addNorth(menuBar, 2.7);
		
		navPanel = Utils.mkHorizontalPanel();
		mainDockPanel.addNorth(navPanel, 2.7);
		
		initScreens(); //Need access to the nav. panel
				
		setScreenForToken(History.getToken());						
		deconfigureAll(pickScreen(History.getToken()));
	}
	
	
	private MenuBar setupMenu() {
		MenuBar menuBar = new MenuBar(false);
		menuBar.setWidth("100%");		
		
		MenuBar hm = new MenuBar(true);		
		MenuItem mi = new MenuItem("Help", hm);
		menuBar.addItem(mi);
		
		hm.addItem(new MenuItem("Instructions...", new Command() {
			public void execute() {
				currentScreen.showHelp();
			}
		}));
		
		hm.addItem(new MenuItem("About Toxygates...", new Command() {
			public void execute() {
				Utils.showHelp(getAboutHTML(), getAboutImage());
			}
		}));
		
		return menuBar;
	}

	public MenuBar getMenuBar() { 
		return menuBar;
	}

	void addWorkflowLinks(Screen current) {
		navPanel.clear();
		for (int i = 0; i < workflow.size(); ++i) {
			final Screen s = workflow.get(i);
		
			String link = (i < workflow.size() - 1) ? (s.getTitle()  + " >> ") : s.getTitle();
			Label l = new Label(link);
			if (s.enabled() && s != current) {								
				l.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent e) {
						History.newItem(s.key());						
					}
				});
				l.setStyleName("clickHeading");		
			} else {
				if (s == current) {
					l.setStyleName("headingCurrent");
				} else {
					l.setStyleName("headingBlack");
				}
			}
			navPanel.add(l);
		}		
	}
	
	private void setScreenForToken(String token) {
		Screen s = pickScreen(token);
		showScreen(s);
	}
	
	private void showScreen(Screen s) {
		if (currentScreen != null) {
			mainDockPanel.remove(currentScreen);
			currentScreen.hide();
		}
		currentScreen = s;
		addWorkflowLinks(currentScreen);
		mainDockPanel.add(currentScreen);
		currentScreen.show();
		resizeInterface(); 
	}

	/**
	 * Pick the appropriate screen to display.
	 * @return
	 */
	private Screen pickScreen(String token) {
		
		if (!screens.containsKey(token)) {
		    return screens.get(DatasetScreen.key); //default			
		} else {
			return screens.get(token);
		}		
	}
	
	/**
	 * Proceed if the screen is ready.
	 */
	public void attemptProceed(String to) {
		Screen s = pickScreen(to);
		if (s.enabled()) {
			History.newItem(to);
		} else {			
			//proceed to default screen (must always be enabled!)
			History.newItem(DatasetScreen.key);
		}
	}
	
	private void addScreenSeq(Screen s) {
		screens.put(s.key(), s);
		workflow.add(s);		
		s.initGUI();
		s.tryConfigure(); //give it a chance to register itself as configured
	}
	
	private void initScreens() {
		addScreenSeq(new DatasetScreen(this));		
		addScreenSeq(new ColumnScreen(this));		
		addScreenSeq(new ProbeScreen(this));		
		addScreenSeq(new DataScreen(this));		
		addScreenSeq(new PathologyScreen(this));
		addScreenSeq(new SampleDetailScreen(this));
	}
	
	@Override
	public void setConfigured(Screen s, boolean configured) {
		if (configured) {
			configuredScreens.add(s.key());
		} else {
			configuredScreens.remove(s.key());
		}
	}

	@Override
	public void deconfigureAll(Screen from) {
		for (Screen s: workflow) {
			if (s != from) {
				s.setConfigured(false);				
			}
		}		
		for (Screen s: workflow) {
			if (s != from) {
				s.loadState();
				s.tryConfigure();
			}
		}
		addWorkflowLinks(currentScreen);
	}
	
	@Override
	public boolean isConfigured(String key) {
		return configuredScreens.contains(key);
	}
	
	private TextResource getAboutHTML() {
		return resources.aboutHTML();
	}
	
	private ImageResource getAboutImage() {
		return resources.about();
	}
	
	private void resizeInterface() {
		if (currentScreen != null) {
			currentScreen.resizeInterface();
		}
	}
	
	
}
