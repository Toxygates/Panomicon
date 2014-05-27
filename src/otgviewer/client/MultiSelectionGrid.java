package otgviewer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.SelectionTDGrid.UnitListener;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import otgviewer.shared.BUnit;
import otgviewer.shared.DataFilter;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A SelectionTDGrid with multiple sections, one for each data filter. 
 * Dispatches compound and filter change signals to subgrids appropriately.
 */
public class MultiSelectionGrid extends DataListenerWidget implements SelectionTDGrid.UnitListener {

	private SelectionTDGrid currentGrid;
	private Map<DataFilter, SelectionTDGrid> sections = new HashMap<DataFilter, SelectionTDGrid>();
	private UnitListener listener;
	private VerticalPanel vp;
	private final Screen scr;
	
	public MultiSelectionGrid(Screen scr) {		
		vp = new VerticalPanel();
		initWidget(vp);
		this.scr = scr;			
	}
	
	private SelectionTDGrid findOrCreateSection(Screen scr, DataFilter filter) {
		SelectionTDGrid g = sections.get(filter);
		if (g == null) {
			g = new SelectionTDGrid(scr, this);			
			g.dataFilterChanged(filter);
			sections.put(filter, g);
			g.compoundsChanged(chosenCompounds);
			
			Label l = new Label(filter.toString());
			l.setStylePrimaryName("heavyEmphasized");
			vp.add(l);
			vp.add(g); 
		}
		return g;
	}

	@Override
	public void unitsChanged(DataListenerWidget sender, List<BUnit> units) {
		if (listener != null) {
			listener.unitsChanged(this, fullSelection(true));
		} 		
	}
	
	List<BUnit> fullSelection(boolean treatedOnly) {
		List<BUnit> r = new ArrayList<BUnit>();
		for (SelectionTDGrid g: sections.values()) {
			r.addAll(g.getSelectedUnits(treatedOnly));
		}
		return r;
	}
	
	void setAll(boolean state) {
		for (SelectionTDGrid g: sections.values()) {
			g.setAll(false); 
		}
		if (state == false) {
			clearEmptyGrids();
		}
	}

	@Override
	public void dataFilterChanged(DataFilter filter) {
		SelectionTDGrid g = findOrCreateSection(scr, filter);		
		currentGrid = g;
		clearEmptyGrids();
	}

	@Override
	public void compoundsChanged(List<String> compounds) {
		if (currentGrid != null) {
			currentGrid.compoundsChanged(compounds);
		}
	}	
	
	List<String> compoundsFor(DataFilter filter) {
		SelectionTDGrid g = findOrCreateSection(scr, filter);
		return g.chosenCompounds;
	}
	
	void setSelection(BUnit[] selection) {
		for (SelectionTDGrid g: sections.values()) {
			g.setAll(false);
		}
		for (BUnit u: selection) {
			DataFilter df = new DataFilter(u.getCellType(), u.getOrgan(),
					 u.getRepeatType(), u.getOrganism());
			SelectionTDGrid g = findOrCreateSection(scr, df);
			g.setSelected(u, true);			
		}
		clearEmptyGrids();
	}
	
	private void clearEmptyGrids() {
		int count = vp.getWidgetCount();
		for (int i = 1; i < count; i += 2) {
			SelectionTDGrid tg = (SelectionTDGrid) vp.getWidget(i);
			if (tg != currentGrid && tg.getSelectedUnits(true).size() == 0) {				
				vp.remove(i);
				vp.remove(i - 1);
				sections.remove(tg.chosenDataFilter);
				clearEmptyGrids();
				// TODO not the best flow logic
				return;
			}
		}
	}
}
