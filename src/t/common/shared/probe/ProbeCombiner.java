package t.common.shared.probe;

import t.common.shared.sample.ExpressionRow;

/**
 * A method for combining simple probes into a single MultiProbe.
 * @author johan
 */
public abstract class ProbeCombiner {
	abstract ExpressionRow combine(ExpressionRow[] rows);	
}
