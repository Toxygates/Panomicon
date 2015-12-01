/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

package otgviewer.client.dialog;

import t.common.shared.Platform;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PlatformInfo extends Composite {

  public PlatformInfo(Platform[] platforms) {
    VerticalPanel vp = new VerticalPanel();
    initWidget(vp);
    
    Grid g = new Grid(platforms.length + 1, 2);
    vp.add(g);
    g.setStylePrimaryName("platform-grid");
    
    Label l = new Label("Platform");
    l.setStylePrimaryName("platform-info-heading");
    g.setWidget(0, 0, l);
    l = new Label("Comment");
    l.setStylePrimaryName("platform-info-heading");
    g.setWidget(0, 1, l);
    
    int i = 1;
    for (Platform p: platforms) {
      l = new Label(p.getTitle());
      l.setStylePrimaryName("platform-info-title");
      g.setWidget(i, 0, l);
      
      l = new Label(p.getPublicComment());
      l.setStylePrimaryName("platform-info-comment");
      g.setWidget(i, 1, l);
      i++;
    }
  }  
}
