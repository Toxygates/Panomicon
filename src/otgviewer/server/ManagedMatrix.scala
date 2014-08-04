package otgviewer.server

import t.common.shared.sample.ExpressionValue
import otg.ExprValue
import otg.OTGContext
import otg.PExprValue
import otgviewer.server.rpc.Conversions._
import otgviewer.shared.Group
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.Synthetic
import t.db.MatrixDBReader
import otgviewer.shared.OTGSample
import t.db.Sample


/**
 * A server-side ExprMatrix and support logic for
 * sorting, filtering, data loading etc.
 * 
 * Data is loaded when the matrix is constructed.
 * Once loaded, the data can be modified in various ways without further
 * disk access.
 * 
 * A managed matrix is constructed on the basis of some number of 
 * "request columns" but may insert additional columns with extra information.
 * The info object should be used to query what columns have actually been 
 * constructed.
 * 
 */
abstract class ManagedMatrix[E <: ExprValue](requestColumns: Seq[Group],
    val reader: MatrixDBReader[E], initProbes: Array[String], sparseRead: Boolean)
    (implicit val context: OTGContext) {
  
  protected var currentInfo: ManagedMatrixInfo = new ManagedMatrixInfo()
  protected var rawUngroupedMat, rawGroupedMat, currentMat: ExprMatrix = _
  
  protected var _synthetics: Vector[Synthetic] = Vector()  
  protected var _sortColumn: Int = 0
  protected var _sortAscending: Boolean = false
  
  protected var requestProbes: Array[String] = initProbes
  
  loadRawData()
  
  /**
   * What is the current sort column?
   */
  def sortColumn: Int = _sortColumn
  
  /**
   * Is the current sort type ascending?
   */
  def sortAscending: Boolean = _sortAscending
  
  /**
   * Set the filtering threshold for a column with separate filtering.
   */
  def setFilterThreshold(col: Int, threshold: java.lang.Double): Unit = {
    currentInfo.setColumnFilter(col, threshold)        
    resetSortAndFilter()
    filterAndSort()
  }
  
  /**
   * Select only the rows corresponding to the given probes.
   */
  def selectProbes(probes: Array[String]): Unit = {
    requestProbes = probes
    resetSortAndFilter()
    filterAndSort()
  }
  
  protected def filterAndSort(): Unit = {
    def f(r: Seq[ExpressionValue]): Boolean = {
      for (col <- 0 until currentInfo.numColumns();
    		  thresh = currentInfo.columnFilter(col);
    		  if (thresh != null)) {      
        val isUpper = currentInfo.isUpperFiltering(col)
        val pass: Boolean = (if (isUpper) {
          Math.abs(r(col).value) <= thresh
        } else {
          Math.abs(r(col).value) >= thresh
        })
        if (! (pass && !java.lang.Double.isNaN(r(col).value))) {        
          return false
        }
      }
      true
    }

    currentMat = currentMat.selectNamedRows(requestProbes)    
    currentMat = currentMat.filterRows(f)
    
    currentInfo.setNumRows(currentMat.rows)
    sort(_sortColumn, _sortAscending)
  }
  
  def sort(col: Int, ascending: Boolean): Unit = {
      def sortData(v1: Seq[ExpressionValue],
                 v2: Seq[ExpressionValue]): Boolean = {
      val ev1 = v1(col)
      val ev2 = v2(col)
      if (ev1.call == 'A' && ev2.call != 'A') {
        false
      } else if (ev1.call != 'A' && ev2.call == 'A') {
        true
      } else {
        if (ascending) { ev1.value < ev2.value } else { ev1.value > ev2.value }
      }
    }
      
    _sortColumn = col
    _sortAscending = ascending
    currentMat = currentMat.sortRows(sortData)
  }
  
  def addSynthetic(s: Synthetic): Unit = {
    _synthetics :+= s
    addOneSynthetic(s)
  }
  
  def removeSynthetics(): Unit = {
    _synthetics = Vector()
    val nonTestColumns = 0 until currentInfo.numDataColumns()
    currentMat = currentMat.selectColumns(nonTestColumns)
    rawGroupedMat = rawGroupedMat.selectColumns(nonTestColumns)
    currentInfo.removeSynthetics()
  }
  
  /**
   * Adds one two-group test to the raw grouped matrix.
   * Re-filtering will be necessary to produce the final matrix.
   */
  protected def addOneSynthetic(s: Synthetic): Unit = {
    s match {
      case test: Synthetic.TwoGroupSynthetic =>
        //TODO
        val g1s = test.getGroup1.getSamples.filter(_.get("dose_level") != "Control").map(_.getCode)
        val g2s = test.getGroup2.getSamples.filter(_.get("dose_level") != "Control").map(_.getCode)       
        var upper = true
        currentMat = test match {
          case ut: Synthetic.UTest =>
            currentMat.appendUTest(rawUngroupedMat, g1s, g2s, ut.getShortTitle(null)) //TODO don't pass null
          case tt: Synthetic.TTest =>
            currentMat.appendTTest(rawUngroupedMat, g1s, g2s, tt.getShortTitle(null)) //TODO
          case md: Synthetic.MeanDifference =>
            upper = false
            currentMat.appendDiffTest(rawUngroupedMat, g1s, g2s, md.getShortTitle(null)) //TODO
          case _ => throw new Exception("Unexpected test type!")
        }
        currentInfo.addColumn(true, test.getShortTitle(null), test.getTooltip(), upper, null) //TODO
      case _ => throw new Exception("Unexpected test type")
    }       
  }
  
  protected def applySynthetics(): Unit = {
    for (s <- _synthetics) {
      addOneSynthetic(s)
    }
  }
 
  /**
   * Reset modifications such as filtering, sorting and probe selection.
   * Synthetics are restored after resetting.
   */
  def resetSortAndFilter(): Unit = {
    currentMat = rawGroupedMat
    currentInfo.setNumRows(currentMat.rows)
    applySynthetics()
  }
  
  /**
   * Obtain the current info for this matrix.
   * The only info members that can change once a matrix has been constructed
   * is data relating to the synthetic columns (since they can be manually
   * added and removed).
   */
  def info: ManagedMatrixInfo = currentInfo

  /**
   * Obtain the current view of this matrix, with all modifications
   * applied.
   */
  def current: ExprMatrix = currentMat
  
  def rawData: ExprMatrix = rawUngroupedMat
  
  /**
   * Initial data loading.
   */
  protected def loadRawData(): Unit = {
    val pmap = context.unifiedProbes
    var groupedParts, ungroupedParts: List[ExprMatrix] = Nil
    
    val packedProbes = initProbes.map(pmap.pack)
    for (g <- requestColumns) {
    	val barcodes = samplesForDisplay(g)
    	val sortedBarcodes = reader.sortSamples(barcodes.map(b => Sample(b.getCode)))
        val data = reader.valuesForSamplesAndProbes(sortedBarcodes,
        		packedProbes, sparseRead)
        groupedParts ::= columnsForGroup(g, sortedBarcodes, data)
        
        ungroupedParts ::= ExprMatrix.withRows(data.map(_.map(asJava(_))), 
            initProbes, sortedBarcodes.map(_.sampleId))
    }

    val annotations = initProbes.map(new RowAnnotation(_)).toVector
    
    rawGroupedMat = groupedParts.reverse.reduceLeft(_ adjoinRight _).
    	copyWithAnnotations(annotations)
    rawUngroupedMat = ungroupedParts.reverse.reduceLeft(_ adjoinRight _).
    	copyWithAnnotations(annotations)
    currentMat = rawGroupedMat
    currentInfo.setNumRows(currentMat.rows)
  }
  
  final protected def selectIdx(data: Seq[E], is: Seq[Int]) = is.map(data(_))
  final protected def javaMean(data: Iterable[E]) = {
    val mean = ExprValue.presentMean(data, "")
    var tooltip = data.take(5).map(_.toString).mkString(" ")
    if (data.size > 5) {
      tooltip += ", ..."
    }
    new ExpressionValue(mean.value, mean.call, tooltip)
  }

  protected def selectIdxs(g: Group,  predicate: (t.common.shared.Unit) => Boolean, 
      barcodes: Seq[Sample]): Seq[Int] = {
    val units = g.getUnits().filter(predicate)
    val ids = units.flatMap(_.getSamples.map(_.getCode)).toSet
    val inSet = barcodes.map(s => ids.contains(s.sampleId))
    inSet.zipWithIndex.filter(_._1).map(_._2)
  }
  
  //TODO
  protected def controlIdxs(g: Group, barcodes: Seq[Sample]): Seq[Int] = 
    selectIdxs(g, _.get("dose_level") == "Control", barcodes)    

  protected def treatedIdxs(g: Group, barcodes: Seq[Sample]): Seq[Int] = 
    selectIdxs(g, _.get("dose_level") != "Control", barcodes)    

  protected def columnsForGroup(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): ExprMatrix = {
    // A simple average column
    val treatedIdx = treatedIdxs(g, sortedBarcodes)
    
    currentInfo.addColumn(false, g.toString, "Average of treated samples", false, g)
    ExprMatrix.withRows(data.map(vs =>
      Vector(javaMean(selectIdx(vs, treatedIdx)))),
      initProbes,
      List(g.toString))
  }
  
  protected def samplesForDisplay(g: Group): Iterable[OTGSample] = {
    //TODO
    val (cus, ncus) = g.getUnits().partition(_.get("dose_level") == "Control")
    if (ncus.size > 1) {
      //treated samples only
      ncus.flatMap(_.getSamples())
    } else {
      //all samples
      g.getSamples()
    }
  } 
}

