package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.Resources;
import otgviewer.client.SampleDetailScreen;
import otgviewer.client.Utils;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.ProvidesResize;
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
	private boolean showDataFilter = false, showGroups = false;
	private MenuBar menuBar;	
	
	/**
	 * Is this screen currently configured?
	 */
	protected boolean configured = false;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	
	/**
	 * Widgets to be shown below the main content area, if any.
	 */
	private Widget bottom;
	private HorizontalPanel spOuter;
	
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
	
	/**
	 * Help text for this screen.
	 */
	protected TextResource helpHTML;
	/**
	 * Image to show alongside the help text for this screen.
	 */
	protected ImageResource helpImage;
	
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
	
	private Set<QueuedAction> actionQueue = new HashSet<QueuedAction>(); 
	
	private void runActions() {
		for (QueuedAction qa: actionQueue) {
			qa.run();
		}
		actionQueue.clear();
	}

	public void enqueue(QueuedAction qa) {
		actionQueue.remove(qa); //remove it if it's already there (so we can update it)
		actionQueue.add(qa);
	}
	
	public Screen(String title, String key,  
			boolean showDataFilter, boolean showGroups, 
			ScreenManager man,
			TextResource helpHTML, ImageResource helpImage) {
		this.showDataFilter = showDataFilter;
		this.showGroups = showGroups;
		this.helpHTML = helpHTML;
		this.helpImage = helpImage;
		
		//PX must be used for measurements or there will be problems in e.g. internet explorer.
		//This problem might possibly be solved if everything is changed to use the new-style
		//LayoutPanels.
		rootPanel = new DockLayoutPanel(Unit.PX); 
		
		initWidget(rootPanel);
		menuBar = man.getMenuBar();
		manager = man;				
		viewLabel.setWordWrap(false);
		viewLabel.getElement().getStyle().setMargin(2, Unit.PX);
		this.key = key;
		
		setTitle(title);		
	}
	
	public Screen(String title, String key,  
			boolean showDataFilter, boolean showGroups, ScreenManager man) {
		this(title, key, showDataFilter, showGroups, man, resources.defaultHelpHTML(), null);
	}
	
	public ScreenManager manager() {
		return this.manager;
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
	
	public void initGUI() {
		statusPanel = new FlowPanel(); 
		statusPanel.setStyleName("statusPanel");		
		Utils.floatLeft(statusPanel);

		spOuter = Utils.mkWidePanel();		
		spOuter.setHeight("30px");
		spOuter.add(statusPanel);		
		spOuter.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		statusPanel.setStyleName("statusPanel");
		spOuter.setStyleName("statusPanel");	

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
	
	
	
	/**
	 * This method will be called each time the screen is displayed anew.
	 * If overriding, make sure to call the superclass method.
	 */
	public void show() {
		rootPanel.forceLayout();
		visible = true;		
		for (MenuItem mi: menuItems) {
			mi.setVisible(true);
		}
		loadState();
		updateStatusPanel(); //needs access to the groups from loadState
		runActions();
		deferredResize();
	}

	/**
	 * The standard status panel contains a label that indicates the current data set,
	 * and descriptions of the currently defined groups.
	 */
	private void updateStatusPanel() {
		statusPanel.clear();
		statusPanel.add(viewLabel);
		Utils.floatLeft(viewLabel);
		if (showGroups) {
			Collections.sort(chosenColumns);
			
			for (Group g: chosenColumns) {				
				FlowPanel fp = new FlowPanel(); 
				fp.setStyleName("statusBorder");
				String tip = g.getCDTs(-1, ", ");
				Label l = Utils.mkEmphLabel(g.getName() + ":");
				l.setWordWrap(false);
				l.getElement().getStyle().setMargin(2, Unit.PX);
				l.setStyleName(g.getStyleName());
				Utils.floatLeft(fp, l);
				l.setTitle(tip);
				l = new Label(g.getCDTs(2, ", "));
				l.getElement().getStyle().setMargin(2, Unit.PX);
				l.setStyleName(g.getStyleName());
				Utils.floatLeft(fp, l);
				l.setTitle(tip);
				l.setWordWrap(false);
				Utils.floatLeft(statusPanel, fp);				
			}
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
	
	/**
	 * This can be overridden by subclasses to add more toolbars
	 * or more "leftbars".
	 */
	protected void addToolbars() {
		addToolbar(spOuter, 40);
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
		for (MenuItem mi: menuItems) {
			mi.setVisible(false);
		}
	}
	
	public void addMenu(MenuItem m) {
		menuBar.addItem(m);
		m.setVisible(false);
		menuItems.add(m);
	}

	
	/**
	 * Override this method to define the main content of the screen.
	 * Stored state may not have been loaded when this method is invoked.
	 * @return
	 */
	public Widget content() {
		return new SimplePanel();
	}
	
	/**
	 * Override this method to define the widgets that should be displayed
	 * below the main content.
	 * @return
	 */	
	public Widget bottomContent() {
		return null;
	}
	
	public String key() {
		return key;
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);
		if (showDataFilter) {
			viewLabel.setText(filter.toString());
		}
	}
	
	public void showHelp() {
		Utils.showHelp(getHelpHTML(), getHelpImage());		
	}
	
	protected TextResource getHelpHTML() {
		return helpHTML;
	}
	
	protected ImageResource getHelpImage() {
		return helpImage;	
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
	public void displaySampleDetail(Barcode b) {
		storeCustomColumn(b);
		configuredProceed(SampleDetailScreen.key);
	}
	
}
