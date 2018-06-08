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

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SampleServiceAsync;

/**
 * This screen displays detailed information about a sample or a set of samples, i.e. experimental
 * conditions, histopathological data, blood composition. The samples that can be displayed are the
 * currently configured groups. In addition, a single custom group of samples can be passed to this
 * screen (the "custom column") to make it display samples that are not in the configured groups.
 */

public class SampleDetailScreen extends MinimalScreen {
  private SampleServiceAsync sampleService;

  public static final String key = "ad";

  VerticalPanel sectionsPanel;
  private Map<String, SampleDetailTable> sections = new HashMap<String, SampleDetailTable>();

  private ListBox columnList = new ListBox();

  private AnnotationTDGrid atd;// = new AnnotationTDGrid(this);

  //  private List<Group> lastColumns;
  private @Nullable SampleColumn currentColumn;

  private Button downloadButton;
  private HorizontalPanel tools;

  private List<Group> chosenColumns = new ArrayList<Group>();
  private SampleColumn chosenCustomColumn;
  private SampleClass chosenSampleClass;

  public SampleDetailScreen(ScreenManager man) {
    super("Sample details", key, man);
    sampleService = man.sampleService();
    atd = new AnnotationTDGrid(this);
    mkTools();
  }

  @Override
  public void loadState(AttributeSet attributes) {
    chosenColumns = getParser().getChosenColumns(schema(), attributes);
    chosenCustomColumn = getParser().getCustomColumn(schema(), attributes);
    // TODO: serialize choice of displayed column, don't reload/rerender unless necessary, reconsider custom column behavior 
    //    // consume the data so the custom column isn't shown when switching back to this screen
    //    getParser().storeCustomColumn(null); 
    chosenSampleClass = getParser().getSampleClass(attributes);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    addToolbar(tools, 30);
  }

  @Override
  public String getGuideText() {
    return "Here you can view experimental information and biological details for each sample in the groups you have defined.";
  }

  private SampleDetailTable addSection(String section, Annotation[] annotations,
      HasSamples<Sample> c, boolean isSection) {
    SampleDetailTable sdt = new SampleDetailTable(SampleDetailScreen.this, section, isSection);
    sections.put(section, sdt);
    sdt.setData(c, annotations);
    return sdt;
  }

  public void loadSections(final HasSamples<Sample> hasSamples, boolean importantOnly) {
    downloadButton.setEnabled(false);
    sampleService.annotations(hasSamples, importantOnly, new PendingAsyncCallback<Annotation[]>(
        SampleDetailScreen.this) {
      @Override
      public void handleFailure(Throwable caught) {
        Window.alert("Unable to get array annotations.");
      }

      @Override
      public void handleSuccess(Annotation[] annotations) {
        sections.clear();
        sectionsPanel.clear();
        if (annotations.length < 1) {
          return;
        }
        SampleDetailTable sec = addSection(null, annotations, hasSamples, true);
        sectionsPanel.add(sec);

        LinkedList<SampleDetailTable> secList =
            new LinkedList<SampleDetailTable>();
        for (BioParamValue bp: annotations[0].getAnnotations()) {
          if (bp.section() != null &&
              !sections.containsKey(bp.section())) {
            secList.add(addSection(bp.section(), annotations, hasSamples, true));
          }
        }

        Collections.sort(secList, new Comparator<SampleDetailTable>() {
          @Override
          public int compare(SampleDetailTable o1, SampleDetailTable o2) {
            return o1.sectionTitle().compareTo(o2.sectionTitle());
          }
        });
        for (SampleDetailTable sdt: secList) {
          sectionsPanel.add(sdt);
        }
      }
    });
  }

  private void updateColumnList() {
    columnList.clear();
    if (chosenColumns.size() > 0) {
      for (DataColumn<?> c : chosenColumns) {
        columnList.addItem(c.getShortTitle());
      }
    }
    if (chosenCustomColumn != null) {
      columnList.addItem(chosenCustomColumn.getShortTitle());
      columnList.setSelectedIndex(columnList.getItemCount() - 1);
    } else {
      columnList.setSelectedIndex(0);
    }
  }

  @Override
  public void show() {
    super.show();
    if (visible) {
      updateColumnList();
      displayWith(columnList.getItemText(columnList.getSelectedIndex()));
      //      lastColumns = chosenColumns;
    }
  }

  @Override
  public boolean enabled() {
    return manager.isConfigured(ColumnScreen.key);
  }

  private void mkTools() {
    HorizontalPanel hp = Utils.mkHorizontalPanel(true);
    tools = Utils.mkWidePanel();
    tools.add(hp);

    hp.add(columnList);

    hp.add(new Button("Mini-heatmap...", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        List<String> compounds = chosenColumns.stream().flatMap(c -> SampleClassUtils.getMajors(schema(), c)).distinct()
            .collect(Collectors.toList());
        atd.sampleClassChanged(chosenSampleClass);
        atd.compoundsChanged(compounds);
        Utils.displayInPopup("Visualisation", atd, DialogPosition.Center);
      }
    }));

    columnList.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent ce) {
        displayWith(columnList.getItemText(columnList.getSelectedIndex()));
      }
    });

    downloadButton = new Button("Download CSV...", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (currentColumn == null) {
          return;
        }
        sampleService.prepareAnnotationCSVDownload(currentColumn,
            new PendingAsyncCallback<String>(SampleDetailScreen.this,
            "Unable to prepare the data for download,") {
          @Override
          public void handleSuccess(String url) {
                Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
                    Analytics.ACTION_DOWNLOAD_SAMPLE_DETAILS);
            Utils.displayURL("Your download is ready.", "Download", url);
          }
        });
      }
    });

    hp.add(downloadButton);
  }

  @Override
  public Widget content() {
    sectionsPanel = Utils.mkVerticalPanel();

    HorizontalPanel hp = Utils.mkWidePanel(); // to make it centered
    hp.add(sectionsPanel);
    return new ScrollPanel(hp);
  }

  private void setDisplayColumn(SampleColumn c) {
    loadSections(c, false);
    currentColumn = c;
    downloadButton.setEnabled(true);
    //    SampleClass sc = SampleClassUtils.asMacroClass(c.getSamples()[0].sampleClass(),
    //        manager.schema());
    //    atd.sampleClassChanged(sc);
  }

  private void displayWith(String column) {
    if (chosenCustomColumn != null && column.equals(chosenCustomColumn.getShortTitle())) {
      setDisplayColumn(chosenCustomColumn);
      return;
    } else {
      for (SampleColumn c : chosenColumns) {
        if (c.getShortTitle().equals(column)) {
          setDisplayColumn(c);
          return;
        }
      }
    }
    Window.alert("Error: no display column selected.");
  }
}
