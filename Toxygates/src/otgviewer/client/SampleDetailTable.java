/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.Annotation;
import otgviewer.shared.BioParamValue;
import otgviewer.shared.NumericalBioParamValue;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.table.TooltipColumn;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * A table that displays sample annotations for a small set of samples.
 */

public class SampleDetailTable extends Composite {
  private CellTable<BioParamValue[]> table;
  private Sample[] barcodes;
  private HasSamples<Sample> displayColumn;
  private SampleServiceAsync sampleService;
  private final @Nullable String title;
  private final boolean isSection;
  private final DataListenerWidget waitListener;

  public static final String DEFAULT_SECTION_TITLE = "Sample details";
  
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
        if (nbpv.isAbove()) {
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
  };
  
  public SampleDetailTable(Screen screen, @Nullable String title, boolean isSection) {
    this.title = title != null ? title : DEFAULT_SECTION_TITLE;
    this.isSection = isSection;
    this.waitListener = screen;
    sampleService = screen.manager().sampleService();
    table = new CellTable<BioParamValue[]>();
    initWidget(table);
    table.setWidth("100%", true); // use fixed layout so we can control column width explicitly
    table.setSelectionModel(new NoSelectionModel<BioParamValue[]>());
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
  }

  public @Nullable String sectionTitle() { return title; }
  
  public void loadFrom(final HasSamples<Sample> c, boolean importantOnly) {        
    sampleService.annotations(displayColumn, importantOnly, new PendingAsyncCallback<Annotation[]>(
        waitListener) {
      public void handleFailure(Throwable caught) {
        Window.alert("Unable to get array annotations.");
      }

      public void handleSuccess(Annotation[] as) {
        setData(c, as);
      }
    });
  }
  
  private void setupColumns(HasSamples<Sample> c) {
    barcodes = c.getSamples();
    displayColumn = c;
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
    table.setColumnWidth(labelCol, "15em");
    
    TextCell tc = new TextCell();
    for (int i = 1; i < barcodes.length + 1; ++i) {
      String name = barcodes[i - 1].id();
      BioParamColumn bpc = new BioParamColumn(tc, i - 1);
      table.addColumn(bpc, name);
      table.setColumnWidth(bpc, "10em");
    }
    table.setWidth((15 + 9 * barcodes.length) + "em", true);

  }

  private BioParamValue[] makeAnnotItem(int i, Annotation[] as) {
    BioParamValue[] item = new BioParamValue[barcodes.length];

    for (int j = 0; j < as.length && j < barcodes.length; ++j) {
      item[j] = as[j].getAnnotations().get(i);
    }
    return item;
  }
  
  void setData(HasSamples<Sample> c, Annotation[] annotations) {
    setupColumns(c);
    if (annotations.length > 0) {
      List<BioParamValue[]> processed = new ArrayList<BioParamValue[]>();
      Annotation a = annotations[0];
      final int numEntries = a.getAnnotations().size();
      for (int i = 0; i < numEntries; i++) {
        String sec = a.getAnnotations().get(i).section();
        if (!isSection || (sec == null && title.equals(DEFAULT_SECTION_TITLE))
            || (sec != null && sec.equals(title))) {
          processed.add(makeAnnotItem(i, annotations));
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
  }
}
