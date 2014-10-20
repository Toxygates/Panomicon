package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.SelectionTDGrid.UnitListener;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import t.common.shared.SampleClass;
import t.common.shared.Unit;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A SelectionTDGrid with multiple sections, one for each data filter. 
 * Dispatches compound and filter change signals to subgrids appropriately.
 */
public class MultiSelectionGrid extends DataListenerWidget implements SelectionTDGrid.UnitListener {

	private SelectionTDGrid currentGrid;
	private Map<SampleClass, SelectionTDGrid> sections = 
			new HashMap<SampleClass, SelectionTDGrid>();
	private UnitListener listener;
	private VerticalPanel vp;
	private final Screen scr;
	protected final Logger logger = Utils.getLogger("group");
	
	public MultiSelectionGrid(Screen scr, @Nullable SelectionTDGrid.UnitListener listener) {		
		vp = new VerticalPanel();
		initWidget(vp);
		this.scr = scr;
		this.listener = listener;
	}
	
	private SelectionTDGrid findOrCreateSection(Screen scr, SampleClass sc) {
		SelectionTDGrid g = sections.get(sc);
		logger.info("Find or create for " + sc.toString());
		if (g == null) {
			g = new SelectionTDGrid(scr, this);			
			g.sampleClassChanged(sc);
			sections.put(sc, g);
			g.compoundsChanged(chosenCompounds);			
			Label l = new Label(sc.label(scr.schema()));
			l.setStylePrimaryName("heavyEmphasized");
			vp.add(l);
			vp.add(g); 
		}
		return g;
	}

	@Override
	public void unitsChanged(DataListenerWidget sender, List<Unit> units) {
		if (listener != null) {
			listener.unitsChanged(this, fullSelection(true));
		} 		
	}
	
	List<Unit> fullSelection(boolean treatedOnly) {
		List<Unit> r = new ArrayList<Unit>();
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
	public void sampleClassChanged(SampleClass sc) {		
		SelectionTDGrid g = findOrCreateSection(scr, sc);
		if (g != currentGrid) {
			logger.info("SC change " + sc.toString());
			currentGrid = g;
			clearEmptyGrids();
		}
	}

	@Override
	public void compoundsChanged(List<String> compounds) {
		if (currentGrid != null) {
			currentGrid.compoundsChanged(compounds);
		}
	}	
	
	List<String> compoundsFor(SampleClass sc) {
		SelectionTDGrid g = findOrCreateSection(scr, sc);
		return g.chosenCompounds;
	}
	
	void setSelection(Unit[] selection) {
		logger.info("Set selection: " + selection.length + " units");
		for (SelectionTDGrid g: sections.values()) {
			g.setAll(false);
		}
		Set<String> compounds = Unit.collect(Arrays.asList(selection), 
				scr.schema().majorParameter());
		for (Unit u: selection) {						
			SampleClass sc = u.asMacroClass(scr.schema());
			SelectionTDGrid g = findOrCreateSection(scr, sc);
			g.compoundsChanged(new java.util.ArrayList<String>(compounds));
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
				sections.remove(tg.chosenSampleClass);
				clearEmptyGrids();
				// TODO not the best flow logic
				return;
			}
		}
	}
}
