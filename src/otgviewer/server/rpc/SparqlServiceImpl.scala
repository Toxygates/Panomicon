package otgviewer.server.rpc

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asJavaMap
import scala.collection.{Set => CSet}
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import Conversions.asJava
import Conversions.asJavaSample
import Conversions.convertPairs
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.OTGBConfig
import otg.OTGContext
import otg.Species.Human
import t.sparql.secondary._
import otg.sparql.Probes
import otg.sparql._
import t.sparql._
import otgviewer.client.rpc.SparqlService
import otgviewer.server.ScalaUtils.useTriplestore
import otgviewer.shared.OTGColumn
import otgviewer.shared.OTGSample
import otgviewer.shared.Pathology
import t.BaseConfig
import t.DataConfig
import t.TriplestoreConfig
import t.common.shared.DataSchema
import t.common.shared.SampleClass
import t.common.shared.sample.Annotation
import t.common.shared.sample.HasSamples
import t.common.shared.Pair
import t.db.DefaultBio
import t.sparql.Instances
import t.sparql.Triplestore
import t.sparql.TriplestoreMetadata
import t.viewer.server.Configuration
import t.viewer.server.Conversions.asSpecies
import t.viewer.server.Conversions.scAsScala
import t.viewer.shared.AType
import t.viewer.shared.Association
import otgviewer.server.ScalaUtils
import otg.sparql.Probes
import otgviewer.shared.TimeoutException
import otgviewer.shared.OTGSchema

object SparqlServiceImpl {
  var inited = false
  var affyProbes: Probes = _
  var uniprot: Uniprot = _
  var otgSamples: OTGSamples = _
  var b2rKegg: B2RKegg = _
  var chembl: ChEMBL = _
  var drugBank: DrugBank = _
  var homologene: B2RHomologene = _
  //TODO update mechanism for this
  var platforms: Map[String, Iterable[String]] = _

  def staticInit(bc: BaseConfig) = synchronized {
    if (!inited) {
      val tsCon = bc.triplestore
      val ts = tsCon.triplestore
      otgSamples = new OTGSamples(bc)
      affyProbes = new Probes(ts)
      uniprot = new LocalUniprot(ts)
      b2rKegg = new B2RKegg(ts)
      chembl = new ChEMBL()
      drugBank = new DrugBank()
      homologene = new B2RHomologene()
      platforms = affyProbes.platforms
      inited = true
    }
  }
  
