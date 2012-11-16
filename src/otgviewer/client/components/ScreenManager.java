package otgviewer.client.components;

import otgviewer.client.Screen;

import com.google.gwt.user.client.ui.MenuBar;

public interface ScreenManager {

	void showTemporary(Screen s);
	
	MenuBar getMenuBar();
}
