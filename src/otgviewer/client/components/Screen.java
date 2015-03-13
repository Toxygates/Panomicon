package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import otgviewer.client.Resources;
import otgviewer.client.SampleDetailScreen;
import otgviewer.client.Utils;
import otgviewer.shared.Group;
import otgviewer.shared.OTGSample;
import t.common.shared.DataSchema;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Screens are a high level building block for user interfaces.
 * Sequences of screens can form a workflow.
 * Screens require a ScreenManager to assist inter-screen communication.
 * Screens can be hidden/visible and configured/deconfigured. A configured screen has been
 * completely configured by the user, for example by making certain selections. This is a useful
 * concept when late screens depend on data that is selected in earlier screens. 
 * 
 * @author johan
 *
 */
public class Screen extends DataListenerWidget implements RequiresResize, ProvidesResize {
	protected static Resources resources = GWT.create(Resources.class);
	
	protected DockLayoutPanel rootPanel;
	
	/**
	 * Each screen is uniquely identified by its key.
	 */
	private String key; 

	private FlowPanel statusPanel;
	
	/**
	 * Is this screen currently visible?
	 */
	protected boolean visible = false;
	private Label viewLabel = new Label();
	private boolean showGroups = false;	
	
	/**
	 * Is this screen currently configured?
	 */
	protected boolean configured = false;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	private List<MenuItem> analysisMenuItems = new ArrayList<MenuItem>();
	
	
	/**
	 * Widgets to be shown below the main content area, if any.
	 */
	private Widget bottom;
	private HorizontalPanel spOuter, guideBar;
	
	/**
	 * Widgets to be shown above the main content area, if any.
	 */
	private List<Widget> toolbars = new ArrayList<Widget>();
	
	/**
	 * Widgets to be shown to the left of the main content area, if any. Analogous
	 * to "toolbars".
	 */
//	private List<Widget> leftbars = new ArrayList<Widget>();
	
	protected ScreenManager manager;
	
	protected final Logger logger;
	
	/**
	 * Help text for this screen.
	 */
	protected TextResource helpHTML;
	/**
	 * Image to show alongside the help text for this screen.
	 */
	protected ImageResource helpImage;
	
	private boolean showGuide;
	
	/**
	 * An action to be invoked at some later time
	 * (for example when data becomes available)
	 * @author johan
	 *
	 */
	public abstract static class QueuedAction implements Runnable {
		String name;
		public QueuedAction(String name) {
			this.name = name;
		}
		
		public int hashCode() {
			return name.hashCode();
		}
		
		public boolean equals(Object other) {
			if (other instanceof QueuedAction) {
				return name.equals(((QueuedAction) other).name);
			}
			return false;
		}
		
		abstract public void run();
	}
	
	private List<QueuedAction> actionQueue = new LinkedList<QueuedAction>(); 
	
	protected void runActions() {
		for (QueuedAction qa: actionQueue) {
			qa.run();
		}
		actionQueue.clear();
	}

	/**
	 * Add an action to the queue (or replace an action with the same name).
	 * Actions are executed in order, but the order can change if replacement
	 * occurs.
	 * @param qa
	 */
	public void enqueue(QueuedAction qa) {		
		actionQueue.remove(qa); //remove it if it's already there (so we can update it)
		actionQueue.add(qa);
	}
	
	public Screen(String title, String key,  
			boolean showGroups, 
			ScreenManager man,
			TextResource helpHTML, ImageResource helpImage) {		
		this.showGroups = showGroups;
		this.helpHTML = helpHTML;
		this.helpImage = helpImage;
		
		//PX must be used for measurements or there will be problems in e.g. internet explorer.
		//This problem might possibly be solved if everything is changed to use the new-style
		//LayoutPanels.
		rootPanel = new DockLayoutPanel(Unit.PX); 
		
		initWidget(rootPanel);
		manager = man;				
		viewLabel.setWordWrap(false);
		viewLabel.getElement().getStyle().setMargin(2, Unit.PX);
		this.key = key;
		this.logger = Utils.getLogger(key);
		setTitle(title);		
	}
	
	public Screen(String title, String key,  
			boolean showGroups, ScreenManager man) {
		this(title, key, showGroups, man, null, null);
	}

	public ScreenManager manager() {
		return this.manager;
	}
	
	public DataSchema schema() {
		return this.manager.schema();
	}
	
