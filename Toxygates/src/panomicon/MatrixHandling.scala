package panomicon

import t.Context
import t.db.{Sample, SampleId}
import t.model.sample.CoreParameter
import t.model.sample.CoreParameter.{ControlTreatment, Treatment}
import t.server.viewer.{AssociationMasterLookup, Configuration}
import t.server.viewer.Conversions.asJavaSample
import t.server.viewer.matrix.{ExpressionRow, ManagedMatrix, MatrixController, PageDecorator}
import t.shared.common.{AType, ValueType}
import t.shared.viewer.{Association, ManagedMatrixInfo}
import t.sparql.{SampleClassFilter, SampleFilter}
import t.util.LRUCache
import ujson.Value
import upickle.default.writeJs

/**
 * Routines that support matrix loading requests.
 * @param sampleFilter allows filtering all data requests by batches, datasets, and/or instances.
 */
class MatrixHandling(context: Context, sampleFilter: SampleFilter) {
  lazy val associationLookup = new AssociationMasterLookup(context, sampleFilter)

  def filledGroups(matParams: json.MatrixParams) = {
    val sampleIds = matParams.groups.flatMap(_.sampleIds)
    //This will get all attributes and also filter samples that we do not have access to.
    //Accordingly, not all requested sample IDs may be present in fullSamples.

    val fullSamples = Map.empty ++
      context.sampleStore.withRequiredAttributes(SampleClassFilter(), sampleFilter, sampleIds)().map(
        s => (s.sampleId -> s))
    matParams.groups.flatMap(g => checkAndFillGroup(g.name, g.sampleIds.flatMap(s => fullSamples.get(s))))
  }

  def loadMatrix(matParams: json.MatrixParams, valueType: ValueType): MatrixController = {
    val groups = filledGroups(matParams)
    val controller = MatrixController(context, groups, matParams.probes, valueType)
    val matrix = controller.managedMatrix
    matParams.applyFilters(matrix)
    matParams.applySorting(matrix)
    controller
  }

  def columnInfoToJS(info: ManagedMatrixInfo): Seq[Map[String, Value]] = {
    (0 until info.numColumns()).map(i => {
      Map("name" -> writeJs(info.columnName(i)),
        "parent" -> writeJs(info.parentColumnName(i)),
        "shortName" -> writeJs(info.shortColumnName(i)),
        "hint" -> writeJs(info.columnHint(i)),
        "samples" -> writeJs(info.samples(i).map(s => s.id()))
      )
    })
  }

  import t.shared.common.sample.{Unit => TUnit}
  def unitForTreatment(sf: SampleFilter, treatment: String): Option[TUnit] = {
    val samples = context.sampleStore.sampleQuery(SampleClassFilter(Map(Treatment -> treatment)), sf)()
    if (samples.nonEmpty) {
      Some(new TUnit(samples.head.sampleClass, samples.map(asJavaSample).toArray))
    } else {
      None
    }
  }

  /**
   * By using the sample treatment ID, ensure that the group contains all the available samples for a given treatment.
   * @return A group with filled samples for the treatment if available, or None if the group could not be validated.
   */
  def checkAndFillGroup(name: String, group: Seq[Sample]): Option[t.shared.common.sample.Group] = {
    if (group.isEmpty) {
      return None
    }
    val batchURI = group.head.apply(CoreParameter.Batch)
    val sf = sampleFilter.copy(batchURI = Some(batchURI))

    val treatedTreatments = group.map(s => s.sampleClass(Treatment)).distinct
    val controlTreatments = group.map(s => s.sampleClass(ControlTreatment)).distinct

    //Note: querying treated/control separately leads to one extra sparql query - can
    //probably be optimised away
    val treatedUnits = treatedTreatments.flatMap(t => unitForTreatment(sf, t))
    val controlUnits = controlTreatments.flatMap(t => unitForTreatment(sf, t))

    Some(new t.shared.common.sample.Group(name, treatedUnits.toArray, controlUnits.toArray))
  }

  def rowsToJS(rows: Seq[ExpressionRow], matrixInfo: ManagedMatrixInfo): Seq[Map[String, Value]] = {
    rows.map(r => Map(
      "probe" -> writeJs(r.probe),
      "probeTitles" -> writeJs(r.probeTitles),
      "geneIds" -> writeJs(r.geneIds.map(_.toInt)),
      "geneSymbols" -> writeJs(r.geneSymbols),
      "expression" -> writeJs(Map() ++ ((0 until matrixInfo.numColumns)
        .map(matrixInfo.columnName(_)) zip r
        .values.map(v => writeJs(v.value))))
    ))
  }

  /**
   * Cache the most recently used matrices in memory
   */
  private val matrixCache = new LRUCache[(json.MatrixParams, ValueType), MatrixController](10)

  def findOrLoadMatrix(params: json.MatrixParams, valueType: ValueType, offset: Int, pageSize: Int):
    (ManagedMatrix, Seq[ExpressionRow]) = {
    val key = (params, valueType)
    val controller = matrixCache.get(key) match {
      case Some(mat) => mat
      case _ =>
        val c = loadMatrix(key._1, key._2)
        matrixCache.insert(key, c)
        c
    }

    val matrix = controller.managedMatrix
    val pages = new PageDecorator(context, controller)
    val page = pages.getPageView(offset, pageSize, true)
    (matrix, page)
  }

  def association(atype: AType, probes: Array[String], representativeSampleId: SampleId,
                  limit: Int): Association = {
    val sampleData = context.sampleStore.withRequiredAttributes(SampleClassFilter(),
      sampleFilter, Seq(representativeSampleId))().
      headOption.getOrElse(throw new Exception(s"No such sample $representativeSampleId"))

    val assoc = associationLookup.doLookup(sampleData.sampleClass, Array(atype),
      probes, limit).headOption.getOrElse(throw new Exception("Unable to get association data"))

    assoc
  }
}