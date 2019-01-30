/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otg.viewer.client.charts.google;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;

import otg.viewer.client.charts.ChartSample;
import otg.viewer.client.charts.Dataset;

import com.google.gwt.visualization.client.DataTable;

import t.common.shared.SharedUtils;
import t.common.shared.sample.SampleClassUtils;
import t.model.SampleClass;
import t.viewer.client.storage.StorageProvider;

/**
 * A Dataset backed by a DataTable from Google Visualizations.
 */
public class GDTDataset extends Dataset<GDTData> {
  private StorageProvider storage;
  static final String SAMPLE_ID_PROP = "sample";

  GDTDataset(List<ChartSample> samples, String[] categories, boolean categoriesAreMins,
      StorageProvider storage) {
    super(samples, categories, categoriesAreMins);
    this.storage = storage;
  }

  @Override
  public GDTData makeData(SampleClass filter, @Nullable String probe) {
    DataTable dataTable = DataTable.create();
    dataTable.addColumn(ColumnType.STRING, "Time");

    for (int i = 0; i < categories.length; ++i) {
      dataTable.addRow();
      dataTable.setValue(i, 0, categories[i]);
    }

    List<ChartSample> fsamples = new ArrayList<ChartSample>();
    for (ChartSample s : samples) {
      if ((probe == null || s.probe().equals(probe))
          && SampleClassUtils.strictCompatible(filter, s)) {
        fsamples.add(s);
      }
    }

    GDTData gdtData = new GDTData(dataTable);
    makeColumns(gdtData, fsamples);
    return gdtData;
  }

  @Override
  protected void makeColumns(GDTData gdt, List<ChartSample> samples) {
    int colCount = 0;
    int[] valCount = new int[categories.length];
    DataTable dataTable = gdt.data();

    for (ChartSample sample : samples) {
      int categoryIndex = SharedUtils.indexOf(categories, categoryForSample(sample));
      if (categoryIndex != -1) {
        if (colCount < valCount[categoryIndex] + 1) {
          dataTable.addColumn(ColumnType.NUMBER);
          addStyleColumn(dataTable);
          colCount++;
        }

        final int col = valCount[categoryIndex] * 2 + 1;
        dataTable.setValue(categoryIndex, col, sample.value());
        if (sample.sample() != null) {
          dataTable.setProperty(categoryIndex, col, SAMPLE_ID_PROP,
              storage.samplePacker.pack(sample.sample()));
        }
        dataTable.setFormattedValue(categoryIndex, col, sample.formattedValue());
        String style = "fill-color:" + sample.color() + "; stroke-width:1px; ";

        dataTable.setValue(categoryIndex, col + 1, style);
        valCount[categoryIndex]++;
      }
    }
  }

  protected native void addStyleColumn(DataTable dt) /*-{
		dt.addColumn({
			type : 'string',
			role : 'style'
		});
  }-*/;
}
