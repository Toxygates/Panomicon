package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.Resources;
import otgviewer.client.Utils;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * Part of a sequence of screens in a workflow.
 * Each screen knows what its parent is, and renders a sequence of links to all the parents.
 * @author johan
 *
 */
public class Screen extends DataListenerWidget {
	protected static Resources resources = GWT.create(Resources.class);
	
	protected DockPanel rootPanel;
	private String key; //An identifier string

	private HorizontalPanel statusPanel;		
	protected boolean visible = false;
	private Label viewLabel = new Label();
	private boolean showDataFilter = false, showGroups = false;
	private MenuBar menuBar;	
	protected boolean configured = false;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	
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
		rootPanel = new DockPanel();
		initWidget(rootPanel);
		menuBar = man.getMenuBar();
		manager = man;				
		rootPanel.setWidth("100%");
		rootPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		rootPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
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
//		VerticalPanel vp = Utils.mkVerticalPanel();
		statusPanel = Utils.mkHorizontalPanel(true);	
		HorizontalPanel spOuter = new HorizontalPanel();
		
		spOuter.setWidth("100%");
		spOuter.setHeight("3em");
		spOuter.add(statusPanel);		
		spOuter.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		statusPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		statusPanel.setStyleName("statusPanel");
		statusPanel.setHeight("3em");
		spOuter.setStyleName("statusPanel");	

		rootPanel.add(spOuter, DockPanel.NORTH);		
		rootPanel.add(content(), DockPanel.CENTER);	
		rootPanel.add(bottomContent(), DockPanel.SOUTH);
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
	
	protected void updateStatusPanel() {
		statusPanel.clear();
		statusPanel.add(viewLabel);		
		if (showGroups) {
			for (DataColumn dc: chosenColumns) {
				Group g = (Group) dc;
				HorizontalPanel hp = Utils.mkHorizontalPanel(true);
				hp.setStyleName("statusBorder");
				String tip = g.getCDTs(-1, ", ");
				Label l = Utils.mkEmphLabel(g.getName() + ":");
				hp.add(l);
				l.setTitle(tip);
				l = new Label(g.getCDTs(2, ", "));
				hp.add(l);
				l.setTitle(tip);
				statusPanel.add(hp);
			}
		}
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
	
	public void resizeInterface(int newHeight) {
		String h = (newHeight - rootPanel.getAbsoluteTop()) + "px";
		rootPanel.setHeight(h);		
		changeHeight(newHeight);
	}
	
	public int availableHeight() {
		return manager.availableHeight() - statusPanel.getOffsetHeight() - 10;
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
		return new SimplePanel();
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
}
