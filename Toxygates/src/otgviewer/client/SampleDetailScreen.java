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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.sample.DataColumn;
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleColumn;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This screen displays detailed information about a sample or a set of samples,
 * i.e. experimental conditions, histopathological data, blood composition.
 * The samples that can be displayed are the currently configured groups.
 * In addition, a single custom group of samples can be passed to this screen 
 * (the "custom column") to make it display samples that are not in the configured groups.
 * @author johan
 *
 */
public class SampleDetailScreen extends Screen {
	
	public static final String key = "ad";
	
	private SampleDetailTable experimentTable = new SampleDetailTable(this, "Experiment detail");
	private SampleDetailTable biologicalTable = new SampleDetailTable(this, "Biological detail");
	
	private ListBox columnList = new ListBox();

	AnnotationTDGrid atd = new AnnotationTDGrid(this);
	
	private SampleClass lastClass;	
	private List<Group> lastColumns;
	private SampleColumn lastCustomColumn;
	
	private HorizontalPanel tools;
	
	public SampleDetailScreen(ScreenManager man) {
		super("Sample details", key, true, man);						
		this.addListener(atd);
		mkTools();
	}
	
	@Override
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		if (visible && !columns.equals(lastColumns)) {
			updateColumnList();
		}
	}
	
	private void updateColumnList() {
		columnList.clear();
		DataSchema schema = schema();
		if (chosenColumns.size() > 0) {
			for (DataColumn<?> c : chosenColumns) {
				columnList.addItem(c.getShortTitle(schema));
			}
		}
		if (chosenCustomColumn != null) {
			columnList.addItem(chosenCustomColumn.getShortTitle(schema));
			columnList.setSelectedIndex(columnList.getItemCount() - 1);
		} else {
			columnList.setSelectedIndex(0);
		}
	}

	@Override
	public void show() {
		super.show();
		if (visible
				&& (lastClass == null || !lastClass.equals(chosenSampleClass)
						|| lastColumns == null
						|| !chosenColumns.equals(lastColumns) || chosenCustomColumn != null)
						|| chosenCustomColumn != null && (lastCustomColumn == null || !lastCustomColumn
								.equals(chosenCustomColumn))) {
			updateColumnList();			
			displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			
			lastClass = chosenSampleClass;			
			lastColumns = chosenColumns;						
			lastCustomColumn = chosenCustomColumn;
		}
	}

	@Override
	public void customColumnChanged(SampleColumn customColumn) {
		super.customColumnChanged(customColumn);		
		if (visible) {
			updateColumnList();
			StorageParser p = getParser(this);
			if (p != null) {
				// consume the data so it doesn't turn up again.
				storeCustomColumn(p, null); 
			}
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
				Set<String> compounds = new HashSet<String>();
				for (SampleColumn d: chosenColumns) {
					compounds.addAll(SampleClass.getMajors(schema(), d));
				}
				List<String> compounds_ = new ArrayList<String>(compounds);
				atd.compoundsChanged(compounds_);
				Utils.displayInPopup("Visualisation", atd, DialogPosition.Center);								
			}
		}));
		
		columnList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent ce) {
				displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			}
		});		
	}
	
	public Widget content() {		
		VerticalPanel vp = Utils.mkVerticalPanel(false, experimentTable, biologicalTable);

		HorizontalPanel hp = Utils.mkWidePanel(); //to make it centered
		hp.add(vp);
		return new ScrollPanel(hp);				
	}
	
	
	private void setDisplayColumn(SampleColumn c) {
		experimentTable.loadFrom(c, false, 0, 23);
		biologicalTable.loadFrom(c, false, 23, -1);
		SampleClass sc = c.getSamples()[0].sampleClass().asMacroClass(manager.schema());
		atd.sampleClassChanged(sc);
	}
	
	private void displayWith(String column) {
		DataSchema schema = schema();
		if (chosenCustomColumn != null && column.equals(chosenCustomColumn.getShortTitle(schema))) {
			setDisplayColumn(chosenCustomColumn);
			return;
		} else {
			for (SampleColumn c : chosenColumns) {
				if (c.getShortTitle(schema).equals(column)) {
					setDisplayColumn(c);
					return;
				}
			}
		}
		Window.alert("Error: no display column selected.");
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
}
