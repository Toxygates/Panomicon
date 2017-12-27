package t.viewer.server.matrix

import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.Group
import t.common.shared.sample.{ Sample => SSample }
import t.common.shared.sample.{ Unit => TUnit }
import t.db._
import t.viewer.server.Conversions._
import t.viewer.shared.ColumnFilter
import t.viewer.shared.ManagedMatrixInfo
import t.viewer.shared.Synthetic
import otg.model.sample.OTGAttribute

/**
 * Routines for loading a ManagedMatrix and constructing groups.
 */
abstract class ManagedMatrixBuilder[E >: Null <: ExprValue](reader: MatrixDBReader[E], val probes: Seq[String]) {
  import ManagedMatrix._
  
  def build(requestColumns: Seq[Group], sparseRead: Boolean,
    fullLoad: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    loadRawData(requestColumns, reader, sparseRead,
      fullLoad)
  }

  /**
   * Construct the columns representing a particular group (g), from the given
   * raw data. Update info to reflect the changes.
   * Resulting data should be row-major.
   */
  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): (Seq[RowData], ManagedMatrixInfo)

  /**
   * Collapse multiple raw expression values into a single cell.
   */
  protected def buildValue(raw: RowData): ExprValue

  /**
   * Default tooltip for columns
   */
  protected def tooltipSuffix: String = ": average of treated samples"
  
  protected def shortName(g: Group): String = g.toString
  
  protected def defaultColumns[E <: ExprValue](g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[RowData]): (Seq[RowData], ManagedMatrixInfo) = {
    // A simple average column
    val tus = treatedAndControl(g)._1
    val treatedIdx = unitIdxs(tus, sortedBarcodes)
    val samples = TUnit.collectBarcodes(tus)

    val info = new ManagedMatrixInfo()

    info.addColumn(false, shortName(g), g.toString,
        s"$g$tooltipSuffix",
        ColumnFilter.emptyAbsGT, g, false, samples)
    val d = data.map(vs => Seq(buildValue(selectIdx(vs, treatedIdx))))

    (d, info)
  }

  def loadRawData(requestColumns: Seq[Group],
    reader: MatrixDBReader[E], sparseRead: Boolean,
    fullLoad: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    val packedProbes = probes.map(context.probeMap.pack)

    val samples = requestColumns.flatMap(g =>
      (if (fullLoad) g.getSamples else samplesToLoad(g)).
          toVector).distinct
    val sortedSamples = reader.sortSamples(samples.map(b => Sample(b.id)))
    val data = reader.valuesForSamplesAndProbes(sortedSamples,
        packedProbes, sparseRead, false)

    val sortedProbes = data.map(row => row(0).probe)
    val annotations = sortedProbes.map(x => new SimpleAnnotation(x)).toVector

    val cols = requestColumns.par.map(g => {
        println(g.getUnits()(0).toString())
        columnsFor(g, sortedSamples, data)
    }).seq

    val (groupedData, info) = cols.par.reduceLeft((p1, p2) => {
      val d = (p1._1 zip p2._1).map(r => r._1 ++ r._2)
      val info = p1._2.addAllNonSynthetic(p2._2)
      (d, info)
    })
    val colNames = (0 until info.numColumns()).map(i => info.columnName(i))
    val grouped = ExprMatrix.withRows(groupedData, sortedProbes, colNames)

    val ungrouped = ExprMatrix.withRows(data,
        sortedProbes, sortedSamples.map(_.sampleId))

    val baseColumns = Map() ++ (0 until info.numDataColumns()).map(i => {
      val sampleIds = info.samples(i).map(_.id).toSeq
      val sampleIdxs = sampleIds.map(i => ungrouped.columnMap.get(i)).flatten
      (i -> sampleIdxs)
    })

    new ManagedMatrix(sortedProbes, info,
      ungrouped.copyWithAnnotations(annotations),
      grouped.copyWithAnnotations(annotations),
      baseColumns,
      log2transform)
  }

  protected def log2transform: Boolean = false

  protected def mean(data: Iterable[ExprValue], presentOnly: Boolean = true) = {
    if (presentOnly) {
      ExprValue.presentMean(data, "")
    } else {
      ExprValue.allMean(data, "")
    }
  }

  final protected def selectIdx[E <: ExprValue](data: Seq[E], is: Seq[Int]) = is.map(data(_))
  final protected def javaMean[E <: ExprValue](data: Iterable[E], presentOnly: Boolean = true) = {
    val m = mean(data, presentOnly)
    new ExpressionValue(m.value, m.call, null)
  }

  protected def unitIdxs(us: Iterable[t.common.shared.sample.Unit], samples: Seq[Sample]): Seq[Int] = {
    val ids = us.flatMap(u => u.getSamples.map(_.id)).toSet
    val inSet = samples.map(s => ids.contains(s.sampleId))
    inSet.zipWithIndex.filter(_._1).map(_._2)
  }

  protected def samplesToLoad(g: Group): Array[SSample] = {
    val (tus, cus) = treatedAndControl(g)
    tus.flatMap(_.getSamples())
  }

  //TODO use schema
  protected def treatedAndControl(g: Group) =
    g.getUnits().partition(_.get(OTGAttribute.DoseLevel) != "Control")
}

