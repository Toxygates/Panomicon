/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.gwt.viewer.client.screen;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import t.gwt.common.client.components.StringArrayTable;
import t.gwt.viewer.client.Utils;

import javax.annotation.Nullable;

import static t.gwt.common.client.Utils.makeScrolled;

/**
 * This is the first screen, where a dataset can be selected.
 */
public class StartScreen extends MinimalScreen {

  public static String key = "st";

  public StartScreen(ScreenManager man) {
    super("Start", key, man, man.resources().startHTML(), null);
  }

  final private HTML welcomeHtml = new HTML();

  @Override
  protected Widget content() {
    VerticalPanel vp = Utils.mkTallPanel();    
    vp.addStyleName("widePanel");
   
    HorizontalPanel hp = Utils.mkWidePanel();
    vp.add(hp);
    
    hp.add(welcomeHtml);
    welcomeHtml.addStyleName("welcomeText");
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
