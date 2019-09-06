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

package otg.viewer.client;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import otg.model.sample.OTGAttribute;
import otg.viewer.client.components.*;
import otg.viewer.client.rpc.SampleServiceAsync;
import otg.viewer.shared.Pathology;
import t.common.client.ImageClickCell;
import t.common.shared.GroupUtils;
import t.common.shared.sample.Sample;
import t.common.shared.sample.SampleColumn;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.*;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This screen displays information about pathological findings in a given set of sample groups.
 */
public class PathologyScreen extends MinimalScreen {
  public static final String key = "path";

  private CellTable<Pathology> pathologyTable;
  private ScrollPanel scrollPanel = new ScrollPanel();
  private Set<Pathology> pathologies = new HashSet<Pathology>();

  private SampleClass lastClass;
  private List<ClientGroup> lastColumns;

  protected SampleClass chosenSampleClass;
  protected Groups groups;

  @Override
  public void loadState(AttributeSet attributes) {
    chosenSampleClass = getStorage().sampleClassStorage.getIgnoringException();
    groups.storage().loadFromStorage();
  }

  public interface Resources extends CellTable.Resources {
    @Override
    @Source("t/viewer/client/table/Tables.gss")
    CellTable.Style cellTableStyle();
  }

  @Override
  public boolean enabled() {
    List<ClientGroup> chosenColumns = groups.activeGroups();
    return chosenColumns != null && chosenColumns.size() > 0;
  }

  private final SampleServiceAsync sampleService;

  public PathologyScreen(ScreenManager man) {
    super("Pathologies", key, man);
    groups = new Groups(getStorage().groupsStorage);
    Resources resources = GWT.create(Resources.class);
    pathologyTable = new CellTable<Pathology>(15, resources);
    sampleService = man.sampleService();
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

  private void addColumn(Column<Pathology, ?> column, String headerString, String borderStyle,
      String columnWidth) {
    addColumn(column, headerString, "", borderStyle, columnWidth);
  }

  private void addColumn(Column<Pathology, ?> column, String headerString, String cellStyle,
      String borderStyle, String columnWidth) {
    column.setCellStyleNames(cellStyle + " " + borderStyle);
    TextHeader header = new TextHeader(headerString);
    header.setHeaderStyleNames(borderStyle);
    pathologyTable.addColumn(column, header);
    pathologyTable.setColumnWidth(column, columnWidth);
  }

  @Override
  protected Widget content() {
    scrollPanel.setWidget(pathologyTable);
    pathologyTable.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    pathologyTable.setWidth("auto");

    TextColumn<Pathology> col = new TextColumn<Pathology>() {
      @Override
      public String getValue(Pathology p) {
        Stream<ClientGroup> gs = GroupUtils.groupsFor(groups.activeGroups(), p.sampleId());        
        String r = gs.map(g -> g.getName()).collect(Collectors.joining(" "));
        if (r.length() == 0) {
          return "None";
        }
        return r;
      }
    };
    addColumn(col, "Group", "", "10em");

    // Note: we may need to stop including p.sample() at some point
    // if pathologies get to have longer barcodes (currently only OTG samples,
    // where sample ID == barcode, have pathologies)
    col = new TextColumn<Pathology>() {
      @Override
      public String getValue(Pathology p) {
        Sample b = GroupUtils.sampleFor(groups.activeGroups(), p.sampleId());
        return b.get(OTGAttribute.Compound) + "/" + b.getShortTitle(schema()) +
            " [" + p.sampleId() + "]";
      }
    };
    addColumn(col, "Sample", "lightBorderLeft", "22em");

    ToolColumn tcl = new ToolColumn(new InspectCell());
    addColumn(tcl, "", "clickCell", "lightBorderLeft", "35px");

    col = new TextColumn<Pathology>() {
      @Override
      public String getValue(Pathology p) {
        return p.finding();
      }
    };
    addColumn(col, "Finding", "lightBorderLeft", "10em");

    col = new TextColumn<Pathology>() {
      @Override
      public String getValue(Pathology p) {
        return p.topography();
      }
    };
    addColumn(col, "Topography", "lightBorderLeft", "8em");

    col = new TextColumn<Pathology>() {
      @Override
      public String getValue(Pathology p) {
        return p.grade();
      }
    };
    addColumn(col, "Grade", "lightBorderLeft", "8em");

    col = new TextColumn<Pathology>() {
      @Override
      public String getValue(Pathology p) {
        return "" + p.spontaneous();
      }
    };
    addColumn(col, "Spontaneous", "lightBorderLeft", "8em");

    Column<Pathology, SafeHtml> lcol = new Column<Pathology, SafeHtml>(new SafeHtmlCell()) {
      @Override
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
    addColumn(lcol, "Digital viewer", "lightBorderLeft", "8em");

    return scrollPanel;
  }

  @Override
  public void show() {
    super.show();
    displayStatusPanel(groups.activeGroups());
    if (visible
        && (lastClass == null || !lastClass.equals(chosenSampleClass) || lastColumns == null || !groups.activeGroups()
            .equals(lastColumns))) {
      pathologies.clear();
      for (SampleColumn c : groups.activeGroups()) {
        sampleService.pathologies(c.getSamples(), new AsyncCallback<Pathology[]>() {
          @Override
          public void onFailure(Throwable caught) {
            getLogger().log(Level.WARNING, "sampleService.pathologies failed", caught);
            Window.alert("Unable to get pathologies.");
          }

          @Override
          public void onSuccess(Pathology[] values) {
            pathologies.addAll(Arrays.asList(values));
            pathologyTable.setRowData(new ArrayList<Pathology>(pathologies));
          }
        });
      }
      lastClass = chosenSampleClass;
      lastColumns = groups.activeGroups();
    }
  }


  class InspectCell extends ImageClickCell.StringImageClickCell {
    InspectCell() {
      super(manager.resources().magnify(), "inspect", false);
    }

    @Override
    public void onClick(String value) {
      ScreenUtils.displaySampleDetail(PathologyScreen.this,
        GroupUtils.sampleFor(groups.activeGroups(), value));
    }
  }

  class ToolColumn extends Column<Pathology, String> {
    public ToolColumn(InspectCell tc) {
      super(tc);
    }

    @Override
    public String getValue(Pathology p) {
      return p.sampleId();
    }
  }

  @Override
  public String getGuideText() {
    return "This is the list of pathologies in the sample groups you have defined. Click on an icon to see detailed sample information.";
  }
}
