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

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.NoSelectionModel;
import otg.viewer.client.components.OTGScreen;
import t.common.shared.sample.*;
import t.viewer.client.Utils;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.table.TooltipColumn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;

/**
 * A table that displays sample annotations for a small set of samples.
 */

public class OldSampleDetailTable extends Composite {
  private CellTable<BioParamValue[]> table;
  private Sample[] samples;
  private SampleServiceAsync sampleService;
  private final @Nullable String title;
  private final boolean isSection;
  private final OTGScreen screen;
  public Delegate delegate;

  public static final String DEFAULT_SECTION_TITLE = "Sample details";
  
  public interface Delegate {
    void sampleDetailTableFinishedSettingData();
  }
  
  public interface Resources extends CellTable.Resources {
    @Override
    @Source("t/viewer/client/table/Tables.gss")
    CellTable.Style cellTableStyle();
  }

  protected static class BioParamColumn extends TooltipColumn<BioParamValue[]> {

    private final int i;
    public BioParamColumn(Cell<String> cell, int column) {
      super(cell);
      i = column;
    }

    @Override
    public String getValue(BioParamValue[] object) {
      return object[i].displayValue();
    }

    @Override
    protected String getTooltip(BioParamValue[] item) {
      return item[i].tooltip();
    }

    @Override
    protected void htmlBeforeContent(SafeHtmlBuilder sb, BioParamValue[] object) {
      super.htmlBeforeContent(sb, object);
      BioParamValue bpv = object[i];
      if (bpv instanceof NumericalBioParamValue) {
        NumericalBioParamValue nbpv = (NumericalBioParamValue) bpv;
        if (nbpv.value() == null) {
          // otherwise we get NullPointerExceptions on nbpv.isAbove() 
        } else if (nbpv.isAbove()) {
          sb.append(TEMPLATES.startStyled("numericalParameterAbove"));                  
        } else if (nbpv.isBelow()) {
          sb.append(TEMPLATES.startStyled("numericalParameterBelow"));
        } else if (nbpv.isPathological()) {
          sb.append(TEMPLATES.startStyled("numericalParameterPathological"));
        } else {
          sb.append(TEMPLATES.startStyled("numericalParameterHealthy"));
        }
      }        
    }

    @Override
    protected void htmlAfterContent(SafeHtmlBuilder sb, BioParamValue[] object) {
      super.htmlAfterContent(sb, object);
      BioParamValue bpv = object[i];
      if (bpv instanceof NumericalBioParamValue) {
        sb.append(TEMPLATES.endStyled());
      }
    }        
  }

  public OldSampleDetailTable(OTGScreen screen, @Nullable String title, boolean isSection) {
    this.title = title != null ? title : DEFAULT_SECTION_TITLE;
    this.isSection = isSection;
    this.screen = screen;
    sampleService = screen.manager().sampleService();
    Resources resources = GWT.create(Resources.class);
    table = new CellTable<BioParamValue[]>(15, resources);
    initWidget(table);
    table.setWidth("100%", true); // use fixed layout so we can control column width explicitly
    table.setSelectionModel(new NoSelectionModel<BioParamValue[]>());
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
  }

  public @Nullable String sectionTitle() { return title; }
  
  public void loadFrom(final HasSamples<Sample> c, boolean importantOnly) {
    sampleService.annotations(c.getSamples(), importantOnly, new PendingAsyncCallback<Annotation[]>(
            screen) {
      @Override
      public void handleFailure(Throwable caught) {
        screen.getLogger().log(Level.WARNING, "sampleService.annotations failed", caught);
        Window.alert("Unable to get sample annotations.");
      }

      @Override
      public void handleSuccess(Annotation[] as) {
        setData(c, as);
      }
    });
  }
  
  private void setupColumns(HasSamples<Sample> c) {
    samples = c.getSamples();    
    while (table.getColumnCount() > 0) {
      table.removeColumn(0);
    }
    
    TextColumn<BioParamValue[]> labelCol = new TextColumn<BioParamValue[]>() {
      @Override
      public String getValue(BioParamValue[] object) {
        return object[0].label();
      }      
    };   
    table.addColumn(labelCol, title);
    table.addColumnStyleName(0, "sampleDetailTitleColumn");
    
    TextCell tc = new TextCell();
    for (int i = 1; i < samples.length + 1; ++i) {
      String name = samples[i - 1].id();
      BioParamColumn bpc = new BioParamColumn(tc, i - 1);
      String borderStyle = i == 1 ? "darkBorderLeft" : "lightBorderLeft";
      bpc.setCellStyleNames(borderStyle);
      String displayTitle = abbreviate(name);      
      SafeHtmlHeader header = new SafeHtmlHeader(Utils.tooltipSpan(name, displayTitle));
      header.setHeaderStyleNames(borderStyle);
      table.addColumn(bpc, header);
      table.addColumnStyleName(i, "sampleDetailDataColumn");
    }
    table.setWidth((15 + 9 * samples.length) + "em", true);
  }
  
  private static String abbreviate(String sampleId) {
    if (sampleId.length() <= 14) {
      return sampleId;
    } else {
      int l = sampleId.length();
      return sampleId.substring(0, 5) + "..." + sampleId.substring(l - 5, l);
    }
  }

  /**
   * For a single parameter, make a row containing its value for all the samples.
   * 
   * @return The row, or null if no samples had a display value for the parameter.
   */
  private @Nullable BioParamValue[] makeAnnotItem(int i, Annotation[] as) {
    BioParamValue[] item = new BioParamValue[samples.length];

    boolean hasDisplayValue = false;
    for (int j = 0; j < as.length && j < samples.length; ++j) {
      item[j] = as[j].getAnnotations().get(i);
      if (item[j].displayValue() != null) {
        hasDisplayValue = true;
      }
    }
    if (hasDisplayValue) {
      return item;
    } else {
      return null;
    }
  }
  
  void setData(HasSamples<Sample> c, Annotation[] annotations) {
    setupColumns(c);
    if (annotations.length > 0) {
      List<BioParamValue[]> processed = new ArrayList<BioParamValue[]>();
      Annotation a = annotations[0];
      final int numEntries = a.getAnnotations().size();
      for (int i = 0; i < numEntries; i++) {
        BioParamValue value = a.getAnnotations().get(i);
        String sec = value.section();
        if (!isSection || (sec == null && title.equals(DEFAULT_SECTION_TITLE))
            || (sec != null && sec.equals(title))) {

          BioParamValue[] row = makeAnnotItem(i, annotations);
          if (row != null) {
            processed.add(row);
          }
        }
      }
      Collections.sort(processed, new Comparator<BioParamValue[]>() {
        @Override
        public int compare(BioParamValue[] o1, BioParamValue[] o2) {
          if (o1 == null || o2 == null || o1.length < 1 || o2.length < 1) {
            return 0;
          }
          return o1[0].label().compareTo(o2[0].label());
        }        
      });
      table.setRowData(processed);
    }
    if (delegate != null) {
      delegate.sampleDetailTableFinishedSettingData();
    }
  }
}
