package otgviewer.server.rpc

import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.collection.{Set => CSet}
import scala.collection.{Set => CSet}
import scala.collection.JavaConversions._
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import t.common.shared.Pair
import t.common.shared.sample.Annotation
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.DefaultBio
import otg.Species._
import otg.OTGContext
import otg.sparql._
import otgviewer.client.rpc.SparqlService
import otgviewer.server._
import otgviewer.shared.AType
import otgviewer.shared.Association
import otgviewer.shared.Barcode
import otgviewer.shared.BUnit
import otgviewer.shared.BarcodeColumn
import otgviewer.shared.DataFilter
import otgviewer.shared.Pathology
import otgviewer.shared.TimesDoses
import t.common.shared.sample.HasSamples
import otg.sparql.AffyProbes
import otg.sparql.LocalUniprot
import otg.sparql.DrugBank
import otg.sparql.ChEMBL
import t.sparql.Triplestore
import t.BaseConfig
import t.viewer.shared.SampleClass

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class SparqlServiceImpl extends RemoteServiceServlet with SparqlService {
  import Conversions._
  import t.viewer.server.Conversions._
  import UtilsS._
  import Assocations._
  import CommonSPARQL._

  type DataColumn = t.common.shared.sample.DataColumn[Barcode]
  
  implicit var context: OTGContext = _
  var baseConfig: BaseConfig = _
  var tgConfig: Configuration = _
  var affyProbes: AffyProbes = _
  var uniprot: Uniprot = _
  var otgSamples: OTGSamples = _
  var b2rKegg: B2RKegg = _
  var chembl: ChEMBL = _
  var drugBank: DrugBank = _
  var homologene: B2RHomologene = _
  //TODO update mechanism for this
  var platforms: Map[String, Iterable[String]] = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))
  }

  def localInit(conf: Configuration) {
    this.context = conf.context
    this.tgConfig = conf   
    this.baseConfig = conf.baseConfig
    
    val ts = baseConfig.triplestore.triplestore
    otgSamples = new OTGSamples(ts)
    affyProbes = new AffyProbes(ts)
    uniprot = new LocalUniprot(ts) 
    b2rKegg = new B2RKegg(ts)
    chembl = new ChEMBL()
    drugBank = new DrugBank()
    homologene = new B2RHomologene()
    platforms = affyProbes.platforms
  }

  override def destroy() {
    affyProbes.close()
    otgSamples.close()
    uniprot.close()
    b2rKegg.close()
    chembl.close()
    drugBank.close()
    homologene.close()   
    super.destroy()
  }

  def compounds(sc: SampleClass): Array[String] = {
    //TODO don't have a special case for shared_control here
    otgSamples.compounds(scAsScala(sc)).toArray
  }

  val orderedDoses = List("Control", "Low", "Middle", "High")
  def doseLevels(sc: SampleClass): Array[String] = {
    val r = otgSamples.doseLevels(sc).toArray
    r.sortWith((d1, d2) => orderedDoses.indexOf(d1) < orderedDoses.indexOf(d2))
  }

  def samples(sc: SampleClass): Array[Barcode] =
    otgSamples.samples(scAsScala(sc), List()).map(asJava(_)).toArray

  def samples(sc: SampleClass, compounds: Array[String]): Array[Barcode] =
    otgSamples.samples(sc, compounds).map(asJava(_)).toArray

  def sampleClasses(): Array[t.viewer.shared.SampleClass] = {    
	otgSamples.sampleClasses().map(x => 
	  new SampleClass(new java.util.HashMap(asJavaMap(x)))
	  ).toArray
  }
      
  def units(sc: SampleClass, compounds: Array[String]): Array[BUnit] = {
    val bcs = otgSamples.samples(sc, compounds).map(asJava(_))
    val g = bcs.groupBy(x => new BUnit(x, sc))
    for ((k, v) <- g) {
      k.setSamples(v.toArray)
    }
    g.keySet.toArray
  }
    
  val orderedTimes = TimesDoses.allTimes.toList
  def times(sc: SampleClass): Array[String] = {
    val r = otgSamples.times(sc).toArray
    r.sortWith((t1, t2) => orderedTimes.indexOf(t1) < orderedTimes.indexOf(t2))
  }

