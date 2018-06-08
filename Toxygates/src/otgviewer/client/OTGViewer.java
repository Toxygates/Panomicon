/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.OTGSchema;
import t.common.shared.DataSchema;
import t.viewer.client.PersistedState;

public class OTGViewer extends TApplication {

  @Override
  protected void initScreens() {
    addScreenSeq(new StartScreen(this));
    addScreenSeq(new ColumnScreen(this));
    addScreenSeq(new SampleSearchScreen(this));   
    addScreenSeq(new MultiDataScreen(this));
    addScreenSeq(new RankingScreen(this));
    addScreenSeq(new PathologyScreen(this));
    addScreenSeq(new SampleDetailScreen(this));    

    if (factory.hasMyData()) {      
      addScreenSeq(new MyDataScreen(this));
    }
  }

  final private OTGSchema schema = new OTGSchema();

  @Override
  public DataSchema schema() {
    return schema;
  }

  private UIFactory factory;

  @Override
  protected void setupUIBase() {
    initFactory();
    super.setupUIBase();
  }

  private void initFactory() {
    UIFactory f;
    //TODO hardcoding these instance names here may be controversial
    // - think of a better way of handling this
    switch (appInfo().instanceName()) {
      case "toxygates":
      case "tg-update":
        f = new ClassicOTGFactory();
        break;
      case "adjuvant":
      case "dev":
        f = new AdjuvantFactory();
        break;
      default:
        f = new OTGFactory();        
    }
    logger.info("Using factory: " + f.getClass().toString());
    factory = f;
  }
  
  @Override
  public UIFactory factory() {    
    return factory;
  }

  @Override
  protected List<PersistedState<?>> getPersistedItems() {
    List<PersistedState<?>> r = new ArrayList<PersistedState<?>>();
    r.addAll(super.getPersistedItems());
    return r;
  }  
}
