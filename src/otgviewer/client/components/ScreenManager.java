package otgviewer.client.components;


import com.google.gwt.user.client.ui.MenuBar;

public interface ScreenManager {
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
}
