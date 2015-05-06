package t.db

import t.platform.Probe

/**
 * The extended microarray DB is a microarray DB that also has p-values and
 * custom PA-calls for each sample group.
 */
trait ExtMatrixDBReader extends MatrixDBReader[PExprValue] {
  def emptyValue(probe: String) = PExprValue(0.0, Double.NaN, 'A', Probe(probe))  
}

trait ExtMatrixDBWriter extends MatrixDBWriter[PExprValue]

trait ExtMatrixDB extends MatrixDB[PExprValue, PExprValue] 
with ExtMatrixDBReader with ExtMatrixDBWriter 