/**
 * Columns consisting of normalized intensity / "absolute value" expression data
 * for both treated and control samples. 
 */
class NormalizedIntensityMatrix(requestColumns: Seq[Group],
    reader: MatrixDBReader[ExprValue], initProbes: Array[String], sparseRead: Boolean,
    enhancedColumns: Boolean)(implicit context: OTGContext) 
extends ManagedMatrix[ExprValue](requestColumns, reader, initProbes, sparseRead) {

  override protected def columnsForGroup(g: Group, sortedBarcodes: Seq[Sample], 
      data: Seq[Seq[ExprValue]]): ExprMatrix = {
    //TODO
    val (cus, ncus) = g.getUnits().partition(_.get("dose_level") == "Control")
    
    if (ncus.size > 1 || (!enhancedColumns)) {
      // A simple average column
      super.columnsForGroup(g, sortedBarcodes, data)      
    } else if (ncus.size == 1) {
      // Insert a control column as well as the usual one
      
      val treatedIdx = treatedIdxs(g, sortedBarcodes)
      val controlIdx = controlIdxs(g, sortedBarcodes)
      
      val (colName1, colName2) = (g.toString, g.toString + "(cont)")
      val rows = data.map(vs => Vector(
          javaMean(selectIdx(vs, treatedIdx)),
          javaMean(selectIdx(vs, controlIdx))
    	))
    	
      currentInfo.addColumn(false, colName1, "Average of treated samples", false, g)
      currentInfo.addColumn(false, colName2, "Average of control samples", false, g)
      ExprMatrix.withRows(rows, initProbes, List(colName1, colName2))            
    } else {
      throw new Exception("No units in group")
    }
  }
}

