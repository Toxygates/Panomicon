package otgviewer.client.components;


import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.ProvidesResize;

public interface ScreenManager extends ProvidesResize {
//
//	void showTemporary(Screen s);
//	
	MenuBar getMenuBar();
	
	void setConfigured(Screen s, boolean configured);
	
	/**
	 * Invalidate all screens' "configured" state and subsequently
	 * attempt their reconfiguration.
	 * Precondition: all screens must have been displayed at least once
	 * using Screen.show()
	 */
	void deconfigureAll(Screen from);
	
	boolean isConfigured(String key);
	
	/**
	 * Try to proceed to a new screen.
	 * @param to
	 */
	void attemptProceed(String to);
	
}
