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

package t.viewer.client.screen;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import t.viewer.client.Analytics;
import t.viewer.client.ClientGroup;
import t.viewer.client.Groups;
import t.viewer.client.Utils;
import t.viewer.client.components.*;
import t.common.shared.Pair;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.AttributeSet;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.storage.Packer.UnpackInputException;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * This screen displays detailed information about a sample or a set of samples, i.e. experimental
 * conditions, histopathological data, blood composition. The samples that can be displayed are the
 * currently configured groups. In addition, a single custom group of samples can be passed to this
 * screen (the "custom column") to make it display samples that are not in the configured groups.
 */

public class SampleDetailScreen extends MinimalScreen
    implements HeatmapTDGrid.Delegate {
  private SampleServiceAsync sampleService;
  
  private DialogBox heatmapDialog;

  public static final String key = "ad";

  VerticalPanel sectionsPanel;
  private Map<String, SampleDetailTable> sections = new HashMap<String, SampleDetailTable>();

  private ListBox columnList = new ListBox();

  private HeatmapTDGrid atd;// = new AnnotationTDGrid(this);

  //  private List<Group> lastColumns;
  private @Nullable SampleColumn currentColumn;

  private Button downloadButton;
  private HorizontalPanel tools;

  private Groups groups;
  private SampleColumn chosenCustomColumn;

  public SampleDetailScreen(ScreenManager man) {
    super("Sample details", key, man);
    groups = new Groups(getStorage().groupsStorage);
    sampleService = man.sampleService();
    atd = new HeatmapTDGrid(this, this);
    mkTools();
  }

  @Override
  public void loadState(AttributeSet attributes) {
    groups.storage().loadFromStorage();
    try {
      chosenCustomColumn = getStorage().customColumnStorage.get();
    } catch (UnpackInputException e) {
      Window.alert("Failed to load custom column: " + e);
      chosenCustomColumn = null;
    }
    // TODO: serialize choice of displayed column, don't reload/rerender unless necessary, reconsider custom column behavior 
    //    // consume the data so the custom column isn't shown when switching back to this screen
    //    getParser().storeCustomColumn(null); 
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

  private SampleDetailTable addSection(String section, Sample[] samples,
                                       Map<String, PrecomputedVarianceSet> varianceMap,
                                       boolean isSection) {
    SampleDetailTable sdt = new SampleDetailTable(SampleDetailScreen.this, section, isSection);
    sections.put(section, sdt);
    sdt.setData(samples, varianceMap);
    return sdt;
  }

  public void loadSections(final HasSamples<Sample> hasSamples, boolean importantOnly) {
    downloadButton.setEnabled(false);
      sampleService.attributeValuesAndVariance(hasSamples.getSamples(), importantOnly,
              new PendingAsyncCallback<Pair<Sample[], Map<String, PrecomputedVarianceSet>>>(SampleDetailScreen.this.manager) {
      @Override
      public void handleFailure(Throwable caught) {
          getLogger().log(Level.WARNING, "sampleService.annotations failed", caught);
          Window.alert("Unable to get sample annotations.");
      }

      @Override
      public void handleSuccess(Pair<Sample[], Map<String, PrecomputedVarianceSet>> pair) {
        sections.clear();
        sectionsPanel.clear();
        Sample[] samples = pair.first();
        Map<String, PrecomputedVarianceSet> varianceMap = pair.second();
        if (samples.length < 1) {
          return;
        }

        // We ensure that the  "default section", which contains basic
        // information about the sample, will show up first.
        sectionsPanel.add(addSection(SampleDetailTable.DEFAULT_SECTION_TITLE, samples, varianceMap, true));

        LinkedList<SampleDetailTable> secList = new LinkedList<>();
        for (Attribute attribute: samples[0].getKeys()) {
          String section = attribute.section();
          if (section != null && !sections.containsKey(section)) {
            secList.add(addSection(section, samples, varianceMap, true));
          }
        }

        Collections.sort(secList, Comparator.comparing(SampleDetailTable::sectionTitle));
        for (SampleDetailTable sdt: secList) {
          sectionsPanel.add(sdt);
        }
      }
    });
  }

  private void updateColumnList() {
    columnList.clear();
    if (groups.activeGroups().size() > 0) {
      for (DataColumn<?> c : groups.activeGroups()) {
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
    groups.storage().loadFromStorage();
    List<ClientGroup> chosenColumns = groups.activeGroups();
    return chosenColumns != null && chosenColumns.size() > 0;
  }

  private void mkTools() {
    HorizontalPanel hp = Utils.mkHorizontalPanel(true);
    tools = Utils.mkWidePanel();
    tools.add(hp);

    hp.add(columnList);

    hp.add(new Button("Mini-heatmap...", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        int index = columnList.getSelectedIndex();
        
        List<ClientGroup> chosenColumns = groups.activeGroups();
        
        // Get compounds for currently selected group
        List<String> compounds = SampleClassUtils.getMajors(schema(), chosenColumns.get(index))
            .collect(Collectors.toList());
        
        // Determine the currently selected column, and choose a representative sample from it
        Sample representativeSample;
        if (index < chosenColumns.size()) {
          representativeSample = chosenColumns.get(index).getSamples()[0];
        } else {
          representativeSample = chosenCustomColumn.getSamples()[0];
        }
        
        SampleClass sampleClass = 
            SampleClassUtils.asMacroClass(representativeSample.sampleClass(), schema());
        atd.initializeState(sampleClass, compounds, true);
        heatmapDialog = Utils.displayInPopup("Visualisation", atd, DialogPosition.Center);
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
          sampleService.prepareAnnotationCSVDownload(currentColumn.getSamples(),
            new PendingAsyncCallback<String>(SampleDetailScreen.this.manager,
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
  protected Widget content() {
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
      for (SampleColumn c : groups.activeGroups()) {
        if (c.getShortTitle().equals(column)) {
          setDisplayColumn(c);
          return;
        }
      }
    }
    Window.alert("Error: no display column selected.");
  }
  
  // AnnotationTDGrid.Delegate method
  @Override
  public void finishedDisplayingValues() {
    Utils.positionPanel(heatmapDialog, DialogPosition.Center);
  }
}
