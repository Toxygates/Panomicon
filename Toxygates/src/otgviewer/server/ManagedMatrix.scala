package otgviewer.server

import t.common.shared.sample.ExpressionValue
import otgviewer.server.rpc.Conversions._
import otgviewer.shared.Group
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.Synthetic
import t.db.MatrixDBReader
import otgviewer.shared.OTGSample
import t.db.Sample
import t.db.PExprValue
import t.db.ExprValue
import t.db.MatrixContext

/**
 * Routines for loading a ManagedMatrix
 * and constructing groups.
 */
abstract class ManagedMatrixBuilder[E <: ExprValue]
  (reader: MatrixDBReader[E], val probes: Array[String]) {

  /**
   * Info corresponding to the matrix being built. Gradually updated.
   */
  protected val info = new ManagedMatrixInfo()
      
  def build(requestColumns: Seq[Group], sparseRead: Boolean,
    fullLoad: Boolean)
    (implicit context: MatrixContext): ManagedMatrix = {
    loadRawData(requestColumns, reader, sparseRead,
        fullLoad)
  }
  
  /**
   * Construct the columns representing a particular group (g), from the given 
   * raw data. Update info to reflect the changes.
   */
  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample], 
    data: Seq[Seq[E]]): ExprMatrix
  
   protected def defaultColumns[E <: ExprValue](g: Group, sortedBarcodes: Seq[Sample], 
    data: Seq[Seq[ExprValue]]): ExprMatrix = {
    // A simple average column
    val tus = treatedAndControl(g)._1
    val treatedIdx = unitIdxs(tus, sortedBarcodes)
    
    info.addColumn(false, g.toString, "Average of treated samples", false, g, false)
    ExprMatrix.withRows(data.map(vs =>
      EVArray(Seq(javaMean(selectIdx(vs, treatedIdx))))),
      probes,
      List(g.toString))
  }
  
  def loadRawData(requestColumns: Seq[Group],
      reader: MatrixDBReader[E], sparseRead: Boolean,
      fullLoad: Boolean)
  (implicit context: MatrixContext): ManagedMatrix = {
    val pmap = context.probeMap
    
    var rawGroupedMat, rawUngroupedMat: ExprMatrix = null
    
    val packedProbes = probes.map(pmap.pack)

    val annotations = probes.map(x => new RowAnnotation(x, List(x))).toVector
    
    for (g <- requestColumns) {
        //Remove repeated samples as some other algorithms assume distinct samples
        //Also for efficiency
      val samples = 
        (if(fullLoad) g.getSamples.toList else samplesForDisplay(g)).
          toVector.distinct
      val sortedSamples = reader.sortSamples(samples.map(b => Sample(b.getCode)))
        val data = reader.valuesForSamplesAndProbes(sortedSamples,
            packedProbes, sparseRead)
            
        println(g.getUnits()(0).toString())
        
        val rowLookup = Map() ++ data.map(r => r(0).probe.identifier -> r)
        val standardOrder = probes.map(p => rowLookup(p))
        
        val grouped = columnsFor(g, sortedSamples, standardOrder)

        val ungrouped = ExprMatrix.withRows(standardOrder.map(r => EVArray(r.map(asJava(_)))), 
            probes, sortedSamples.map(_.sampleId))
            
        if (rawGroupedMat == null) {
          rawGroupedMat = grouped
        } else {
          rawGroupedMat = rawGroupedMat adjoinRight grouped
        }
      
      if (rawUngroupedMat == null) {
        rawUngroupedMat = ungrouped
      } else {
        //account for the fact that samples may be shared between requestColumns
        val newCols = ungrouped.columnKeys.toSet -- rawUngroupedMat.columnKeys.toSet
        rawUngroupedMat = rawUngroupedMat adjoinRight 
          (ungrouped.selectNamedColumns(newCols.toSeq))
      }     
    }

    new ManagedMatrix(probes, info, 
        rawUngroupedMat.copyWithAnnotations(annotations), 
        rawGroupedMat.copyWithAnnotations(annotations))
  }
  
  final protected def selectIdx[E <: ExprValue](data: Seq[E], is: Seq[Int]) = is.map(data(_))
  final protected def javaMean[E <: ExprValue](data: Iterable[E]) = {
    val mean = ExprValue.presentMean(data, "")
    var tooltip = data.take(10).map(_.toString).mkString(" ")
    if (data.size > 10) {
      tooltip += ", ..."
    }
    new ExpressionValue(mean.value, mean.call, tooltip)
  }
  
  protected def unitIdxs(us: Iterable[t.viewer.shared.Unit], samples: Seq[Sample]): Seq[Int] = {
    val ids = us.flatMap(u => u.getSamples.map(_.getCode)).toSet
    val inSet = samples.map(s => ids.contains(s.sampleId))
    inSet.zipWithIndex.filter(_._1).map(_._2)
  }

  protected def samplesForDisplay(g: Group): Iterable[OTGSample] = {
    //TODO
    val (tus, cus) = treatedAndControl(g)
    if (tus.size > 1) {
      //treated samples only
      tus.flatMap(_.getSamples())
    } else {
      //all samples
      g.getSamples()
    }
  } 
  
  //TODO use schema
  protected def treatedAndControl(g: Group) = 
    g.getUnits().partition(_.get("dose_level") == "Control")
}
  
