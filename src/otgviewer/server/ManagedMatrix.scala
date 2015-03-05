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
object ManagedMatrixBuilder {
    
/**
 * No extra columns. Simple averaged fold values.
 */
  def buildFold(requestColumns: Seq[Group],
    reader: MatrixDBReader[ExprValue], initProbes: Array[String], sparseRead: Boolean,
    fullLoad: Boolean)
    (implicit context: MatrixContext): ManagedMatrix = {
    loadRawData[ExprValue](requestColumns, reader, initProbes, sparseRead,
        fullLoad,
        columnsForGroupDefault(_, _, _, _, _))
  }
  
/**
 * Columns consisting of normalized intensity / "absolute value" expression data
 * for both treated and control samples. 
 */
  def buildNormalized(requestColumns: Seq[Group],
    reader: MatrixDBReader[ExprValue], initProbes: Array[String], sparseRead: Boolean,
    fullLoad: Boolean,
    enhancedColumns: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    loadRawData[ExprValue](requestColumns, reader, initProbes, sparseRead,
        fullLoad,
        columnsForGroupNormalized(enhancedColumns, _, _, _, _, _))
  }
   
  /**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
  def buildExtFold(requestColumns: Seq[Group],
    reader: MatrixDBReader[PExprValue], initProbes: Array[String], sparseRead: Boolean,
    fullLoad: Boolean,
    enhancedColumns: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    loadRawData[PExprValue](requestColumns, reader, initProbes, sparseRead,
        fullLoad,
        columnsForGroupExtFold(enhancedColumns, _, _, _, _, _))
  }
  
  def loadRawData[E <: ExprValue](requestColumns: Seq[Group],
      reader: MatrixDBReader[E], initProbes: Array[String], sparseRead: Boolean,
      fullLoad: Boolean,
      columnBuilder: (Array[String], ManagedMatrixInfo, Group, Seq[Sample], Seq[Seq[E]]) => ExprMatrix)
  (implicit context: MatrixContext): ManagedMatrix = {
    val pmap = context.probeMap
    
    var rawGroupedMat, rawUngroupedMat: ExprMatrix = null
    
    val packedProbes = initProbes.map(pmap.pack)
    val info = new ManagedMatrixInfo()
    val annotations = initProbes.map(x => new RowAnnotation(x, List(x))).toVector
    
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
        
        val returnedProbes = data.map(_(0).probe.identifier).toArray
        
        //Force standard sort order
        val grouped = columnBuilder(returnedProbes, info, g, sortedSamples, data).
          selectNamedRows(initProbes)

        val ungrouped = ExprMatrix.withRows(data.map(_.map(asJava(_))), 
            returnedProbes, sortedSamples.map(_.sampleId)).
            selectNamedRows(initProbes)
            
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

    new ManagedMatrix(initProbes, info, 
        rawUngroupedMat.copyWithAnnotations(annotations), 
        rawGroupedMat.copyWithAnnotations(annotations))
  }
  
  private def columnsForGroupDefault[E <: ExprValue](initProbes: Array[String],
      info: ManagedMatrixInfo, g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): ExprMatrix = {
    // A simple average column
    val treatedIdx = treatedIdxs(g, sortedBarcodes)
    
    info.addColumn(false, g.toString, "Average of treated samples", false, g, false)
    ExprMatrix.withRows(data.map(vs =>
      Vector(javaMean(selectIdx(vs, treatedIdx)))),
      initProbes,
      List(g.toString))
  }
  
  private def columnsForGroupNormalized(enhancedColumns: Boolean,
      initProbes: Array[String],
      info: ManagedMatrixInfo, 
      g: Group, sortedBarcodes: Seq[Sample], 
      data: Seq[Seq[ExprValue]]): ExprMatrix = {
    //TODO
    val (cus, tus) = g.getUnits().partition(_.get("dose_level") == "Control")
    
    if (tus.size > 1 || (!enhancedColumns) || cus.size == 0 || tus.size == 0) {
      // A simple average column
      columnsForGroupDefault(initProbes, info, g, sortedBarcodes, data)      
    } else if (tus.size == 1) {
      // Possibly insert a control column as well as the usual one

      val treatedIdx = treatedIdxs(g, sortedBarcodes)
      val controlIdx = controlIdxs(g, sortedBarcodes)

      val (colName1, colName2) = (g.toString, g.toString + "(cont)")
      val rows = data.map(vs => Vector(
        javaMean(selectIdx(vs, treatedIdx)),
        javaMean(selectIdx(vs, controlIdx))))

      info.addColumn(false, colName1, "Average of treated samples", false, g, false)
      info.addColumn(false, colName2, "Average of control samples", false, g, false)
      ExprMatrix.withRows(rows, initProbes, List(colName1, colName2))                       
    } else {
      throw new Exception("No units in group")
    }
  }
  
  private def columnsForGroupExtFold(enhancedColumns: Boolean, initProbes: Array[String],
      info: ManagedMatrixInfo,
      g: Group, sortedBarcodes: Seq[Sample], 
      data: Seq[Seq[PExprValue]]): ExprMatrix = {
      //TODO
    val (cus, tus) = g.getUnits().partition(_.get("dose_level") == "Control")
    
    println(s"#Control units: ${cus.size} #Non-control units: ${tus.size}")
    
    if (tus.size > 1 || (!enhancedColumns) || tus.size == 0) {
      // A simple average column
      columnsForGroupDefault(initProbes, info, g, sortedBarcodes, data)      
    } else if (tus.size == 1) {
      // Insert a p-value column as well as the usual one
      
      val (colName1, colName2) = (g.toString, g.toString + "(p)")
      val treatedIdx = treatedIdxs(g, sortedBarcodes)
      
      // TODO: work out call properly
      val rows = data.map(vs => {
        val treatedVs = selectIdx(vs, treatedIdx)
        val first = treatedVs.head
        Vector(javaMean(treatedVs), new ExpressionValue(first.p, first.call))
      })
      
      info.addColumn(false, colName1, "Average of treated samples", false, g, false)
      info.addColumn(false, colName2, "p-values of treated against control", true, g, true)
      
      ExprMatrix.withRows(rows, initProbes, List(colName1, colName2))
    } else {
      throw new Exception("No units in group")
    }
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
 * A server-side ExprMatrix and support logic for
 * sorting, filtering, data loading etc.
 * 
 * A managed matrix is constructed on the basis of some number of 
 * "request columns" but may insert additional columns with extra information.
 * The info object should be used to query what columns have actually been 
 * constructed.
 * 
 */

class ManagedMatrix(val initProbes: Array[String],
    //TODO visibility of these 3 vars
    var currentInfo: ManagedMatrixInfo,
    //ungrouped mat is mainly used for computing T- and U-tests. Sorting of columns
    //is irrelevant.
    var rawUngroupedMat: ExprMatrix, var rawGroupedMat: ExprMatrix) {
  
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
      def sortData(v1: Seq[ExpressionValue],
                 v2: Seq[ExpressionValue]): Boolean = {
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
    val nonTestColumns = 0 until currentInfo.numDataColumns()
    currentMat = currentMat.selectColumns(nonTestColumns)
    rawGroupedMat = rawGroupedMat.selectColumns(nonTestColumns)
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
