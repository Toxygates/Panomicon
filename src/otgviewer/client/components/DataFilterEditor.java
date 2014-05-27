package otgviewer.client.components;

import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class DataFilterEditor extends DataListenerWidget {	
	EnumSelector<Organism> organismSelector = new EnumSelector<Organism>() {
		public Organism[] values() { return Organism.values(); }
		
		@Override
		protected void onValueChange(Organism x) {
			DataFilter df = new DataFilter(chosenDataFilter);
			df.organism = x;
			changeDataFilter(df);
		}
	};		
	
	EnumSelector<Organ> organSelector = new EnumSelector<Organ>() {
		public Organ[] values() { return Organ.values(); }
		
		@Override
		protected void onValueChange(Organ x) {
			DataFilter df = new DataFilter(chosenDataFilter);
			df.organ = x;
			changeDataFilter(df);
		}
	};	
	
	EnumSelector<CellType> cellTypeSelector = new EnumSelector<CellType>() {
		public CellType[] values() { return CellType.values(); }
		
		@Override
		protected void onValueChange(CellType x) {
			DataFilter df = new DataFilter(chosenDataFilter);
			df.cellType = x;
			changeDataFilter(df);
		}
	};		
	
	EnumSelector<RepeatType> repeatTypeSelector = new EnumSelector<RepeatType>() {
		public RepeatType[] values() { return RepeatType.values(); }
		
		@Override
		protected void onValueChange(RepeatType x) {
			DataFilter df = new DataFilter(chosenDataFilter);			
			df.repeatType = x;
			changeDataFilter(df);
		}
	};	
	
	public DataFilterEditor() {
		HorizontalPanel hp = new HorizontalPanel();
		initWidget(hp);
		hp.add(organismSelector);
		hp.add(organSelector);
		hp.add(cellTypeSelector);
		hp.add(repeatTypeSelector);
	}

	@Override
	public void dataFilterChanged(DataFilter filter) {
		//do NOT call superclass method. Prevent signal from being passed on.
		chosenDataFilter = filter; 
		organismSelector.setSelected(filter.organism);
		organSelector.setSelected(filter.organ);
		cellTypeSelector.setSelected(filter.cellType);
		repeatTypeSelector.setSelected(filter.repeatType);
	}
}