  def staticDestroy() = synchronized {
    
  }
}

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class SparqlServiceImpl extends RemoteServiceServlet with SparqlService {
  import Conversions._
  import SparqlServiceImpl._
  import t.viewer.server.Conversions._
  import ScalaUtils._

  type DataColumn = t.common.shared.sample.DataColumn[OTGSample]
    
  implicit var context: OTGContext = _
  var baseConfig: BaseConfig = _
  var tgConfig: Configuration = _
 
  var instanceURI: Option[String] = None
  
  protected val schema: DataSchema = new OTGSchema()
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)    
    localInit(Configuration.fromServletConfig(config))
  }
  
  def localInit(conf: Configuration) {
	this.baseConfig = baseConfig(conf.tsConfig, conf.dataConfig)
    this.context = conf.context(baseConfig)
    this.tgConfig = conf   
    staticInit(baseConfig)
 
    if (conf.instanceName == null || conf.instanceName == "") {
      instanceURI = None
    } else {
      instanceURI = Some(Instances.defaultPrefix + "/" + conf.instanceName)
    }
  }
  
  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig = OTGBConfig(ts, data)

  override def destroy() {
    super.destroy()
  }

  @throws[TimeoutException]
  def parameterValues(sc: Array[SampleClass], parameter: String): Array[String] = {
    sc.flatMap(x => parameterValues(x, parameter)).toSet.toArray
  }
  
  @throws[TimeoutException]
  def parameterValues(sc: SampleClass, parameter: String): Array[String] = {
    //TODO when DataSchema is available here, use it instead of hardcoding shared_control
    otgSamples.attributeValues(scAsScala(sc), parameter, instanceURI).
    	filter(x => !x.startsWith("shared_control")).toArray
  }

  //TODO compound_name is a dummy parameter below
  @throws[TimeoutException]
  def samples(sc: SampleClass): Array[OTGSample] =
    otgSamples.samples(scAsScala(sc), "?compound_name", 
        List(), instanceURI).map(asJavaSample(_)).toArray

  @throws[TimeoutException]
  def samples(sc: SampleClass, param: String, 
      paramValues: Array[String]): Array[OTGSample] =
    otgSamples.samples(sc, param, paramValues, instanceURI).map(asJavaSample(_)).toArray

  @throws[TimeoutException]
  def samples(scs: Array[SampleClass], param: String, 
      paramValues: Array[String]): Array[OTGSample] =
        scs.flatMap(x => samples(x, param, paramValues)).toSet.toArray
  
  @throws[TimeoutException]
  def sampleClasses(): Array[SampleClass] = {    
	otgSamples.sampleClasses(instanceURI).map(x => 
	  new SampleClass(new java.util.HashMap(asJavaMap(x)))
	  ).toArray
  }
      
  import t.common.shared.{Unit => TUnit}

  @throws[TimeoutException]
  def units(sc: SampleClass,  
      param: String, paramValues: Array[String]): Array[Pair[TUnit, TUnit]] = {

    val majorParam = schema.majorParameter()
    //Ensure shared control is always included, if possible
    val useParamValues = if (param == majorParam) {
      val allMajors = 
        otgSamples.attributeValues(scAsScala(sc), majorParam, instanceURI)        
      val shared = allMajors.filter(schema.isMajorParamSharedControl(_))
      (shared.toSeq ++ paramValues.toSeq)
    } else {
      paramValues.toSeq
    }
    
    //TODO rethink how to use batch here
    val ss = otgSamples.samples(sc, param, useParamValues, instanceURI).    
    		groupBy(x =>( 
    		    x.sampleClass(schema.timeParameter()), 
    		    x.sampleClass.get("control_group")))
    
    //For each unit of treated samples inside a control group, all
    //control samples in that group are assigned as control.
    var r = Vector[Pair[TUnit, TUnit]]()
    for (((t, cg), samples) <- ss;
    		treatedControl = samples.partition(s => !schema.isSelectionControl(s.sampleClass) )) {
    	val treatedUnits = treatedControl._1.map(asJavaSample).
    			groupBy(_.sampleClass.asUnit(schema))
    			
    	val cus = treatedControl._2.map(asJavaSample)
    	val cu = if (!cus.isEmpty) {
    	  new TUnit(cus.head.sampleClass().asUnit(schema),    	
    	    cus.toArray)
    	} else {
    	  new TUnit(sc.asUnit(schema), Array())
    	}
    	
    	r ++= treatedUnits.map(u => new Pair(
    	    new TUnit(u._1, u._2.toArray), cu))
    	if (!cu.getSamples().isEmpty) {
    	  r :+= new Pair(cu, null: TUnit) //add this as a pseudo-treated unit by itself
    	}
    }    
    r.toArray
  }
  
  def units(scs: Array[SampleClass], param: String, 
      paramValues: Array[String]): Array[Pair[TUnit, TUnit]] = {
    scs.flatMap(units(_, param, paramValues))
  }
    
