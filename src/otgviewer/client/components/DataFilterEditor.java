package otgviewer.client.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.shared.DataFilter;
import otgviewer.shared.SampleClass;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

public class DataFilterEditor extends DataListenerWidget {
	final List<SampleClass> sampleClasses;
	
	final SCListBox organismSelector, organSelector, cellTypeSelector, repeatTypeSelector;
	
	class SCListBox extends ListBox {
		void setItems(List<String> items) {
			clear();
			for (String i: items) {
				addItem(i);
			}
			if (items.size() > 0) {
				setSelectedIndex(0);
			}
		}
		
		void trySelect(String item) {
			for (int i = 0; i < getItemCount(); i++) {
				if (getItemText(i).equals(item)) {
					setSelectedIndex(i);
					return;
				}
			}
		}
		
		void setItemsFrom(List<SampleClass> scs, String key) {
			setItems(SampleClass.collect(scs, key));		
		}
				
		String getSelected() {
			int i = getSelectedIndex();
			if (i != -1) {
				return getItemText(i);
			} else {
				return null;
			}
		}		
	}

	public DataFilterEditor(SampleClass[] sampleClasses) {
		HorizontalPanel hp = new HorizontalPanel();
		initWidget(hp);
		this.sampleClasses = Arrays.asList(sampleClasses);
		
		organismSelector = new SCListBox();
		organSelector = new SCListBox();
		cellTypeSelector = new SCListBox();
		repeatTypeSelector = new SCListBox();
		
		hp.add(organismSelector);
		hp.add(organSelector);
		hp.add(cellTypeSelector);
		hp.add(repeatTypeSelector);
		
		List<String> organisms = SampleClass.collect(this.sampleClasses, "organism");
		organismSelector.setItems(organisms);
		
		organismSelector.addChangeHandler(new ChangeHandler() {			
			@Override
			public void onChange(ChangeEvent event) {
				String sel = organismSelector.getSelected();
				if (sel != null) {
					List<SampleClass> selected = constrain1(sel);
					organSelector.setItemsFrom(selected, "organ");
				}				
			}
		});
		
		organSelector.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				String sel1 = organismSelector.getSelected();
				String sel2 = organSelector.getSelected();
				if (sel1 != null && sel2 != null) {
					List<SampleClass> selected = constrain2(sel1, sel2);
					cellTypeSelector.setItemsFrom(selected, "cellType");
				}
			}
		});

		cellTypeSelector.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				String sel1 = organismSelector.getSelected();
				String sel2 = organSelector.getSelected();
				String sel3 = cellTypeSelector.getSelected();
				if (sel1 != null && sel2 != null && sel3 != null) {
					List<SampleClass> selected = constrain3(sel1, sel2, sel3);
					repeatTypeSelector.setItemsFrom(selected, "repeatType");
				}
			}
		});

		repeatTypeSelector.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				String sel1 = organismSelector.getSelected();
				String sel2 = organSelector.getSelected();
				String sel3 = cellTypeSelector.getSelected();
				String sel4 = repeatTypeSelector.getSelected();
				if (sel1 != null && sel2 != null && sel3 != null && sel4 != null) {
					Map<String, String> sc = new HashMap<String, String>();
					sc.put("organism", sel1);
					sc.put("organ", sel2);
					sc.put("cellType", sel3);
					sc.put("repeatType", sel4);
					SampleClass r = new SampleClass(sc);
					//TODO send out
				}
			}
		});
	}
	
	public List<SampleClass> constrain1(String organism) {
		return SampleClass.filter(sampleClasses, "organism", organism);
	}
	
	public List<SampleClass> constrain2(String organism, String organ) {
		return SampleClass.filter(constrain1(organism), "organ", organ);
	}
	
	public List<SampleClass> constrain3(String organism, String organ, String testType) {
		return SampleClass.filter(constrain2(organism, organ), "testType", testType);
	}

	@Override
	public void dataFilterChanged(DataFilter filter) {
		//do NOT call superclass method. Prevent signal from being passed on.
		chosenDataFilter = filter;
		
		//TODO update
//		organismSelector.setSelected(filter.organism);
//		organSelector.setSelected(filter.organ);
//		cellTypeSelector.setSelected(filter.cellType);
//		repeatTypeSelector.setSelected(filter.repeatType);
	}
}
