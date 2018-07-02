/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

import com.google.gwt.resources.client.*;

public interface Resources extends t.common.client.Resources {

  public interface OtgCssResource extends CssResource {
    String group0_color();
    String group1_color();
    String group2_color();
    String group3_color();
    String group4_color();
    String group5_color();
    String group6_color();
    String group7_color();
    int menubarpanel_height();
    int navpanel_height();
  }

  @Source("otgviewer/client/OTGViewer.gss")
  OtgCssResource otgViewerStyle();

  @Source("otgviewer/client/txgicons-Regular.woff")
  DataResource iconsWoff();

  @Source("help/default.html")
  TextResource defaultHelpHTML();

  @Source("help/groupDefinition.png")
  ImageResource groupDefinitionHelp();

  @Source("help/groupDefinition.html")
  TextResource groupDefinitionHTML();

  @Source("help/probeSelection.png")
  ImageResource probeSelectionHelp();

  @Source("help/probeSelection.html")
  TextResource probeSelectionHTML();

  @Source("help/dataDisplay.html")
  TextResource dataDisplayHTML();

  @Source("help/dataDisplay.png")
  ImageResource dataDisplayHelp();

  @Source("help/compoundRanking.html")
  TextResource compoundRankingHTML();

  @Source("help/compoundRanking.png")
  ImageResource compoundRankingHelp();
  
  @Source("help/sampleSearch.html")
  TextResource sampleSearchHTML();

  @Source("help/sampleSearch.png")
  ImageResource sampleSearchHelp();

  @Source("help/start.html")
  TextResource startHTML();

  @Source("help/about.html")
  TextResource aboutHTML();

  @Source("help/about.png")
  ImageResource about();

  @Source("help/versions.html")
  TextResource versionHTML();

}