trait TreatedControlBuilder[E >: Null <: ExprValue] {
  this: ManagedMatrixBuilder[E] =>
    
  type RowData = ManagedMatrix.RowData
    
  def enhancedColumns: Boolean

  protected def buildRow(raw: Seq[E],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): RowData

  protected def columnInfo(g: Group): ManagedMatrixInfo
  def colNames(g: Group): Seq[String]

  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): (Seq[RowData], ManagedMatrixInfo) = {

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
      val i = columnInfo(g)

      (rows, i)
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
  probes: Seq[String]) extends ManagedMatrixBuilder[ExprValue](reader, probes)
    with TreatedControlBuilder[ExprValue] {

  protected def buildValue(raw: RowData): ExprValue = mean(raw)
  
  override protected def shortName(g: Group): String = "Treated"
  
  protected def buildRow(raw: RowData,
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): RowData =
    Seq(buildValue(selectIdx(raw, treatedIdx)),
      buildValue(selectIdx(raw, controlIdx)))

  protected def columnInfo(g: Group) = {
    val (tus, cus) = treatedAndControl(g)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, shortName(g), colNames(g)(0),
        colNames(g)(0) + ": average of treated samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectBarcodes(tus))
    info.addColumn(false, "Control", colNames(g)(1),
        colNames(g)(1) + ": average of control samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectBarcodes(cus))
    info
  }

  def colNames(g: Group): Seq[String] =
    List(g.toString, g.toString + "(cont)")

  override protected def samplesToLoad(g: Group): Array[SSample] = {
    val (tus, cus) = treatedAndControl(g)
    if (tus.size > 1) {
      super.samplesToLoad(g)
    } else {
      //all samples
      g.getSamples()
    }
  }
}

/**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
class ExtFoldBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[PExprValue],
  probes: Seq[String]) extends ManagedMatrixBuilder[PExprValue](reader, probes)
    with TreatedControlBuilder[PExprValue] {

  import ManagedMatrix._

  protected def buildValue(raw: RowData): ExprValue = log2(javaMean(raw))
    
  protected def buildRow(raw: Seq[PExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): RowData = {
    val treatedVs = selectIdx(raw, treatedIdx)
    val first = treatedVs.head    
    val fold = buildValue(treatedVs)
    Seq(fold, new BasicExprValue(first.p, fold.call))
  }

  override protected def log2transform = true
  
  override protected def shortName(g: Group) = "Log2-fold"
  
  override protected def tooltipSuffix = ": log2-fold change of treated versus control"

  override protected def columnInfo(g: Group): ManagedMatrixInfo = {
    val tus = treatedAndControl(g)._1
    val samples = TUnit.collectBarcodes(tus)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, shortName(g), colNames(g)(0),
        colNames(g)(0) + tooltipSuffix,
        ColumnFilter.emptyAbsGT, g, false, samples)
    info.addColumn(false, "P-value", colNames(g)(1),
        colNames(g)(1) + ": p-values of treated against control",
        ColumnFilter.emptyLT, g, true,
        Array[SSample]())
    info
  }

  def colNames(g: Group) =
    List(g.toString, g.toString + "(p)")
}