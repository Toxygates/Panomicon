import t.model._
import t.db.Sample
import t.manager._

/*
 Imports and contexts for inspecting the database interactively.
 Load in the Scala REPL with e.g.
 scala> :load dbRepl.sh
 */
 
val bc = Manager.getBaseConfig
implicit val c = Manager.initContext(bc)
val batchMan = new BatchManager(c)
implicit val mc = batchMan.matrixContext
val dbReader = bc.data.absoluteDBReader
val probes = dbReader.sortProbes(mc.probeMap.keys)
dbReader.valuesInSample(Sample("@51105600762874011613412645738009", new SampleClass()), probes, false)
