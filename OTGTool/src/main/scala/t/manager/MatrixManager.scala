package t.manager

import t.Context
import t.db._
import t.sparql.{BatchStore, SampleClassFilter}

import scala.collection.JavaConverters._

/**
 * Mid-level copy tool for copying matrix data between different formats.
 * Leaves other databases (such as sample/probe index, series data) untouched.
 */
object MatrixManager extends ManagerTool {

  def apply(args: Seq[String])(implicit context: Context): Unit = {

    def config = context.config
    def factory = context.factory

    def samplesInBatch(batch: String) = {
      val sf = t.sparql.SampleFilter(None, Some(BatchStore.packURI(batch)))
      context.sampleStore.samples(SampleClassFilter(), sf)
    }

    def matcopy[E >: Null <: ExprValue](from: MatrixDBReader[E],
                                        batch: Option[String],
                                        getDB: () => MatrixDBWriter[PExprValue],
                                        formVal: E => FoldPExpr,
                                        label: String)(implicit mat: MatrixContext) {
      val allProbes = from.sortProbes(mat.probeMap.keys)
      def allSamples = from.sortSamples(mat.sampleMap.tokens.map(Sample(_)))

      val useSamples = batch.map(samplesInBatch).getOrElse(allSamples)

      for (ss <- useSamples.grouped(50)) {
        val vs = from.valuesInSamples(ss, allProbes, true)
        val svs = Map() ++ (ss zip vs)
        val raw = new ColumnExpressionData {
          val samples = ss.toArray
          def probes = allProbes.map(mat.probeMap.unpack)
          def data(s: Sample) = Map() ++
            svs(s).filter(!_.isPadding).map(v => v.probe -> formVal(v))
        }
        val t = new SimpleValueInsert(getDB, raw).
          insert(s"$label")
        TaskRunner.runThenFinally(t)(())
        println(s"$ss ($label)")
      }
    }

    if (args.size < 1) {
      showHelp()
    } else {
      args(0) match {
        case "copy" =>
          val todir = require(stringOption(args, "-toDir"),
            "Please specify a destination directory with -toDir")
          val tsconfig = config.triplestoreConfig
          val dataParams = System.getenv().asScala + ("T_DATA_DIR" -> todir)
          val toDConfig = Manager.getDataConfig(dataParams)
          val toBConfig = Manager.makeBaseConfig(tsconfig, toDConfig)
          //If the batch is specified, only that batch will be copied.
          //Otherwise, all batches are copied.
          val batch = stringOption(args, "-batch")

          implicit val mat = context.matrix

          matcopy[PExprValue](config.data.foldsDBReader(mat),
              batch,
            () => toDConfig.extWriter(toDConfig.foldDb),
            v => (v.value, v.call, v.p),
            "Insert folds")

          matcopy[PExprValue](mat.absoluteDBReader,
              batch,
            () => toDConfig.extWriter(toDConfig.exprDb),
            v => (v.value, v.call, 0.0),
            "Insert absolute values")

        case _ => showHelp()
      }
    }
  }

  def showHelp(): Unit = {
    println("Please specify a command (copy)")
  }
}
