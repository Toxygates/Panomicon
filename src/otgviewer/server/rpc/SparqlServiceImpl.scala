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
import t.viewer.server.ApplicationClass
import t.viewer.server.Configuration
import t.viewer.server.Conversions.asSpecies
import t.viewer.server.Conversions.scAsScala
import t.viewer.shared.AType
import t.viewer.shared.Association
import otgviewer.server.ScalaUtils
import otg.sparql.Probes

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class SparqlServiceImpl extends RemoteServiceServlet with SparqlService {
  import Conversions._
  import t.viewer.server.Conversions._
  import ScalaUtils._

  type DataColumn = t.common.shared.sample.DataColumn[OTGSample]
  
  /*
   * TODO: most of the state here should be static, shared between clients.
   * Currently I believe it is created anew for every thread/session.
   */
  
  implicit var context: OTGContext = _
  var baseConfig: BaseConfig = _
  var tgConfig: Configuration = _
  var affyProbes: Probes = _
  var uniprot: Uniprot = _
  var otgSamples: OTGSamples = _
  var b2rKegg: B2RKegg = _
  var chembl: ChEMBL = _
  var drugBank: DrugBank = _
  var homologene: B2RHomologene = _
  //TODO update mechanism for this
  var platforms: Map[String, Iterable[String]] = _
  var instanceURI: Option[String] = None
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))
  }
  
  def localInit(conf: Configuration) {
	this.baseConfig = baseConfig(conf.tsConfig, conf.dataConfig)
    this.context = conf.context(baseConfig)
    this.tgConfig = conf   
    
    val tsCon = baseConfig.triplestore
    val ts = tsCon.triplestore
    otgSamples = new OTGSamples(baseConfig)
    affyProbes = new Probes(ts)
    uniprot = new LocalUniprot(ts) 
    b2rKegg = new B2RKegg(ts)
    chembl = new ChEMBL()
    drugBank = new DrugBank()
    homologene = new B2RHomologene()
    platforms = affyProbes.platforms
    if (conf.instanceName == null || conf.instanceName == "") {
      instanceURI = None
    } else {
      instanceURI = Some(Instances.defaultPrefix + "/" + conf.instanceName)
    }
  }
  
  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig = OTGBConfig(ts, data)

  //TODO is this handled properly?
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

  def parameterValues(sc: Array[SampleClass], parameter: String): Array[String] = {
    sc.flatMap(x => parameterValues(x, parameter)).toSet.toArray
  }
  
  def parameterValues(sc: SampleClass, parameter: String): Array[String] = {
    //TODO when DataSchema is available here, use it instead of hardcoding shared_control
    otgSamples.attributeValues(scAsScala(sc), parameter, instanceURI).
    	filter(_ != "shared_control").toArray
  }

  //TODO compound_name is a dummy parameter below
  def samples(sc: SampleClass): Array[OTGSample] =
    otgSamples.samples(scAsScala(sc), "?compound_name", 
        List(), instanceURI).map(asJavaSample(_)).toArray

  def samples(sc: SampleClass, param: String, 
      paramValues: Array[String]): Array[OTGSample] =
    otgSamples.samples(sc, param, paramValues, instanceURI).map(asJavaSample(_)).toArray

  def samples(scs: Array[SampleClass], param: String, 
      paramValues: Array[String]): Array[OTGSample] =
        scs.flatMap(x => samples(x, param, paramValues)).toSet.toArray
    
  def sampleClasses(): Array[SampleClass] = {    
	otgSamples.sampleClasses(instanceURI).map(x => 
	  new SampleClass(new java.util.HashMap(asJavaMap(x)))
	  ).toArray
  }
      
  import t.common.shared.{Unit => TUnit}
  //TODO don't pass schema from client
  def units(sc: SampleClass, schema: DataSchema, 
      param: String, paramValues: Array[String]): Array[Pair[TUnit, TUnit]] = {

    val majorParam = schema.majorParameter()
    val sharedControl = schema.majorParamSharedControlValue()
    //Ensure shared control is always included, if possible
    val useParamValues = if (param == majorParam && sharedControl != null) {
      sharedControl :: paramValues.toList
    } else {
      paramValues.toList
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
    
//  val orderedTimes = TimesDoses.allTimes.toList 

  def probes(columns: Array[OTGColumn]): Array[String] = {
    val samples = columns.flatMap(_.getSamples)
    val metadata = new TriplestoreMetadata(otgSamples, instanceURI)    
    val usePlatforms = samples.map(s => metadata.parameter(
        t.db.Sample(s.getCode), "platform_id")
        ).toSet
    usePlatforms.toVector.flatMap(platforms).toArray
  }
  
  def pathologies(barcode: OTGSample): Array[Pathology] =
    otgSamples.pathologies(barcode.getCode).map(asJava(_)).toArray

  def pathologies(column: OTGColumn): Array[Pathology] =
    column.getSamples.flatMap(x => otgSamples.pathologies(x.getCode)).map(asJava(_))

  def annotations(barcode: OTGSample): Annotation = 
    asJava( otgSamples.annotations(barcode.getCode, List(), instanceURI) )
    
  //TODO get these from schema, etc.
  def annotations(column: HasSamples[OTGSample], importantOnly: Boolean = false): Array[Annotation] = {	  
	  val keys = if (importantOnly) {
	    if (tgConfig.applicationClass == ApplicationClass.Adjuvant) {
	    	List("Dose", "Dose unit", "Dose level", "Exposure time", "Administration route")
	    } else {	      
	    	List("Dose", "Dose unit", "Dose level", "Exposure time")
	    }
	  } else {
	    Nil //fetch everything
	  }
	  column.getSamples.map(x => otgSamples.annotations(x.getCode, keys, instanceURI)).map(asJava(_))
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
    val geneIds = b2rKegg.geneIds(pathway).map(Gene(_))
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