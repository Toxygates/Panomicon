package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.Resources;
import otgviewer.client.Utils;
import otgviewer.shared.DataFilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
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
	
	protected DockPanel dockPanel = new DockPanel();
	private String key; //An identifier string

	private HorizontalPanel horizontalPanel;
	private boolean shown = false;
	private Label viewLabel = new Label();
	private boolean showDataFilter = false;
	private MenuBar menuBar;	
	protected boolean configured = false;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	
	protected ScreenManager manager;
	
	protected TextResource helpHTML;
	protected ImageResource helpImage;
	
	public Screen(String title, String key,  
			boolean showDataFilter, ScreenManager man,
			TextResource helpHTML, ImageResource helpImage) {		
		initWidget(dockPanel);
		menuBar = man.getMenuBar();
		manager = man;		
		this.showDataFilter = showDataFilter;
		this.helpHTML = helpHTML;
		this.helpImage = helpImage;
		dockPanel.setWidth("100%");		
		this.key = key;
		
		setTitle(title);		
	}
	
	public Screen(String title, String key,  
			boolean showDataFilter, ScreenManager man) {
		this(title, key, showDataFilter, man, resources.defaultHelpHTML(), null);
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
	
	protected void setConfigured() {
		configured = true;
		manager.setConfigured(this);
	}
	
	/**
	 * Indicate that this screen is no longer configured.
	 */
	public void deconfigure() {
		configured = false;
	}
	
	protected void configuredProceed(String key) {
		setConfigured();
		History.newItem(key);
	}
	
	/**
	 * This method will be called each time the screen is displayed anew.
	 * If overriding, make sure to call the superclass method.
	 */
	public void show() {
		if (!shown) {
			VerticalPanel vp = new VerticalPanel();
			horizontalPanel = new HorizontalPanel();
			vp.add(horizontalPanel);
			dockPanel.add(vp, DockPanel.NORTH);
			dockPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
			dockPanel.add(content(), DockPanel.CENTER);
			vp.add(viewLabel);			
			shown = true;			
//			first show
		} else {
//			later show
		}
		for (MenuItem mi: menuItems) {
			mi.setVisible(true);
		}
		loadState();
	}
	
	/**
	 * This method will be called each time the screen is hidden.
	 * If overriding, make sure to call the superclass method.
	 */
	public void hide() {		
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
		String h = (newHeight - dockPanel.getAbsoluteTop()) + "px";
		dockPanel.setHeight(h);		
		changeHeight(newHeight);
	}
	
	/**
	 * Override this method to define the main content of the screen.
	 * Stored state may not have been loaded when this method is invoked.
	 * @return
	 */
	public Widget content() {
		return new SimplePanel();
	}
	
	public String key() {
		return key;
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);
		if (showDataFilter) {
			viewLabel.setText("Viewing: " + filter.toString());
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