//  def probes(filter: DataFilter): Array[String] =
//    context.unifiedProbes.tokens.toArray //TODO filtering    

  def probes(columns: Array[BarcodeColumn]): Array[String] = {
    val samples = columns.flatMap(_.getSamples)
    val metadata = new TriplestoreMetadata(otgSamples, None)    
    val usePlatforms = samples.map(s => metadata.parameter(
        otg.Sample(s.getCode), "platform_id")
        ).toSet
    usePlatforms.toVector.flatMap(platforms).toArray
  }
  
  def pathologies(barcode: Barcode): Array[Pathology] =
    otgSamples.pathologies(barcode.getCode).map(asJava(_)).toArray

  def pathologies(column: BarcodeColumn): Array[Pathology] =
    column.getSamples.flatMap(x => otgSamples.pathologies(x.getCode)).map(asJava(_))

  def annotations(barcode: Barcode): Annotation = asJava(otgSamples.annotations(barcode.getCode))
  def annotations(column: HasSamples[Barcode], importantOnly: Boolean = false): Array[Annotation] = {	  
	  val keys = if (importantOnly) {
	    if (tgConfig.applicationClass == ApplicationClass.Adjuvant) {
	    	List("Dose", "Dose unit", "Dose level", "Exposure time", "Administration route")
	    } else {	      
	    	List("Dose", "Dose unit", "Dose level", "Exposure time")
	    }
	  } else {
	    Nil //fetch everything
	  }
	  column.getSamples.map(x => otgSamples.annotations(x.getCode, keys)).map(asJava(_))
  }

  def pathways(sc: SampleClass, pattern: String): Array[String] =
    b2rKegg.forPattern(pattern, sc).toArray

  //TODO: return a map instead
  def geneSyms(probes: Array[String]): Array[Array[String]] = {
    val ps = probes.map(p => Probe(p))
    val attrib = affyProbes.withAttributes(ps)
    probes.map(pi => attrib.find(_.identifier == pi).
      map(_.symbolStrings.toArray).getOrElse(Array()))
  }

  def probesForPathway(sc: SampleClass, pathway: String): Array[String] = {
    val sp = asSpecies(sc)
    val geneIds = b2rKegg.geneIds(pathway, sp).map(Gene(_))
    println("Probes for " + geneIds.size + " genes")
    val probes = affyProbes.forGenes(geneIds).toArray
    val pmap = context.unifiedProbes //TODO
    probes.map(_.identifier).filter(pmap.isToken).toArray
  }

  def probesTargetedByCompound(sc: SampleClass, compound: String, service: String,
    homologous: Boolean): Array[String] = {
    val cmp = Compound.make(compound)
    val sp = asSpecies(sc)
    val proteins = service match {
      case "CHEMBL" => chembl.targetsFor(cmp, if (homologous) { null } else { sp })
      case "DrugBank" => drugBank.targetsFor(cmp)
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    }
    val pbs = if (homologous) {
      val oproteins = uniprot.orthologsFor(proteins, sp).values.flatten.toSet
      affyProbes.forUniprots(oproteins ++ proteins)
      //      OTGOwlim.probesForEntrezGenes(genes)
    } else {
      affyProbes.forUniprots(proteins)
    }
    val pmap = context.unifiedProbes //TODO context.probes(filter)
    pbs.toSet.map((p: Probe) => p.identifier).filter(pmap.isToken).toArray
  }

  def goTerms(pattern: String): Array[String] =
    affyProbes.goTerms(pattern).map(_.name).toArray

  def probesForGoTerm(goTerm: String): Array[String] = {
    val pmap = context.unifiedProbes 
    affyProbes.forGoTerm(GOTerm("", goTerm)).map(_.identifier).filter(pmap.isToken).toArray
  }

  import scala.collection.{ Map => CMap, Set => CSet }

  def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] = {
    val probes = affyProbes.withAttributes(_probes.map(Probe(_)))
    val sp = asSpecies(sc)

    def queryOrEmpty[T <: Triplestore](c: T, f: T => BBMap): BBMap = {
      val emptyVal = CSet(DefaultBio("error", "(Timeout or error)"))
      useTriplestore(c, f,
        Map() ++ probes.map(p => (Probe(p.identifier) -> emptyVal)))
    }

    val proteins = toBioMap(probes, (_: Probe).proteins)

    //orthologous proteins if needed
    val oproteins = if ((types.contains(AType.Chembl) || types.contains(AType.Drugbank) || types.contains(AType.OrthProts))
      && (sp != Human)
      && false // Not used currently due to performance issues!
      ) {
      // This always maps to Human proteins as they are assumed to contain the most targets
      val r = proteins combine ((ps: Iterable[Protein]) => uniprot.orthologsFor(ps, Human))
      r
    } else {
      emptyMMap[Probe, Protein]()
    }
    println(oproteins.allValues.size + " oproteins")

    def getTargeting(from: CompoundTargets): MMap[Probe, Compound] = {
      val expected = otgSamples.compounds(sc).map(Compound.make(_))

      //strictly orthologous
      val oproteinVs = oproteins.allValues.toSet -- proteins.allValues.toSet
      val allProteins = proteins union oproteins
      val allTargets = from.targetingFor(allProteins.allValues, expected)

      allProteins combine allTargets.map(x => if (oproteinVs.contains(x._1)) {
        (x._1 -> x._2.map(c => c.copy(name = c.name + " (inf)")))
      } else {
        x
      })
    }

    import Association._
    def lookupFunction(t: AType): BBMap = t match {
      case x: AType.Chembl.type => queryOrEmpty(chembl, getTargeting(_: ChEMBL))
      case x: AType.Drugbank.type => queryOrEmpty(drugBank, getTargeting(_: DrugBank))
      case x: AType.Uniprot.type => proteins
      case x: AType.OrthProts.type => oproteins
      case x: AType.GOMF.type => queryOrEmpty(affyProbes, 
          (a: AffyProbes) => a.mfGoTerms(probes))        
      case x: AType.GOBP.type => queryOrEmpty(affyProbes, 
          (a: AffyProbes) => a.bpGoTerms(probes))        
      case x: AType.GOCC.type => queryOrEmpty(affyProbes, 
          (a: AffyProbes) => a.ccGoTerms(probes))        
      case x: AType.Homologene.type => queryOrEmpty(homologene,
        (c: B2RHomologene) => toBioMap(probes, (_: Probe).genes) combine
          c.homologousGenes(probes.flatMap(_.genes)))
      case x: AType.KEGG.type => queryOrEmpty(b2rKegg,
        (c: B2RKegg) => toBioMap(probes, (_: Probe).genes) combine
          c.forGenes(probes.flatMap(_.genes), sp))
      case x: AType.Enzymes.type => queryOrEmpty(b2rKegg,
        (c: B2RKegg) => c.enzymes(probes.flatMap(_.genes), sp))
    }

    def standardMapping(m: BBMap): MMap[String, (String, String)] =
      m.mapKValues(_.identifier).mapMValues(p => (p.name, p.identifier))

    val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)))).seq

    m1.map(p => new Association(p._1, convertPairs(p._2))).toArray
  }

  def geneSuggestions(sc: SampleClass, partialName: String): Array[String] = {    
      affyProbes.probesForPartialSymbol(partialName, sc).map(_.identifier).toArray
  }

}