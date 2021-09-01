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

package t.viewer.client;

import t.viewer.client.screen.*;
import t.viewer.client.screen.data.DataScreen;
import t.viewer.client.screen.groupdef.ColumnScreen;
import t.viewer.client.screen.ranking.RankingScreen;
import t.viewer.shared.OTGSchema;
import t.shared.common.DataSchema;

public class OTGViewer extends TApplication {

  @Override
  protected void initScreens() {
    addScreenSeq(new StartScreen(this));
    addScreenSeq(new ColumnScreen(this));
    addScreenSeq(new SampleSearchScreen(this));
    importingScreen = new DataScreen(this);
    addScreenSeq(importingScreen);
    addScreenSeq(new RankingScreen(this));
    addScreenSeq(new PathologyScreen(this));
    addScreenSeq(new SampleDetailScreen(this));
    addScreenSeq(new MyDataScreen(this));
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
    /*
     * Note: ideally we should not hardcode various instance names here, but instead associate them
     * with factories on a different level, for example set the factory class name as a parameter in
     * web.xml, or use some kind of dependency injection?
     */
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
}
