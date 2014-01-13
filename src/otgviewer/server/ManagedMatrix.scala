package otgviewer.server

import otgviewer.shared.BarcodeColumn
import otgviewer.shared.Synthetic
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.DataFilter
import otgviewer.shared.Group
import otg.ExprValue
import otg.db.MicroarrayDBReader
import otg.OTGContext
import otgviewer.server.rpc.Conversions._
import friedrich.data.immutable.VVector
import bioweb.shared.array.ExpressionValue
import otg.PExprValue
import otgviewer.shared.Barcode


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
    val reader: MicroarrayDBReader[E],
    initProbes: Array[String], sparseRead: Boolean)
    (implicit val filter: DataFilter, context: OTGContext) {
  
  protected var currentInfo: ManagedMatrixInfo = new ManagedMatrixInfo()
  protected var rawUngroupedMat, rawGroupedMat, currentMat: ExprMatrix = _
  
  protected var _synthetics: Vector[Synthetic] = Vector()  
  protected var _sortColumn: Int = 0
  protected var _sortAscending: Boolean = false
  
  protected var generalFilter: Option[Double] = None
  protected var separateFilters: Map[Int, Option[Double]] = Map()
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
   * Set the normal filtering threshold (applies across all data columns)
   */
  def filterData(threshold: Option[Double]): Unit = {
    generalFilter = threshold
    resetSortAndFilter()
    filterAndSort()
  }  
  
  /**
   * Set the filtering threshold for a column with separate filtering.
   */
  def filterSeparate(col: Int, threshold: Option[Double]): Unit = {
    separateFilters += (col -> threshold)
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
    def f(r: Seq[ExpressionValue], thresh: Double, beforeCol: Int): Boolean = {
      r.take(beforeCol).exists(v => (Math.abs(v.value) >= thresh - 0.0001) ||
        (java.lang.Double.isNaN(v.value) && thresh == 0))
    }

    currentMat = currentMat.selectNamedRows(requestProbes)
    
    //TODO: separate filtering
    generalFilter match {
      case Some(filtVal) =>
        currentMat = currentMat.filterRows(r => f(r, filtVal, currentInfo.numDataColumns()))
      case None =>
    }
    
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
        val g1s = test.getGroup1.getSamples.map(_.getCode)
        val g2s = test.getGroup2.getSamples.map(_.getCode)
        currentMat = test match {
          case ut: Synthetic.UTest =>
            currentMat.appendUTest(rawUngroupedMat, g1s, g2s, ut.getShortTitle)
          case tt: Synthetic.TTest =>
            currentMat.appendTTest(rawUngroupedMat, g1s, g2s, tt.getShortTitle)
          case md: Synthetic.MeanDifference =>
            currentMat.appendDiffTest(rawUngroupedMat, g1s, g2s, md.getShortTitle)
          case _ => throw new Exception("Unexpected test type!")
        }
        currentInfo.addColumn(true, test.getShortTitle(), test.getTooltip(), false, null)
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
    val pmap = context.probes(filter)
    var groupedParts, ungroupedParts: List[ExprMatrix] = Nil
    
    val packedProbes = initProbes.map(pmap.pack)
    for (g <- requestColumns) {
    	val barcodes = samplesForDisplay(g)
    	val sortedBarcodes = reader.sortSamples(barcodes.map(b => otg.Sample(b.getCode)))
        val data = reader.valuesForSamplesAndProbes(filter, sortedBarcodes,
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
  final protected def javaMean(data: Iterable[E]) = 
    asJava(ExprValue.presentMean(data, ""))

  protected def columnsForGroup(g: Group, sortedBarcodes: Seq[otg.Sample],
    data: Seq[Seq[E]]): ExprMatrix = {
    // A simple average column

    val (cus, ncus) = g.getUnits().partition(_.getDose == "Control")
    val controlIds = cus.flatMap(_.getSamples.map(_.getCode)).toSet
    val isControl = sortedBarcodes.map(s => controlIds.contains(s.sampleId))
    val treatedIdx = isControl.zipWithIndex.filter(!_._1).map(_._2)
    
    currentInfo.addColumn(false, g.toString, "Average of treated samples", false, g)
    ExprMatrix.withRows(data.map(vs =>
      VVector(javaMean(selectIdx(vs, treatedIdx)))),
      initProbes,
      List(g.toString))
  }
  
  protected def samplesForDisplay(g: Group): Iterable[Barcode] = {
    val (cus, ncus) = g.getUnits().partition(_.getDose == "Control")
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
    reader: MicroarrayDBReader[ExprValue],
    initProbes: Array[String], sparseRead: Boolean)
    (implicit filter: DataFilter, context: OTGContext) 
extends ManagedMatrix[ExprValue](requestColumns, reader, initProbes, sparseRead) {

  override protected def columnsForGroup(g: Group, sortedBarcodes: Seq[otg.Sample], 
      data: Seq[Seq[ExprValue]]): ExprMatrix = {
    val (cus, ncus) = g.getUnits().partition(_.getDose == "Control")
    
    if (ncus.size > 1) {
      // A simple average column
      super.columnsForGroup(g, sortedBarcodes, data)      
    } else if (ncus.size == 1) {
      // Insert a control column as well as the usual one
      
      //TODO: factor out this pattern
      val controlIds = cus.flatMap(_.getSamples.map(_.getCode)).toSet
      val isControl = sortedBarcodes.map(s => controlIds.contains(s.sampleId))               
      val controlIdx = isControl.zipWithIndex.filter(_._1).map(_._2)
      val treatedIdx = isControl.zipWithIndex.filter(! _._1).map(_._2)
      
      val (colName1, colName2) = (g.toString, g.toString + "(cont)")
      val rows = data.map(vs => VVector(
          javaMean(selectIdx(vs, treatedIdx)),
          javaMean(selectIdx(vs, controlIdx))
    	))
    	
      currentInfo.addColumn(false, colName1, "Average of treated samples", false, g)
      //TODO: separate filtering
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
    reader: MicroarrayDBReader[ExprValue],
    initProbes: Array[String], sparseRead: Boolean)
    (implicit filter: DataFilter, context: OTGContext) 
    extends ManagedMatrix[ExprValue](requestColumns, reader, initProbes, sparseRead) {
}

/**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
class ExtFoldValueMatrix(requestColumns: Seq[Group],
    reader: MicroarrayDBReader[PExprValue], 
    initProbes: Array[String], sparseRead: Boolean)
    (implicit filter: DataFilter, context: OTGContext) 
    extends ManagedMatrix[PExprValue](requestColumns, reader, initProbes, sparseRead) {
 
    override protected def columnsForGroup(g: Group, sortedBarcodes: Seq[otg.Sample], 
      data: Seq[Seq[PExprValue]]): ExprMatrix = {
    val (cus, ncus) = g.getUnits().partition(_.getDose == "Control")
    
    if (ncus.size > 1) {
      // A simple average column
      super.columnsForGroup(g, sortedBarcodes, data)      
    } else if (ncus.size == 1) {
      // Insert a p-value column as well as the usual one
            
      val (colName1, colName2) = (g.toString, g.toString + "(p)")
      val controlIds = cus.flatMap(_.getSamples.map(_.getCode)).toSet
      val isControl = sortedBarcodes.map(s => controlIds.contains(s.sampleId))         
      val treatedIdx = isControl.zipWithIndex.filter(! _._1).map(_._2)
      
      // TODO: work out call properly
      val rows = data.map(vs => {
        val treatedVs = selectIdx(vs, treatedIdx)
        val first = treatedVs.head
        VVector(javaMean(treatedVs), new ExpressionValue(first.p, first.call))
      })
      
      currentInfo.addColumn(false, colName1, "Average of treated samples", false, g)
      currentInfo.addColumn(false, colName2, "p-values of treated against control", false, g)
      
      ExprMatrix.withRows(rows, initProbes, List(colName1, colName2))
    } else {
      throw new Exception("No units in group")
    }
  }
}