//  val orderedTimes = TimesDoses.allTimes.toList 

  @throws[TimeoutException]
  def probes(columns: Array[OTGColumn]): Array[String] = {
    val samples = columns.flatMap(_.getSamples)
    val metadata = new TriplestoreMetadata(otgSamples, instanceURI)    
    val usePlatforms = samples.map(s => metadata.parameter(
        t.db.Sample(s.getCode), "platform_id")
        ).toSet
    usePlatforms.toVector.flatMap(platforms).toArray
  }
  
  @throws[TimeoutException]
  def pathologies(barcode: OTGSample): Array[Pathology] =
    otgSamples.pathologies(barcode.getCode).map(asJava(_)).toArray

  @throws[TimeoutException]
  def pathologies(column: OTGColumn): Array[Pathology] =
    column.getSamples.flatMap(x => otgSamples.pathologies(x.getCode)).map(asJava(_))

  @throws[TimeoutException]
  def annotations(barcode: OTGSample): Annotation = 
    asJava( otgSamples.annotations(barcode.getCode, List(), instanceURI) )
    
  //TODO get these from schema, etc.
  @throws[TimeoutException]
  def annotations(column: HasSamples[OTGSample], importantOnly: Boolean = false): Array[Annotation] = {	  
	  val keys = if (importantOnly) {
	    List("Dose", "Dose unit", "Dose level", "Exposure time", "Administration route")	    
	  } else {
	    Nil //fetch everything
	  }
	  column.getSamples.map(x => otgSamples.annotations(x.getCode, keys, instanceURI)).map(asJava(_))
  }

  @throws[TimeoutException]
  def pathways(sc: SampleClass, pattern: String): Array[String] =
    b2rKegg.forPattern(pattern, sc).toArray

  //TODO: return a map instead
  @throws[TimeoutException]
  def geneSyms(probes: Array[String]): Array[Array[String]] = {
    val ps = probes.map(p => Probe(p))
    val attrib = affyProbes.withAttributes(ps)
    probes.map(pi => attrib.find(_.identifier == pi).
      map(_.symbolStrings.toArray).getOrElse(Array()))
  }

  @throws[TimeoutException]
  def probesForPathway(sc: SampleClass, pathway: String): Array[String] = {    
    val geneIds = b2rKegg.geneIds(pathway).map(Gene(_))
    println("Probes for " + geneIds.size + " genes")
    val probes = affyProbes.forGenes(geneIds).toArray
    val pmap = context.unifiedProbes //TODO
    probes.map(_.identifier).filter(pmap.isToken).toArray
  }
  
  @throws[TimeoutException]
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

  @throws[TimeoutException]
  def goTerms(pattern: String): Array[String] =
    affyProbes.goTerms(pattern).map(_.name).toArray

  @throws[TimeoutException]
  def probesForGoTerm(goTerm: String): Array[String] = {
    val pmap = context.unifiedProbes 
    affyProbes.forGoTerm(GOTerm("", goTerm)).map(_.identifier).filter(pmap.isToken).toArray
  }

  import scala.collection.{ Map => CMap, Set => CSet }

  //TODO refactor this; instead of gathering all column logic here,
  //implement each column separately in a way that incorporates
  //both presentation and lookup code
  
  @throws[TimeoutException]
  def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] = {
    val probes = affyProbes.withAttributes(_probes.map(Probe(_)))

    def queryOrEmpty[T <: Triplestore](c: T, f: T => BBMap): BBMap = {
      val emptyVal = CSet(DefaultBio("error", "(Timeout or error)"))
      useTriplestore(c, f,
        Map() ++ probes.map(p => (Probe(p.identifier) -> emptyVal)))
    }
    val proteins = toBioMap(probes, (_: Probe).proteins)

//    val sp = asSpecies(sc)
    //orthologous proteins if needed
    val oproteins = if ((types.contains(AType.Chembl) || types.contains(AType.Drugbank) || types.contains(AType.OrthProts))
//      && (sp != Human)
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
      val expected = otgSamples.compounds(sc, instanceURI).map(Compound.make(_))

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
    
    //OSA genes if needed
    //TODO move this and other tritigate specific code to subclass
    val osaGenes = if (types.contains(AType.EnsemblOSA) || types.contains(AType.KEGGOSA)) {
        affyProbes.osaGenes(probes)
    } else {
      emptyMMap[Probe, DefaultBio]()
    }

    import Association._
    def lookupFunction(t: AType): BBMap = t match {
      case x: AType.Chembl.type => queryOrEmpty(chembl, getTargeting(_: ChEMBL))
      case x: AType.Drugbank.type => queryOrEmpty(drugBank, getTargeting(_: DrugBank))
      case x: AType.Uniprot.type => proteins
      case x: AType.OrthProts.type => oproteins
      case x: AType.GOMF.type => queryOrEmpty(affyProbes, 
          (a: Probes) => a.mfGoTerms(probes))        
      case x: AType.GOBP.type => queryOrEmpty(affyProbes, 
          (a: Probes) => a.bpGoTerms(probes))        
      case x: AType.GOCC.type => queryOrEmpty(affyProbes, 
          (a: Probes) => a.ccGoTerms(probes))        
      case x: AType.GO.type => queryOrEmpty(affyProbes, 
          (a: Probes) => a.goTerms(probes))
      case x: AType.Homologene.type => queryOrEmpty(homologene,
        (c: B2RHomologene) => toBioMap(probes, (_: Probe).genes) combine
          c.homologousGenes(probes.flatMap(_.genes)))
      case x: AType.KEGG.type =>
        val sp = asSpecies(sc)
        queryOrEmpty(b2rKegg,
        (c: B2RKegg) => toBioMap(probes, (_: Probe).genes) combine
          c.forGenes(probes.flatMap(_.genes), sp))
      case x: AType.Enzymes.type =>
        val sp = asSpecies(sc)
        queryOrEmpty(b2rKegg,
        (c: B2RKegg) => c.enzymes(probes.flatMap(_.genes), sp))
      case x: AType.EnsemblOSA.type => osaGenes         
      case x: AType.KEGGOSA.type =>         
        val osaAll = osaGenes.flatMap(_._2)
        queryOrEmpty(b2rKegg,
        (c: B2RKegg) => osaGenes combine c.forGenesOSA(osaAll))
      case x: AType.Contigs.type => queryOrEmpty(affyProbes, 
          (a: Probes) => a.contigs(probes))             
      case x: AType.SNPs.type =>  queryOrEmpty(affyProbes, 
          (a: Probes) => a.SNPs(probes))
    }

    def standardMapping(m: BBMap): MMap[String, (String, String)] =
      m.mapKValues(_.identifier).mapMValues(p => (p.name, p.identifier))

    val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)))).seq

    m1.map(p => new Association(p._1, convertPairs(p._2))).toArray
  }

  @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[String] = {    
      affyProbes.probesForPartialSymbol(partialName, sc).map(_.identifier).toArray
  }

}