/**
 * No extra columns. Simple averaged fold values.
 */
class FoldBuilder(reader: MatrixDBReader[ExprValue], probes: Array[String]) 
  extends ManagedMatrixBuilder[ExprValue](reader, probes) {
  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample], 
    data: Seq[Seq[ExprValue]]): ExprMatrix = 
    defaultColumns(g, sortedBarcodes, data)   
}

trait TreatedControlBuilder[E <: ExprValue] {
  this: ManagedMatrixBuilder[E] =>
  def enhancedColumns: Boolean
    
    protected def buildRow(raw: Seq[E], 
      treatedIdx: Seq[Int], controlIdx: Seq[Int]): EVArray
  
    protected def addColumnInfo(g: Group): Unit
    def colNames(g: Group): Seq[String]
  
    protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample], 
      data: Seq[Seq[E]]): ExprMatrix = {
    //TODO
    val (tus, cus) = treatedAndControl(g)    
    println(s"#Control units: ${cus.size} #Non-control units: ${tus.size}")
    
    if (tus.size > 1 || (!enhancedColumns) || cus.size == 0 || tus.size == 0) {
      // A simple average column
      defaultColumns(g, sortedBarcodes, data)      
    } else if (tus.size == 1) {
      // Possibly insert a control column as well as the usual one

      val ti = unitIdxs(tus, sortedBarcodes)
      val ci = unitIdxs(cus, sortedBarcodes)

      val rows = data.map(vs => buildRow(vs, ti, ci))       
      addColumnInfo(g)
    
      ExprMatrix.withRows(rows, probes, colNames(g))                       
    } else {
      throw new Exception("No units in group")
    }
  }
}

/**
 * Columns consisting of normalized intensity / "absolute value" expression data
 * for both treated and control samples. 
 */
class NormalizedBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[ExprValue], 
    probes: Array[String]) extends ManagedMatrixBuilder[ExprValue](reader, probes) 
    with TreatedControlBuilder[ExprValue] {
  
  protected def buildRow(raw: Seq[ExprValue], 
      treatedIdx: Seq[Int], controlIdx: Seq[Int]): EVArray = 
    EVArray(Seq(
      javaMean(selectIdx(raw, treatedIdx)),
      javaMean(selectIdx(raw, controlIdx)))
    )

  protected def addColumnInfo(g: Group) {
    info.addColumn(false, colNames(g)(1), "Average of treated samples", false, g, false)
    info.addColumn(false, colNames(g)(2), "Average of control samples", false, g, false)
  }
  
  def colNames(g: Group): Seq[String] = 
    List(g.toString, g.toString + "(cont)")
    
}

