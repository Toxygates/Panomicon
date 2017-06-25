/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.Pathology;
import t.common.client.ImageClickCell;
import t.common.shared.GroupUtils;
import t.common.shared.SampleClass;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import t.common.shared.sample.SampleColumn;
import t.viewer.client.Utils;
import otgviewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This screen displays information about pathological findings in a given set of sample groups.
 */
public class PathologyScreen extends Screen {
  public static final String key = "path";

  private CellTable<Pathology> pathologyTable = new CellTable<Pathology>();
  private ScrollPanel scrollPanel = new ScrollPanel();
  private Set<Pathology> pathologies = new HashSet<Pathology>();
  private final Resources resources;

  private SampleClass lastClass;
  private List<Group> lastColumns;

  @Override
  public boolean enabled() {
    // TODO check the groups for vivo samples instead
    // CellType ct = CellType.valueOf(chosenSampleClass.get("test_type"));
    return manager.isConfigured(ColumnScreen.key); // && ct == CellType.Vivo;
  }

  private final SparqlServiceAsync sparqlService;

  public PathologyScreen(ScreenManager man) {
    super("Pathologies", key, true, man);
    resources = man.resources();
    sparqlService = man.sparqlService();
    mkTools();
  }

  private HorizontalPanel tools = Utils.mkWidePanel();

  private void mkTools() {
    HTML h = new HTML();
    h.setHTML("<a href=\"" + appInfo().pathologyTermsURL() + "\" target=_new>"
        + "Pathology terms reference</a>");
    tools.add(h);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    addToolbar(tools, 30);
  }

  public Widget content() {

    scrollPanel.setWidget(pathologyTable);
    pathologyTable.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    pathologyTable.setWidth("100%");

    TextColumn<Pathology> col = new TextColumn<Pathology>() {
      public String getValue(Pathology p) {
        List<Group> gs = GroupUtils.groupsFor(chosenColumns, p.barcode());
        StringBuilder sb = new StringBuilder();
        for (Group g : gs) {
          sb.append(g.getName());
          sb.append(" ");
        }
        if (gs.size() > 0) {
          return sb.toString();
        } else {
          return "None";
        }
      }
    };
    pathologyTable.addColumn(col, "Group");

    col = new TextColumn<Pathology>() {
      public String getValue(Pathology p) {
        Sample b = GroupUtils.sampleFor(chosenColumns, p.barcode());
        return b.get("compound_name") + "/" + b.getShortTitle(schema()) + "/"
            + b.get("individual_id");
      }
    };
    pathologyTable.addColumn(col, "Sample");

    col = new TextColumn<Pathology>() {
      public String getValue(Pathology p) {
        return p.finding();
      }
    };

    ToolColumn tcl = new ToolColumn(new InspectCell());
    pathologyTable.addColumn(tcl, "");
    pathologyTable.setColumnWidth(tcl, "40px");
    tcl.setCellStyleNames("clickCell");

    pathologyTable.addColumn(col, "Finding");

    col = new TextColumn<Pathology>() {
      public String getValue(Pathology p) {
        return p.topography();
      }
    };
    pathologyTable.addColumn(col, "Topography");

    col = new TextColumn<Pathology>() {
      public String getValue(Pathology p) {
        return p.grade();
      }
    };
    pathologyTable.addColumn(col, "Grade");

    col = new TextColumn<Pathology>() {
      public String getValue(Pathology p) {
        return "" + p.spontaneous();
      }
    };
    pathologyTable.addColumn(col, "Spontaneous");

    Column<Pathology, SafeHtml> lcol = new Column<Pathology, SafeHtml>(new SafeHtmlCell()) {
      public SafeHtml getValue(Pathology p) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (p.viewerLink() != null) {
          b.appendHtmlConstant("<a target=_new href=\"" + p.viewerLink() + "\">Viewer</a>");
        } else {
          b.appendHtmlConstant("No image");
        }
        return b.toSafeHtml();
      }
    };
    pathologyTable.addColumn(lcol, "Digital viewer");

    return scrollPanel;
  }

  @Override
  public void show() {
    super.show();
    if (visible
        && (lastClass == null || !lastClass.equals(chosenSampleClass) || lastColumns == null || !chosenColumns
            .equals(lastColumns))) {
      pathologies.clear();
      for (SampleColumn c : chosenColumns) {
        sparqlService.pathologies(c, new AsyncCallback<Pathology[]>() {
          public void onFailure(Throwable caught) {
            Window.alert("Unable to get pathologies.");
          }

          public void onSuccess(Pathology[] values) {
            pathologies.addAll(Arrays.asList(values));
            pathologyTable.setRowData(new ArrayList<Pathology>(pathologies));
          }
        });
      }
      lastClass = chosenSampleClass;
      lastColumns = chosenColumns;
    }
  }


  class InspectCell extends ImageClickCell.StringImageClickCell {
    InspectCell() {
      super(resources.magnify(), false);
    }

    public void onClick(String value) {
      displaySampleDetail(GroupUtils.sampleFor(chosenColumns, value));
    }
  }

  class ToolColumn extends Column<Pathology, String> {
    public ToolColumn(InspectCell tc) {
      super(tc);
    }

    public String getValue(Pathology p) {
      return p.barcode();
    }
  }

  @Override
  public String getGuideText() {
    return "This is the list of pathologies in the sample groups you have defined. Click on an icon to see detailed sample information.";
  }
}
