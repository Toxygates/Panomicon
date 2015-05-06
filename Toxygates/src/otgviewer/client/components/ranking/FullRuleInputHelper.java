package otgviewer.client.components.ranking;

import java.util.List;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.shared.RankRule;
import otgviewer.shared.RuleType;
import t.common.shared.SampleClass;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class FullRuleInputHelper extends RuleInputHelper {

	final ListBox refDose = new ListBox();
	final TextBox syntheticCurveText = new TextBox();
	final ListBox refCompound = new ListBox();

	public FullRuleInputHelper(CompoundRanker _ranker, RankRule r,
			boolean lastRule) {
		super(_ranker, r, lastRule);	
		
		syntheticCurveText.setWidth("5em");
		syntheticCurveText.setEnabled(false);
		
		refCompound.setStylePrimaryName("colored");
		refCompound.setEnabled(false);
		for (String c : ranker.availableCompounds) {
			refCompound.addItem(c);
		}

		refCompound.addChangeHandler(new ChangeHandler() {				
			@Override
			public void onChange(ChangeEvent event) {
				updateDoses();					
			}
		});
				
		refDose.setStylePrimaryName("colored");
		refDose.setEnabled(false);
	}

	final static int REQUIRED_COLUMNS = 6;

	protected RuleType[] ruleTypes() {
		return RuleType.values();
	}
	
	@Override
	void populate(Grid grid, int row) {
		super.populate(grid, row);
		grid.setWidget(row + 1, 3, syntheticCurveText);
		grid.setWidget(row + 1, 4, refCompound);
		grid.setWidget(row + 1, 5, refDose);
	}
	
	void updateDoses() {
		refDose.clear();
		int selIndex = refCompound.getSelectedIndex();
		if (selIndex == -1) {
			return;
		}
		final String selCompound = refCompound.getItemText(selIndex);
		SampleClass sc = ranker.chosenSampleClass.copy();
		sc.put("compound_name", selCompound);
		
		ranker.sparqlService.parameterValues(sc, "dose_level",
				new PendingAsyncCallback<String[]>(ranker.selector, 
						"Unable to retrieve dose levels.") {
					
			@Override
			public void handleSuccess(String[] result) {

				for (String i: result) {
					if (!i.equals("Control")) {
						refDose.addItem(i);
					}
				}							
			}
			
		});			
	}
	
	protected void rankTypeChanged() {
		RuleType rt = rankType.value();
		switch (rt) {
		case Synthetic:
			syntheticCurveText.setEnabled(true);
			refCompound.setEnabled(false);
			refDose.setEnabled(false);
			break;
		case ReferenceCompound:
			syntheticCurveText.setEnabled(false);
			refCompound.setEnabled(true);
			if (refCompound.getItemCount() > 0) {
				refCompound.setSelectedIndex(0);
				updateDoses();
			}
			refDose.setEnabled(true);
			break;
		default:
			syntheticCurveText.setEnabled(false);
			refCompound.setEnabled(false);
			refDose.setEnabled(false);
			break;
		}
	}
	
	@Override
	RankRule getRule() throws RankRuleException {
		RuleType rt = rankType.value();
		String probe = probeText.getText();
		switch (rt) {
		case Synthetic: {
			double[] data;
			String[] ss = syntheticCurveText.getText()
					.split(" ");
			RankRule r = new RankRule(rt, probe);			
			SampleClass sc = ranker.chosenSampleClass;
			int expectedPoints = ranker.schema.numDataPointsInSeries(sc);
			
			if (ss.length != expectedPoints) {
				throw new RankRuleException("Please supply " + expectedPoints +
						" space-separated values as the synthetic curve." +
						" (Example: 1 -2 ...)");
			} else {							
				data = new double[expectedPoints];														
				for (int j = 0; j < ss.length; ++j) {
					data[j] = Double.valueOf(ss[j]);
				}
				r.setData(data);
				return r;				
			}
		}
		case ReferenceCompound: {
			String cmp = refCompound
					.getItemText(refCompound
							.getSelectedIndex());
			RankRule r = new RankRule(rt, probe);
			r.setCompound(cmp);
			r.setDose(refDose.getItemText(refDose
					.getSelectedIndex()));
			return r;			
		}
		default:
			return super.getRule();				
		}		
	} 
	
	@Override 
	void reset() {
		super.reset();
		refCompound.setSelectedIndex(0);
		syntheticCurveText.setText("");
	}	

	void sampleClassChanged(SampleClass sc) {
		refCompound.clear();
		refDose.clear();
	}
	
	void availableCompoundsChanged(List<String> compounds) {
		refCompound.clear();
		refDose.clear();
		for (String c : compounds) {
			refCompound.addItem(c);				
		}
		updateDoses();
	}
}
