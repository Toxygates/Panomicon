package otgviewer.server

import t.common.shared.probe.ProbeMapper
import t.common.shared.probe.ValueMapper
import otgviewer.shared.ManagedMatrixInfo

/**
 * A matrix mapper converts a whole matrix from one domain into
 * a matrix in another domain. 
 * Example: Convert a transcript matrix into a gene matrix.
 * Example: Convert a gene matrix into a protein matrix.
 * 
 * This process changes the number and index keys of the rows, but
 * preserves columns.
 */
class MatrixMapper(val pm: ProbeMapper, val vm: ValueMapper) {
	def convert(from: ExprMatrix): ExprMatrix = {
		val rangeProbes = pm.range
		val fromRowSet = from.rowKeys.toSet
		
		val nrows = rangeProbes.map(r => {
		  val sps = pm.toDomain(r).filter(fromRowSet.contains(_))
		  
		  //pull out e.g. all the rows corresponding to probes (domain)
		  //for gene G1 (range)
		  val domainRows = sps.map(sp => from.row(sp))
		  val cols = domainRows.head.size
		  val nr = (0 until cols).map(c => {
		    val xs = domainRows.map(dr => dr(c))
		    vm.convert(r, xs)
		  })		
		  (nr, RowAnnotation(r))
		})
		
		val cols = (0 until from.columns).map(x => from.columnAt(x))
		
		val annots = nrows.map(_._2)
		ExprMatrix.withRows(nrows.map(_._1), cols, rangeProbes).copyWithAnnotations(annots)
	}
	
	def convert(from: ManagedMatrix): ManagedMatrix = {
	  val ungr = convert(from.rawUngroupedMat)
	  val gr = convert(from.rawGroupedMat)
	  
	  new ManagedMatrix(from.initProbes, convert(from.currentInfo), ungr, gr)
	}
	
	/**
	 * This conversion keeps the columns and column names,
	 * but removes synthetics and filtering options.
	 * TODO synthetics handling needs to be tested
	 */
	def convert(from: ManagedMatrixInfo): ManagedMatrixInfo = {
	  val r = new ManagedMatrixInfo()
	  for (i <- 0 until from.numDataColumns()) {	  
	    r.addColumn(false, from.columnName(i), from.columnHint(i), 
	        from.isUpperFiltering(i), from.columnGroup(i))
	  }
	  r
	}
}