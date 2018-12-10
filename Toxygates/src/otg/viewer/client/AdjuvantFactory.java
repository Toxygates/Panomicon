/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package otg.viewer.client;

import static otg.model.sample.OTGAttribute.*;

import otg.viewer.client.components.PendingAsyncCallback;
import otg.viewer.client.components.Screen;
import t.common.client.ValueAcceptor;
import t.common.client.components.StringArrayTable;
import t.common.shared.Dataset;
import t.model.sample.Attribute;
import t.viewer.client.rpc.UserDataServiceAsync;

/** 
 * UI factory for the Adjuvant Database application.
 */

public class AdjuvantFactory extends OTGFactory {

  @Override
  public void sampleSummaryTable(Screen screen,
      ValueAcceptor<StringArrayTable> acceptor) {
    UserDataServiceAsync userData = screen.manager().userDataService();
    Dataset d = new Dataset("adjuvant", null, null, null, null, 0);
    Attribute[] rowAttributes =  { Compound, ExposureTime };
    Attribute[] colAttributes = { Organism, Organ };
    
    userData.datasetSampleSummary(d, rowAttributes, colAttributes,
        AdmRoute,
      new PendingAsyncCallback<String[][]>(screen) {
        @Override
        public void handleSuccess(String[][] data) {
          StringArrayTable r = new StringArrayTable(data);
          r.setWidth("800px");
          acceptor.acceptValue(r);
        }
      });
  }

}
