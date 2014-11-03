package otgviewer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.SelectionTDGrid.UnitListener;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
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
	
	private Unit[] expectedSelection = new Unit[]{}; //waiting for units (grid count)
	
	private SelectionTDGrid findOrCreateSection(Screen scr, 
			SampleClass sc, boolean noCompounds) {
		SelectionTDGrid g = sections.get(sc);
		logger.info("Find or create for " + sc.toString());
		if (g == null) {
			g = new SelectionTDGrid(scr, this);			
			g.sampleClassChanged(sc);
			sections.put(sc, g);
			if (!noCompounds) {
				g.compoundsChanged(chosenCompounds);
			}
			Label l = new Label(sc.label(scr.schema()));
			l.setStylePrimaryName("heavyEmphasized");
			vp.add(l);
			vp.add(g); 			
		}
		return g;
	}

	@Override
	public void unitsChanged(DataListenerWidget sender, List<Unit> units) {
		List<Unit> fullSel = fullSelection(true);
		List<Unit> fullSelAll = fullSelection(false);
		if (listener != null) {
			listener.unitsChanged(this, fullSel);
		} 		
		logger.info("Size: " + fullSelAll.size() + " expected: " + expectedSelection.length);
		if (fullSelAll.size() == expectedSelection.length && expectedSelection.length > 0) {
			clearEmptySections();
			expectedSelection = new Unit[] {};
		}			
	}
	
	public void availableUnitsChanged(DataListenerWidget sender, List<Pair<Unit, Unit>> units) {
		List<Pair<Unit, Unit>> fullAvailability = allAvailable();
		if (listener != null) {
			listener.availableUnitsChanged(this, fullAvailability);
		} 		
	}
	
	List<Unit> fullSelection(boolean treatedOnly) {
		List<Unit> r = new ArrayList<Unit>();
		for (SelectionTDGrid g: sections.values()) {
			r.addAll(g.getSelectedUnits(treatedOnly));
		}
		return r;
	}
	
	List<Pair<Unit, Unit>> allAvailable() {
		List<Pair<Unit, Unit>> r = new ArrayList<Pair<Unit, Unit>>();
		for (SelectionTDGrid g: sections.values()) {
			r.addAll(g.getAvailableUnits());
		}
		return r;
	}
	
	void setAll(boolean state) {
		for (SelectionTDGrid g: sections.values()) {
			g.setAll(false); 
		}
		if (state == false) {
			clearEmptySections();
		}
	}

	@Override
	public void sampleClassChanged(SampleClass sc) {		
		SelectionTDGrid g = findOrCreateSection(scr, sc, false);
		if (g != currentGrid) {
			logger.info("SC change " + sc.toString());
			currentGrid = g;
			clearEmptySections();
		}
	}

	@Override
	public void compoundsChanged(List<String> compounds) {
		if (currentGrid != null) {
			currentGrid.compoundsChanged(compounds);
		}
	}	
	
	List<String> compoundsFor(SampleClass sc) {
		SelectionTDGrid g = findOrCreateSection(scr, sc, false);
		return g.chosenCompounds;
	}
	
	void setSelection(Unit[] selection) {
		logger.info("Set selection: " + selection.length + " units");
		final DataSchema schema = scr.schema();

		for (SelectionTDGrid g: sections.values()) {
			g.setAll(false);
		}
		expectedSelection = selection;
		
		
		final String majorParam = scr.schema().majorParameter(); 
		Map<SampleClass, Set<String>> lcompounds = new HashMap<SampleClass, Set<String>>();
		for (Unit u: selection) {
			SampleClass sc = u.asMacroClass(schema);
			if (!lcompounds.containsKey(sc)) {
				lcompounds.put(sc, new HashSet<String>());
			}
			String majorVal = u.get(majorParam);
			if (!schema.isMajorParamSharedControl(majorVal)) {
				lcompounds.get(sc).add(majorVal);
			}
		}
		
		for (SampleClass sc: lcompounds.keySet()) {
			List<String> compounds = new ArrayList<String>(lcompounds.get(sc));
			Collections.sort(compounds);
			SelectionTDGrid g = findOrCreateSection(scr, sc, true);
			g.compoundsChanged(compounds, selection);
		}		
	}
	
	private void clearEmptySections() {
		int count = vp.getWidgetCount();
		for (int i = 1; i < count; i += 2) {
			SelectionTDGrid tg = (SelectionTDGrid) vp.getWidget(i);
			if (tg != currentGrid && tg.getSelectedUnits(true).size() == 0) {				
				vp.remove(i);
				vp.remove(i - 1);
				sections.remove(tg.chosenSampleClass);
				clearEmptySections();
				// TODO not the best flow logic
				return;
			}
		}
	}
}
