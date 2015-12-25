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

package otgviewer.client;

import static t.common.client.Utils.makeScrolled;

import java.util.List;

import otgviewer.client.components.FilterTools;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.client.components.ranking.CompoundRanker;
import t.common.shared.Dataset;
import t.common.shared.SampleClass;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class RankingScreen extends Screen {

  public static final String key = "rank";
  
  private RankingCompoundSelector cs;
  private FilterTools filterTools;
  private ScrollPanel sp;
  
  public RankingScreen(ScreenManager man) {
    super("Compound ranking", key, false, man,
        resources.defaultHelpHTML(), null);
    
    chosenDatasets = appInfo().datasets();
    filterTools = new FilterTools(this) {

      @Override
      public void datasetsChanged(Dataset[] ds) {
        super.datasetsChanged(ds);
        Screen s = RankingScreen.this;
        storeDatasets(getParser(s));
      }      
    };
    this.addListener(filterTools);    
    
    String majorParam = man.schema().majorParameter();
    cs = new RankingCompoundSelector(this, man.schema().title(majorParam)) {
      @Override
      public void changeCompounds(List<String> compounds) {
        super.changeCompounds(compounds);
        storeCompounds(getParser(RankingScreen.this));
      }      
    };
    this.addListener(cs);
    cs.setStylePrimaryName("compoundSelector");
  }

  @Override
  protected void addToolbars() {
      super.addToolbars();
      addToolbar(filterTools, 45);
      addLeftbar(cs, 350);
  }
  
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
  public void changeSampleClass(SampleClass sc) {
      //On this screen, ignore the blank sample class set by
      //DataListenerWidget
      if (!sc.getMap().isEmpty()) {
          super.changeSampleClass(sc);
      }
      
      storeSampleClass(getParser());
  }
  
  @Override
  public void resizeInterface() {
      //Test carefully in IE8, IE9 and all other browsers if changing this method
      cs.resizeInterface();     
      super.resizeInterface();        
  }
  
  public void show() {
    super.show();
  }
  
  @Override
  public String getGuideText() {
      return "Specify at least one gene symbol to rank compounds according to their effect.";
  }

}
