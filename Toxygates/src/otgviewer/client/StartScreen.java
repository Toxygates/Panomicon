/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package otgviewer.client;

import static t.common.client.Utils.makeScrolled;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.DLWScreen;
import otgviewer.client.components.ScreenManager;
import t.common.client.components.StringArrayTable;
import t.viewer.client.Utils;

/**
 * This is the first screen, where a dataset can be selected.
 */
public class StartScreen extends DLWScreen {

  public static String key = "st";

  public StartScreen(ScreenManager man) {
    super("Start", key, false, man, man.resources().startHTML(), null);
  }

  final private HTML welcomeHtml = new HTML();

  @Override
  public Widget content() {
    VerticalPanel vp = Utils.mkTallPanel();
    vp.setWidth("100%");
   
    HorizontalPanel hp = Utils.mkWidePanel();
    vp.add(hp);
    
    hp.add(welcomeHtml);
    welcomeHtml.setWidth("40em");
    Utils.loadHTML(manager.appInfo().welcomeHtmlURL(), new Utils.HTMLCallback() {
      @Override
      protected void setHTML(String html) {
        welcomeHtml.setHTML(html);
      }
    });
    
    factory().sampleSummaryTable(this,
      (StringArrayTable table) -> vp.add(table));

    return makeScrolled(vp);
  }

  @Override
  public String getGuideText() {
    return "Welcome.";
  }

  @Override
  @Nullable
  public String additionalNavlinkStyle() {
    return "start";
  }
}
