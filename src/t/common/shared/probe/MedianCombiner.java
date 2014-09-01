package t.common.shared.probe;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.ExpressionValue;

public class MedianCombiner extends ProbeCombiner {

	/**
	 * Combine a set of expression rows corresponding to one MultiProbe.
	 * The rows must have the same number of columns.
	 * @param probe
	 * @param rows
	 * @return
	 */
	ExpressionRow combine(ExpressionRow[] rows) {
		int nc = rows[0].getColumns();
		ExpressionValue[] rv = new ExpressionValue[nc];
		
		for (int c = 0; c < nc; c++) {
			ExpressionValue[] combine = new ExpressionValue[rows.length];
			for (int r = 0; r < rows.length; r++) {
				combine[r] = rows[r].getValue(c);				
			}
			rv[c] = combine(combine);
		}
		
		StringBuilder titles = new StringBuilder();
		StringBuilder probes = new StringBuilder();
		Set<String> geneIds = new HashSet<String>();
		Set<String> geneSyms = new HashSet<String>();
		for (int r = 0; r < rows.length; r++) {
			titles.append(rows[r].getProbe()).append(" / ");
			probes.append(rows[r].getProbe()).append(" / ");
			Collections.addAll(geneIds, rows[r].getGeneIds());
			Collections.addAll(geneSyms, rows[r].getGeneSyms());			
		}
		
		//TODO combine gene syms, etc, here
		return new ExpressionRow(probes.toString(), titles.toString(),
				geneIds.toArray(new String[0]), geneSyms.toArray(new String[0]),
				rv);				
	}

	/**
	 * Combine a non-empty array of expression values.
	 * @param values
	 * @return
	 */
	ExpressionValue combine(ExpressionValue[] values) {
		//NB, Arrays.copyOf is not supported on GWT client
		ExpressionValue[]nvs = Arrays.copyOf(values, values.length);
		Arrays.sort(nvs, new Comparator<ExpressionValue>() {
			@Override
			public int compare(ExpressionValue arg0, ExpressionValue arg1) {
				return Double.compare(arg0.getValue(), arg1.getValue());
			}			
		});
		
		double median = 0;
		if (nvs.length % 2 == 0) {
			median = (nvs[nvs.length / 2 - 1].getValue() + 
					nvs[nvs.length / 2].getValue())/2;
		} else {
			median = nvs[nvs.length / 2].getValue();
		}
		
		double call = 0;
		for (ExpressionValue ev: nvs) {
			char c = ev.getCall();
			switch(c) {
			case 'M':
				call += 1.0;
				break;
			case 'P':
				call += 2.0;
				break;
			}
		}
		long c = Math.round(call / values.length);
		char rc = (c == 2 ? 'P' : (c == 1 ? 'M' : 'A'));
		return new ExpressionValue(median, rc);
	}	
}
