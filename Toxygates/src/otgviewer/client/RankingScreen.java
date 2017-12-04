/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otgviewer.client;

import static t.common.client.Utils.makeScrolled;

import java.util.List;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import otgviewer.client.components.FilterTools;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.client.components.ranking.CompoundRanker;
import t.common.shared.Dataset;

public class RankingScreen extends DataFilterScreen {

  public static final String key = "rank";

  private RankingCompoundSelector cs;
  private FilterTools filterTools;
  private ScrollPanel sp;

  public RankingScreen(ScreenManager man) {
    super("Compound ranking", key, false, man,
        man.resources().compoundRankingHTML(),
        man.resources().compoundRankingHelp());
    chosenDatasets = appInfo().datasets();
    filterTools = new FilterTools(this) {
      @Override
      public void datasetsChanged(Dataset[] ds) {
        super.datasetsChanged(ds);
        cs.datasetsChanged(ds);
        //TODO: should really change the datasets in the RankingScreen
        //but that would cause an infinite loop. 
        //Think about changing the way we propagate events and data.
      }      
    };
    this.addListener(filterTools);

    cs = new RankingCompoundSelector(this, man.schema().majorParameter().title()) {
      @Override
      public void changeCompounds(List<String> compounds) {
        super.changeCompounds(compounds);
        storeCompounds(getParser(RankingScreen.this));
      }
    };
    this.addListener(cs);
    cs.addStyleName("compoundSelector");
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    addToolbar(filterTools, 45);
    addLeftbar(cs, 350);
  }

  @Override
  public Widget content() {
    CompoundRanker cr = factory().compoundRanker(this, cs);
    sp = makeScrolled(cr);
    return sp;
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
  public void resizeInterface() {
    // Test carefully in IE8, IE9 and all other browsers if changing this method
    cs.resizeInterface();
    super.resizeInterface();
  }

  @Override
  public String getGuideText() {
    return "Specify at least one gene symbol to rank compounds according to their effect.";
  }  
}
