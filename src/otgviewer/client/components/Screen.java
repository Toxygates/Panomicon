package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import otgviewer.client.Resources;
import otgviewer.client.SampleDetailScreen;
import otgviewer.client.Utils;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
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
 * Part of a sequence of screens in a workflow.
 * Each screen knows what its parent is, and renders a sequence of links to all the parents.
 * @author johan
 *
 */
public class Screen extends DataListenerWidget implements RequiresResize, ProvidesResize {
	protected static Resources resources = GWT.create(Resources.class);
	
	protected DockLayoutPanel rootPanel;
	private String key; //An identifier string

	private FlowPanel statusPanel;		
	protected boolean visible = false;
	private Label viewLabel = new Label();
	private boolean showDataFilter = false, showGroups = false;
	private MenuBar menuBar;	
	protected boolean configured = false;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	private Widget bottom;
	private HorizontalPanel spOuter;
	
	protected ScreenManager manager;
	
	protected TextResource helpHTML;
	protected ImageResource helpImage;
	
	public Screen(String title, String key,  
			boolean showDataFilter, boolean showGroups, 
			ScreenManager man,
			TextResource helpHTML, ImageResource helpImage) {
		this.showDataFilter = showDataFilter;
		this.showGroups = showGroups;
		this.helpHTML = helpHTML;
		this.helpImage = helpImage;
		rootPanel = new DockLayoutPanel(Unit.EM);
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
		
	protected void configuredProceed(String key) {
		setConfigured(true);
		manager.attemptProceed(key);		
	}
	
	public void initGUI() {
		statusPanel = new FlowPanel(); 
		statusPanel.setStyleName("statusPanel");		
		floatLeft(statusPanel);

		spOuter = Utils.mkWidePanel();		
		spOuter.setHeight("3em");
		spOuter.add(statusPanel);		
		spOuter.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		statusPanel.setStyleName("statusPanel");
		spOuter.setStyleName("statusPanel");	

		rootPanel.addNorth(spOuter, 2.5);		
		bottom = bottomContent();
		if (bottom != null) {
			HorizontalPanel hp = Utils.mkWidePanel();
			hp.add(bottom);
			rootPanel.addSouth(hp, 3);
		}
		rootPanel.add(content());
	}
	
	/**
	 * This method will be called each time the screen is displayed anew.
	 * If overriding, make sure to call the superclass method.
	 */
	public void show() {
		visible = true;		
		for (MenuItem mi: menuItems) {
			mi.setVisible(true);
		}
		loadState();
		updateStatusPanel(); //needs access to the groups from loadState
	}
	
	private void floatLeft(Widget w) {
		w.getElement().getStyle().setFloat(Float.LEFT);
	}
	private void floatLeft(FlowPanel fp, Widget w) {
		floatLeft(w);
		fp.add(w);
	}
	
	protected void updateStatusPanel() {
//		statusPanel.setWidth(Window.getClientHeight() + "px");
		statusPanel.clear();
		statusPanel.add(viewLabel);
		floatLeft(viewLabel);
//		viewLabel.getElement().getStyle().setFloat(Float.LEFT);
		if (showGroups) {
			Collections.sort(chosenColumns);
			
			for (Group g: chosenColumns) {				
				FlowPanel fp = new FlowPanel(); 
				fp.setStyleName("statusBorder");
				String tip = g.getCDTs(-1, ", ");
				Label l = Utils.mkEmphLabel(g.getName() + ":");
				l.setWordWrap(false);
				l.getElement().getStyle().setMargin(2, Unit.PX);
				floatLeft(fp, l);
				l.setTitle(tip);
				l = new Label(g.getCDTs(2, ", "));
				l.getElement().getStyle().setMargin(2, Unit.PX);
				floatLeft(fp, l);
				l.setTitle(tip);
				l.setWordWrap(false);
				floatLeft(statusPanel, fp);
			}
		}
//		rootPanel.forceLayout();
//		rootPanel.setWidgetSize(spOuter, statusPanel.getOffsetHeight());
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
	
	//TODO: best location for this?
	public void displaySampleDetail(Barcode b) {
		storeCustomColumn(b);
		configuredProceed(SampleDetailScreen.key);
	}
	
}
