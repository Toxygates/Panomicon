package otgviewer.client;

import static otg.model.sample.OTGAttribute.*;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.DLWScreen;
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
  public void sampleSummaryTable(DLWScreen screen,
                                             ValueAcceptor<StringArrayTable> acceptor) {
    UserDataServiceAsync userData = screen.manager().userDataService();
    Dataset d = new Dataset("adjuvant", null, null, null, null, 0);
    Attribute[] rowAttributes =  { Compound, ExposureTime };
    Attribute[] colAttributes = { Organism, Organ };
    
    userData.datasetSampleSummary(d, rowAttributes, colAttributes,
        AdmRoute,
      new PendingAsyncCallback<String[][]>(screen) {
        public void handleSuccess(String[][] data) {
          StringArrayTable r = new StringArrayTable(data);
          r.setWidth("800px");
          acceptor.acceptValue(r);
        }
      });
  }

}