	/**
	 * Is this screen ready for use?
	 * @return
	 */
	public boolean enabled() {
		return true;
	}
	
	/**
	 * Has the user finished configuring this screen?
	 * @return
	 */
	public boolean configured() {
		return configured;
	}
	
	/**
	 * For subclass implementations to indicate that they have been configured
	 */
	public void setConfigured(boolean cfg) {
		configured = cfg;
		manager.setConfigured(this, configured);
	}
	
	/**
	 * Subclass implementations should use this method to check
	 * whether sufficient state to be "configured" has been loaded.
	 * If it has, they should call setConfigured().
	 */
	public void tryConfigure() {
		setConfigured(true);
	}
		
	/**
	 * Indicate that this screen has finished configuring itself and 
	 * attempt to display another screen.
	 * @param key
	 */
	protected void configuredProceed(String key) {
		setConfigured(true);
		manager.attemptProceed(key);		
	}
	
	protected HorizontalPanel mkStandardToolbar(Widget content, String styleName) {
		HorizontalPanel r = Utils.mkWidePanel();		
		r.setHeight("30px");
		r.add(content);		
		r.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);		
		r.setStyleName(styleName);
		return r;
	}
	
	public void initGUI() {
		statusPanel = new FlowPanel(); 
		statusPanel.setStyleName("statusPanel");		
		Utils.floatLeft(statusPanel);

		spOuter = mkStandardToolbar(statusPanel, "statusPanel");		
		statusPanel.setStyleName("statusPanel");		
		guideBar = mkStandardToolbar(mkGuideTools(), "guideBar");		 
		
		addToolbars(); //must be called before rootPanel.add()		
		bottom = bottomContent();
		if (bottom != null) {
			HorizontalPanel hp = Utils.mkWidePanel();			
			hp.add(bottom);
			hp.setHeight("40px");
			rootPanel.addSouth(hp, 40);
		}
		rootPanel.add(content());
	}
	
	private Widget mkGuideTools() {		
		Label l = new Label(getGuideText());
		Utils.floatLeft(l);
		HorizontalPanel hp = Utils.mkWidePanel();
		hp.add(l);
		
		HorizontalPanel hpi = new HorizontalPanel();
		
		PushButton i;
		if (helpAvailable()) {
			i = new PushButton(new Image(resources.help()));
			i.setStyleName("slightlySpaced");
			i.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					showHelp();
				}
			});
			hpi.add(i);
		}
		
		i = new PushButton(new Image(resources.close()));
		i.setStyleName("slightlySpaced");
		final Screen sc = this;
		i.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hideToolbar(guideBar);
				showGuide = false;
				storeState(sc);
			}			
		});		
		hpi.add(i);		
		
		Utils.floatRight(hpi);
		hp.add(hpi);
		
		return hp;
	}
	
	public void showGuide() {
		showToolbar(guideBar);
		showGuide = true;
		storeState(this);
	}
	
	
	/**
	 * This method will be called each time the screen is displayed anew.
	 * If overriding, make sure to call the superclass method.
	 */
	public void show() {
		rootPanel.forceLayout();
		visible = true;		
		loadState(this);
		if (showGuide) {
			showToolbar(guideBar);
		} else {
			hideToolbar(guideBar);
		}
		updateStatusPanel(); //needs access to the groups from loadState
		runActions();
		deferredResize();
	}

	@Override
	public void loadState(StorageParser p, DataSchema schema) {
		super.loadState(p, schema);
		String v = p.getItem("OTG.showGuide");
		if (v == null || v.equals("yes")) {
			showGuide = true;
		} else {
			showGuide = false;
		}
	}
	
	@Override
	public void storeState(StorageParser p) {
		super.storeState(p);
		if (showGuide) {
			p.setItem("OTG.showGuide", "yes");
		} else {
			p.setItem("OTG.showGuide", "no");
		}
	}	

	/**
	 * The standard status panel contains a label that indicates the current data set,
	 * and descriptions of the currently defined groups.
	 */
	
	protected void updateStatusPanel() {
//		statusPanel.setWidth(Window.getClientHeight() + "px");
		statusPanel.clear();
		statusPanel.add(viewLabel);
		Utils.floatLeft(viewLabel);
		if (showGroups) {			
			Collections.sort(chosenColumns);
			Utils.floatLeft(statusPanel, new GroupLabels(this, schema(), chosenColumns));					
		}		
	}
	
	public void resizeInterface() {		
		for (Widget w: toolbars) {						
			rootPanel.setWidgetSize(w, w.getOffsetHeight());			
		}
//		for (Widget w: leftbars) {
//			rootPanel.setWidgetSize(w, w.getOffsetWidth());
//		}
		rootPanel.forceLayout();
	}
	
	protected boolean shouldShowStatusBar() {
		return true;
	}
	
	/**
	 * This can be overridden by subclasses to add more toolbars
	 * or more "leftbars".
	 */
	protected void addToolbars() {
		addToolbar(guideBar, 40);
		if (!showGuide) {
			guideBar.setVisible(false);
		}
		if (shouldShowStatusBar()) {
			addToolbar(spOuter, 40);
		}		
	}
	
	protected void addToolbar(Widget toolbar, int size) {
		toolbars.add(toolbar);
		rootPanel.addNorth(toolbar, size);		
	}
	
	/**
	 * Show the given toolbar (which must previously have been added with addToolbar
	 * or addLeftbar at the right time).
	 * @param toolbar
	 */
	public void showToolbar(Widget toolbar) {
		showToolbar(toolbar, toolbar.getOffsetHeight());
	}
	
	/**
	 * Show the given toolbar (which must previously have been added with addToolbar
	 * or addLeftbar at the right time).
	 * @param toolbar
	 * @param size
	 */
	public void showToolbar(Widget toolbar, int size) {
		toolbar.setVisible(true);
		rootPanel.setWidgetSize(toolbar, size);		
		deferredResize();
	}
	
	public void hideToolbar(Widget toolbar) {
		toolbar.setVisible(false);
		deferredResize();
	}
	
	protected void addLeftbar(Widget leftbar, int size) {
//		leftbars.add(leftbar);
		rootPanel.addWest(leftbar, size);
	}

	/**
	 * Sometimes we need to do a deferred resize, because the layout engine has not finished yet
	 * at the time when we request the resize operation.
	 */
	private void deferredResize() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
			public void execute() {
				resizeInterface();						
			}
		});
	}
	
	/**
	 * This method will be called each time the screen is hidden.
	 * If overriding, make sure to call the superclass method.
	 */
	public void hide() {		
		visible = false;
	}
	
	public void addMenu(MenuItem m) {
		menuItems.add(m);
	}
	
	public void addAnalysisMenuItem(MenuItem mi) {
		analysisMenuItems.add(mi);
	}
	
	public List<MenuItem> menuItems() {
		return menuItems;
	}
	
	public List<MenuItem> analysisMenuItems() {
		return analysisMenuItems;
	}
	
	/**
	 * Override this method to define the main content of the screen.
	 * Stored state may not have been loaded when this method is invoked.
	 * @return
	 */
	public Widget content() {
		return new SimplePanel();
	}
	
	public Widget bottomContent() {
		return null;
	}
	
	public String key() {
		return key;
	}

	public StorageParser getParser() {
		return getParser(this);
	}
	
	public boolean helpAvailable() {
		return helpHTML != null;
	}
	
	public void showHelp() {
		Utils.showHelp(getHelpHTML(), getHelpImage());		
	}
	
	protected TextResource getHelpHTML() {
		if (helpHTML == null) {
			return resources.defaultHelpHTML();
		} else {
			return helpHTML;
		}
	}
	
	protected ImageResource getHelpImage() {
		return helpImage;	
	}
	
	/**
	 * The text that is displayed to first-time users on each screen to assist them.
	 * @return
	 */
	protected String getGuideText() {
		return "Use Instructions on the Help menu to get more information.";
	}

	@Override
	public void onResize() {
		final int c = rootPanel.getWidgetCount();
		for (int i = 0; i < c; ++i) {
			Widget w = rootPanel.getWidget(i);
			if (w instanceof RequiresResize) {
				((RequiresResize) w).onResize();
			}
		}		
	}
	
	/**
	 * Display the sample detail screen and show information about the 
	 * given barcode. 
	 * TODO: this method should probably be somewhere else.
	 * @param b
	 */
	public void displaySampleDetail(OTGSample b) {
		StorageParser p = getParser(this);
		Group g = new Group(schema(), "custom", new OTGSample[] { b });
		storeCustomColumn(p, g);
		configuredProceed(SampleDetailScreen.key);
	}
	
}
