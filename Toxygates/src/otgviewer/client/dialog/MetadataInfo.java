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

public class MetadataInfo extends Composite {

  public static MetadataInfo fromPlatforms(Platform[] platforms) {
    String[] titles = new String[platforms.length];
    String[] comments = new String[platforms.length];
    
    for (int i = 0; i < platforms.length; ++i) {
      titles[i] = platforms[i].getTitle();
      comments[i] = platforms[i].getPublicComment();
    }
    
    return new MetadataInfo("Platform", titles, comments);
  }
  
  public static MetadataInfo annotations() {
    /*
     * Note: the only data sources hardcoded here should be the ones
     * whose provisioning is independent of SPARQL data that we 
     * control. For example, the ones obtained solely from remote 
     * sources.
     */
    String[] titles = new String[] { 
        "ChEMBL",
        "DrugBank",
        };
    String[] comments = new String[] { 
        "Dynamically obtained from https://www.ebi.ac.uk/rdf/services/chembl/sparql",
        "Dynamically obtained from http://drugbank.bio2rdf.org/sparql",
        };
    
    
    return new MetadataInfo("Annotation", titles, comments);
  }

  public MetadataInfo(String type, String[] titles, String[] comments) {  
    VerticalPanel vp = new VerticalPanel();
    initWidget(vp);
    
    Grid g = new Grid(titles.length + 1, 2);
    vp.add(g);
    g.setStylePrimaryName("metadata-grid");
    g.getColumnFormatter().setStyleName(0, "metadata-firstColumn");
    g.getRowFormatter().setStyleName(0, "metadata-firstRow");
    
    Label l = new Label(type);
    l.setStylePrimaryName("metadata-info-heading");
    g.setWidget(0, 0, l);
    l = new Label("Comment");
    l.setStylePrimaryName("metadata-info-heading");
    g.setWidget(0, 1, l);
    
    for (int i = 1; i < titles.length + 1; ++ i) {    
      l = new Label(titles[i-1]);
      l.setStylePrimaryName("metadata-info-title");
      g.setWidget(i, 0, l);
      
      l = new Label(comments[i-1]);
      l.setStylePrimaryName("metadata-info-comment");
      g.setWidget(i, 1, l);
    }  
  }
}