/**
 * No extra columns. Simple averaged fold values.
 */
class FoldValueMatrix(requestColumns: Seq[Group],
    reader: MatrixDBReader[ExprValue], initProbes: Array[String], sparseRead: Boolean)
    (implicit context: OTGContext) 
    extends ManagedMatrix[ExprValue](requestColumns, reader, initProbes, sparseRead) {
}

/**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
class ExtFoldValueMatrix(requestColumns: Seq[Group],
    reader: MatrixDBReader[PExprValue], initProbes: Array[String], sparseRead: Boolean,
    enhancedColumns: Boolean)
    (implicit context: OTGContext) 
    extends ManagedMatrix[PExprValue](requestColumns, reader, initProbes, sparseRead) {
 
    override protected def columnsForGroup(g: Group, sortedBarcodes: Seq[Sample], 
      data: Seq[Seq[PExprValue]]): ExprMatrix = {
      //TODO
    val (cus, ncus) = g.getUnits().partition(_.get("dose_level") == "Control")
    
    println(s"#Control units: ${cus.size} #Non-control units: ${ncus.size}")
    
    if (ncus.size > 1 || (!enhancedColumns)) {
      // A simple average column
      super.columnsForGroup(g, sortedBarcodes, data)      
    } else if (ncus.size == 1) {
      // Insert a p-value column as well as the usual one
      
      val (colName1, colName2) = (g.toString, g.toString + "(p)")
      val treatedIdx = treatedIdxs(g, sortedBarcodes)
      
      // TODO: work out call properly
      val rows = data.map(vs => {
        val treatedVs = selectIdx(vs, treatedIdx)
        val first = treatedVs.head
        Vector(javaMean(treatedVs), new ExpressionValue(first.p, first.call))
      })
      
      currentInfo.addColumn(false, colName1, "Average of treated samples", false, g)
      currentInfo.addColumn(false, colName2, "p-values of treated against control", true, g)
      
      ExprMatrix.withRows(rows, initProbes, List(colName1, colName2))
    } else {
      throw new Exception("No units in group")
    }
  }
}
