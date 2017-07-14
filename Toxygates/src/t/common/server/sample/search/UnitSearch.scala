package t.common.server.sample.search

import t.viewer.server.Conversions._
import t.viewer.server.Annotations
import t.db.SampleParameter
import t.platform.ControlGroup
import org.stringtemplate.v4.ST
import t.common.shared.DataSchema
import t.common.shared.sample.Unit
import t.common.shared.sample.search.MatchCondition
import t.db.Metadata

object UnitSearch extends SearchCompanion[Unit, UnitSearch] {

  def create(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Unit, ControlGroup],
    samples: Iterable[Unit],
    searchParams: Iterable[SampleParameter]) =
      new UnitSearch(schema, metadata, condition, controlGroups, samples, searchParams)

  /**
   * Preprocess a Unit to prepare it for searching. For each search parameter,
   * computes the average value for samples in the unit, and stores it as the
   * parameter value for the unit.
   */
  //
  def preprocessSample(metadata: Metadata, searchParams: Iterable[SampleParameter]) =
    (unit: Unit) => {
      val samples = unit.getSamples
      for (param <- searchParams) {
        val paramId = param.identifier

        unit.put(paramId, try {
          var sum: Option[Double] = None
          var count: Int = 0;

          for (sample <- samples) {
            val scalaSample = asScalaSample(sample)

            sum = metadata.parameter(scalaSample, paramId) match {
              case Some("NA") => sum
              case Some(str)  => {
                count = count + 1
                sum match {
                  case Some(x) => Some(x + str.toDouble)
                  case None    => Some(str.toDouble)
                }
              }
              case None       => sum
            }
          }

          sum match {
            case Some(x) => {
              (x / count).toString()
            }
            case None    => null
          }
        } catch {
          case nf: NumberFormatException => null
        })
      }
      unit
    }

  def formControlGroups(metadata: Metadata, annotations:Annotations) = (units: Iterable[Unit]) => {
    val sampleControlGroups = annotations.controlGroups(units.flatMap(_.getSamples()), metadata)
    Map() ++
      units.map(unit => {
        unit.getSamples.headOption match {
          case Some(s) => Some(unit -> sampleControlGroups(s))
          case _ => None
        }
      }).flatten
  }
}

class UnitSearch(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Unit, ControlGroup], samples: Iterable[Unit], searchParams: Iterable[SampleParameter])
    extends AbstractSampleSearch[Unit](schema, metadata, condition,
        controlGroups, samples, searchParams)  {

  def sampleParamValue(unit: Unit, param: SampleParameter): Option[Double] = {
    try {
      Option(unit.get(param.identifier)) match {
        case Some(v) => Some(v.toDouble)
        case None    => None
      }
    } catch {
      case nf: NumberFormatException => None
    }
  }

  def time(unit: Unit): String = {
    unit.get(schema.timeParameter())
  }

  def postMatchAdjust(unit: Unit): Unit = {
    unit
  }

  def zTestSampleSize(unit: Unit): Int = {
    unit.getSamples().length
  }

  def sortObject(unit: Unit): (String, Int, Int) = {
    (unit.get("compound_name"), doseLevelMap(unit.get("dose_level")),
        exposureTimeMap(unit.get("exposure_time")))
  }
}