/**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
class ExtFoldBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[PExprValue], 
    probes: Array[String]) extends ManagedMatrixBuilder[PExprValue](reader, probes) 
    with TreatedControlBuilder[PExprValue] {

  protected def buildRow(raw: Seq[PExprValue], 
      treatedIdx: Seq[Int], controlIdx: Seq[Int]): EVArray = {
    val treatedVs = selectIdx(raw, treatedIdx)
    val first = treatedVs.head
    EVArray(Seq(javaMean(treatedVs), new ExpressionValue(first.p, first.call)))
  }  
  
  protected def addColumnInfo(g: Group) {
    info.addColumn(false, colNames(g)(0), "Average of treated samples", false, g, false)
    info.addColumn(false, colNames(g)(1), "p-values of treated against control", true, g, true)      
  }
  
  def colNames(g: Group) =
    List(g.toString, g.toString + "(p)")   
}

/**
 * A server-side ExprMatrix and support logic for
 * sorting, filtering, data loading etc.
 * 
 * A managed matrix is constructed on the basis of some number of 
 * "request columns" but may insert additional columns with extra information.
 * The info object should be used to query what columns have actually been 
 * constructed.
 * 
 * @param rawUngroupedMat ungrouped matrix. 
 * Mainly used for computing T- and U-tests. Sorting is irrelevant.
 *
 * @param rawGroupedMat unfiltered matrix. 
 *  The final view is obtained by filtering this (if requested).
 */

class ManagedMatrix(val initProbes: Array[String],
    //TODO visibility of these 3 vars
    var currentInfo: ManagedMatrixInfo, var rawUngroupedMat: ExprMatrix,    
    var rawGroupedMat: ExprMatrix) {
  
  protected var currentMat: ExprMatrix = rawGroupedMat
  
  protected var _synthetics: Vector[Synthetic] = Vector()  
  protected var _sortColumn: Int = -1
  protected var _sortAscending: Boolean = false
  
  protected var requestProbes: Array[String] = initProbes
  
  currentInfo.setNumRows(currentMat.rows)
    
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
      def sortData(v1: EVArray,
                 v2: EVArray): Boolean = {
      val ev1 = v1(col)
      val ev2 = v2(col)
      if (ev1.call == 'A' && ev2.call != 'A') {
        false
      } else if (ev1.call != 'A' && ev2.call == 'A') {
        true
      } else {
        //Use this to handle NaN correctly (comparison method MUST be transitive)
        def cmp(x: Double, y: Double) = java.lang.Double.compare(x, y)
        if (ascending) {
          cmp(ev1.value, ev2.value) < 0
        } else {
          cmp(ev1.value, ev2.value) > 0
        }
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
    val dataColumns = 0 until currentInfo.numDataColumns()
    currentMat = currentMat.selectColumns(dataColumns)
    rawGroupedMat = rawGroupedMat.selectColumns(dataColumns)
    currentInfo.removeSynthetics()
  }
  
  /**
   * Adds one two-group test to the current matrix.
   */
  protected def addOneSynthetic(s: Synthetic): Unit = {
    s match {
      case test: Synthetic.TwoGroupSynthetic =>
        //TODO
        val g1s = test.getGroup1.getSamples.filter(_.get("dose_level") != "Control").map(_.getCode)
        val g2s = test.getGroup2.getSamples.filter(_.get("dose_level") != "Control").map(_.getCode)       
        var upper = true
        
        val currentRows = (0 until currentMat.rows).map(i => currentMat.rowAt(i))
        //Need this to take into account sorting and filtering of currentMat
        val rawData = rawUngroupedMat.selectNamedRows(currentRows)
        
        currentMat = test match {
          case ut: Synthetic.UTest =>
            currentMat.appendUTest(rawData, g1s, g2s, ut.getShortTitle(null)) //TODO don't pass null
          case tt: Synthetic.TTest =>
            currentMat.appendTTest(rawData, g1s, g2s, tt.getShortTitle(null)) //TODO
          case md: Synthetic.MeanDifference =>
            upper = false
            currentMat.appendDiffTest(rawData, g1s, g2s, md.getShortTitle(null)) //TODO
          case _ => throw new Exception("Unexpected test type!")
        }
        currentInfo.addColumn(true, test.getShortTitle(null), test.getTooltip(), upper, null, false) //TODO
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
}
