/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.components;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

public class TickMenuItem {

  private boolean ticked = false;
  private MenuItem mi;
  private final String title;
  private final boolean withImage;

  public TickMenuItem(String title, boolean initState, boolean withImage) {
    ticked = initState;
    this.withImage = withImage;
    this.title = title;

    mi = new MenuItem(title, true, new Command() {
      @Override
      public void execute() {
        setState(!ticked);
        stateChange(ticked);
      }

    });
    setState(ticked);
  }

  public TickMenuItem(MenuBar mb, String title, boolean initState) {
    this(title, initState, true);
    mb.addItem(mi);
  }

  public MenuItem menuItem() {
    return mi;
  }

  protected boolean getState(boolean state) {
    return ticked;
  }

  protected void setState(boolean state) {
    ticked = state;
    setHTML(withImage);
  }

  protected void setHTML(boolean withImage) {
    if (!withImage) {
      mi.setHTML(title);
    } else if (ticked) {
      mi.setHTML("<img src=\"images/tick_16.png\">" + title);
    } else {
      mi.setHTML("<img src=\"images/blank_16.png\">" + title);
    }
  }

  public void stateChange(boolean newState) {

  }

}
