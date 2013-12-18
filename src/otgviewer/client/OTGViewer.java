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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.MetaElement;
import com.google.gwt.dom.client.NodeList;
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

/**
 * The main entry point for Toxygates.
 * The old name for the application was OTGViewer. This class could possibly
 * be renamed in the future.
 * The main task of this class is to manage the history mechanism and ensure that
 * the correct screen is being displayed at any given time, as well as provide a 
 * facility for inter-screen communication.
 * @author johan
 *
 */
public class OTGViewer implements EntryPoint, ScreenManager {
	private static Resources resources = GWT.create(Resources.class);
	
	private RootLayoutPanel rootPanel;
	private DockLayoutPanel mainDockPanel;
	private MenuBar menuBar;
	private HorizontalPanel navPanel;
	
	/**
	 * All screens in the order that the links are displayed at the top.
	 */
	private List<Screen> workflow = new ArrayList<Screen>();
	
	/**
	 * All available screens. The key in this map is the "key" field of each
	 * Screen instance, which also corresponds to the history token used with
	 * GWT's history tracking mechanism.
	 */
	private Map<String, Screen> screens = new HashMap<String, Screen>();
	
	/**
	 * All currently configured screens. See the Screen class for an explanation of the
	 * "configured" concept.
	 */
	private Set<String> configuredScreens = new HashSet<String>();
	
	/**
	 * The screen currently being displayed.
	 */
	private Screen currentScreen;
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		menuBar = setupMenu();

		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			public void onValueChange(ValueChangeEvent<String> vce) {				
				setScreenForToken(vce.getValue());
			}
		});
		
		rootPanel = RootLayoutPanel.get();

		Window.addResizeHandler(new ResizeHandler() {
			public void onResize(ResizeEvent event) {
				Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
					public void execute() {
						resizeInterface();						
					}
				});
			}
		});

		mainDockPanel = new DockLayoutPanel(Unit.PX);		
		rootPanel.add(mainDockPanel);

		mainDockPanel.addNorth(menuBar, 35);
		
		navPanel = Utils.mkHorizontalPanel();
		mainDockPanel.addNorth(navPanel, 35);
		
		initScreens(); //Need access to the nav. panel
				
		setScreenForToken(History.getToken());						
		deconfigureAll(pickScreen(History.getToken()));
	}
	
	/**
	 * A <meta> tag in the hosting HTML page identifies the kind of UI we want 
	 * to show in this application.
	 * E.g.: <meta name="uitype" content="toxygates"> .
	 * @return The requested UI type
	 */
	public String getUIType() {
		NodeList<Element> metas = Document.get().getElementsByTagName("meta");
	    for (int i=0; i<metas.getLength(); i++) {
	        MetaElement meta = (MetaElement) metas.getItem(i);
	        if ("uitype".equals(meta.getName())) {
	        	return meta.getContent();	            
	        }
	    }
	    return "toxygates"; // Default UIType
	}	
	
	private MenuBar setupMenu() {
		MenuBar menuBar = new MenuBar(false);
		menuBar.setWidth("100%");		
		
		MenuBar hm = new MenuBar(true);		
		MenuItem mi = new MenuItem("Help", hm);
		menuBar.addItem(mi);
		
		hm.addItem(new MenuItem("Help for this screen...", new Command() {
			public void execute() {
				currentScreen.showHelp();
			}
		}));
		
		hm.addItem(new MenuItem("Download user guide...", new Command() {
			public void execute() {
				Window.open("toxygatesManual.pdf", "_blank", "");			
			}
		}));
		
		hm.addItem(new MenuItem("Display guide messages", new Command() {
			public void execute() {
				currentScreen.showGuide();
			}		
		}));
		
		hm.addItem(new MenuItem("About Toxygates...", new Command() {
			public void execute() {
				Utils.showHelp(getAboutHTML(), getAboutImage());
			}
		}));
		
		hm.addItem(new MenuItem("Version history...", new Command() {
			public void execute() {
				Utils.showHelp(getVersionHTML(), null);
			}
		}));
		
		return menuBar;
	}

	/**
	 * Individual screens call this method to get the menu and install their own menu entries.
	 */
	public MenuBar getMenuBar() { 
		return menuBar;
	}

	/**
	 * This method sets up the navigation links that allow the user to jump between screens.
	 * The enabled() method of each screen is used to test whether that screen is currently 
	 * available for use or not.
	 * @param current
	 */
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
	
	/**
	 * Display the screen that corresponds to a given history token.
	 * @param token
	 */
	private void setScreenForToken(String token) {
		Screen s = pickScreen(token);
		showScreen(s);
	}
	
	/**
	 * Switch screens.
	 * @param s
	 */
	private void showScreen(Screen s) {
		if (currentScreen != null) {
			mainDockPanel.remove(currentScreen);
			currentScreen.hide();
		}
		currentScreen = s;
		addWorkflowLinks(currentScreen);
		mainDockPanel.add(currentScreen);		
		currentScreen.show();
		mainDockPanel.forceLayout(); //IE8
		resizeInterface(); 
	}

	/**
	 * Pick the appropriate screen for a given history token.
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
	
	/**
	 * Helper method for initialising screens
	 * @param s
	 */
	private void addScreenSeq(Screen s) {
		screens.put(s.key(), s);
		workflow.add(s);		
		s.initGUI();
		s.tryConfigure(); //give it a chance to register itself as configured
	}
	
	/**
	 * Set up the workflow sequence once.
	 */
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
	
	private TextResource getVersionHTML() {
		return resources.versionHTML();
	}
	
	private void resizeInterface() {
		if (currentScreen != null) {
			currentScreen.resizeInterface();
		}
		rootPanel.onResize();
	}
	
}
