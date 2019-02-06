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

import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;

import otg.viewer.client.charts.DataPoint;
import otg.viewer.client.charts.Dataset;
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

  GDTDataset(List<DataPoint> points, String[] categories, boolean categoriesAreMins,
      StorageProvider storage) {
    super(points, categories, categoriesAreMins);
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

    DataPoint[] passedPoints =
        points.stream().filter(p -> ((probe == null || p.probe().equals(probe))
            && SampleClassUtils.strictCompatible(filter, p))).toArray(DataPoint[]::new);

    GDTData gdtData = new GDTData(dataTable);
    makeColumns(gdtData, passedPoints);
    return gdtData;
  }

  @Override
  protected void makeColumns(GDTData gdt, DataPoint[] points) {
    int colCount = 0;
    int[] valCount = new int[categories.length];
    DataTable dataTable = gdt.data();

    for (DataPoint point : points) {
      int categoryIndex = SharedUtils.indexOf(categories, categoryForPoint(point));
      if (categoryIndex != -1) {
        if (colCount < valCount[categoryIndex] + 1) {
          dataTable.addColumn(ColumnType.NUMBER);
          addStyleColumn(dataTable);
          colCount++;
        }

        final int col = valCount[categoryIndex] * 2 + 1;
        dataTable.setValue(categoryIndex, col, point.value());
        if (point.sample() != null) {
          /*
           * Tag the data point with its sample ID so that we can link to the sample later
           */
          dataTable.setProperty(categoryIndex, col, SAMPLE_ID_PROP,
              storage.samplePacker.pack(point.sample()));
        }
        dataTable.setFormattedValue(categoryIndex, col, point.formattedValue());
        String style = "fill-color:" + point.color() + "; stroke-width:1px; ";

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
