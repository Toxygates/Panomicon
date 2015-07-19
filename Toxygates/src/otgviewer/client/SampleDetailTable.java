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

package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.OTGSample;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * A table that displays sample annotations for a small set of samples.
 */
public class SampleDetailTable extends Composite {
	private CellTable<String[]> table;
	private OTGSample[] barcodes;
	private HasSamples<OTGSample> displayColumn;
	private SparqlServiceAsync sparqlService;
	private final String title;
	private final DataListenerWidget waitListener;
	
	public SampleDetailTable(Screen screen, String title) {
		this.title = title;
		this.waitListener = screen;
		sparqlService = screen.sparqlService();
		table = new CellTable<String[]>();
		initWidget(table);
		table.setWidth("100%", true); //use fixed layout so we can control column width explicitly
		table.setSelectionModel(new NoSelectionModel<String[]>());
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
	}
	
	public void loadFrom(HasSamples<OTGSample> c, boolean importantOnly, 
			final int rangeStart, final int rangeEnd) {
		if (Arrays.equals(barcodes, c.getSamples())) {
			return;
		}
		barcodes = c.getSamples();
		displayColumn = c;
		while(table.getColumnCount() > 0) {
			table.removeColumn(0);
		}
		makeColumn(0, title, "15em");
		for (int i = 1; i < barcodes.length + 1; ++i) {
			// TODO
			String name = barcodes[i - 1].id().substring(2); //remove leading 00					
			makeColumn(i, name, "9em");
		}
		table.setWidth((15 + 9 * barcodes.length) + "em", true);
		
		sparqlService.annotations(displayColumn, importantOnly,
				new PendingAsyncCallback<Annotation[]>(waitListener) {
					public void handleFailure(Throwable caught) {
						Window.alert("Unable to get array annotations.");
					}

					public void handleSuccess(Annotation[] as) {
						List<Annotation> useAnnots = new ArrayList<Annotation>();
						for (int i = 0; i < as.length; ++i) {								
							useAnnots.add(as[i]);
						}
						setData(useAnnots.toArray(new Annotation[0]), rangeStart, rangeEnd);
					}
				});
	}
	
	private TextColumn<String[]> makeColumn(final int idx, String title, String width) {
		TextColumn<String[]> col = new TextColumn<String[]>() {
			public String getValue(String[] x) {
				if (x.length > idx) {
					return x[idx];
				} else {
					return "";
				}
			}
		};		
		
		SafeHtml hhtml = SafeHtmlUtils.fromSafeConstant(
				"<span title=\"" + title + "\">" + title + "</span>");
		SafeHtmlHeader header = new SafeHtmlHeader(hhtml);
		table.addColumn(col, header);		
		table.setColumnWidth(col, width);				
		return col;
	}

	private String[] makeAnnotItem(int i, Annotation[] as) {
		String[] item = new String[barcodes.length + 1];
		item[0] = as[0].getEntries().get(i).description;
		
		// TODO why is as.length sometimes > barcodes.length?
		
		for (int j = 0; j < as.length && j < barcodes.length; ++j) {					
			item[j + 1] = as[j].getEntries().get(i).value;						
		}
		return item;
	}
	
	/**
	 * Set row-major data to display.
	 */
	void setData(Annotation[] annotations, int rangeStart, int rangeEnd) {
		if (annotations.length > 0) {
			List<String[]> processed = new ArrayList<String[]>();
			final int numEntries = annotations[0].getEntries().size();
			for (int i = rangeStart; i < numEntries && (rangeEnd == -1 || i < rangeEnd); ++i) {
				processed.add(makeAnnotItem(i, annotations));
			}
			table.setRowData(processed);
		}
	}
}
