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
import otg.sparql._
import t.sparql._
import otgviewer.client.rpc.SparqlService
import otgviewer.server.ScalaUtils.gracefully
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
import otgviewer.shared.TimeoutException
import otgviewer.shared.OTGSchema
import t.platform.Probe
import otg.sparql.Probes
import t.common.shared.Dataset

object SparqlServiceImpl {
  //TODO consider moving these to an application-wide SparqlContext or similar
  
  var inited = false  
  var uniprot: Uniprot = _
  var otgSamples: OTGSamples = _
  var b2rKegg: B2RKegg = _
  var chembl: ChEMBL = _
  var drugBank: DrugBank = _
  var homologene: B2RHomologene = _
  //TODO update mechanism for this
  var platforms: Map[String, Iterable[String]] = _
  
  def staticInit(c: otg.Context) = synchronized {
    val bc = c.config
    val f = c.factory
    if (!inited) {
      val tsCon = bc.triplestore
      val ts = tsCon.triplestore
      otgSamples = new OTGSamples(bc)      
      uniprot = new LocalUniprot(ts)
      b2rKegg = new B2RKegg(ts)
      chembl = new ChEMBL()
      drugBank = new DrugBank()
      homologene = new B2RHomologene()
      platforms = c.probes.platforms
      inited = true
    }
  }
  
  def staticDestroy() = synchronized {
    
  }
}

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 * 
 * TODO: extract superclass
 */
class SparqlServiceImpl extends OTGServiceServlet with SparqlService {
  import Conversions._
  import SparqlServiceImpl._
  import t.viewer.server.Conversions._
  import ScalaUtils._

  type DataColumn = t.common.shared.sample.DataColumn[OTGSample]
 
  var instanceURI: Option[String] = None
  
  protected def probeStore: Probes = context.probes
  
  override def localInit(conf: Configuration) {
	  super.localInit(conf)
    staticInit(context)
 
    if (conf.instanceName == null || conf.instanceName == "") {
      instanceURI = None
    } else {
      instanceURI = Some(Instances.defaultPrefix + "/" + conf.instanceName)
    }
  }
  
  def datasets(): Array[Dataset] = {
   Array() 
  }
  
  def chooseDatasets(ds: Array[Dataset]): Unit = {
    
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
  
  def samplesById(ids: Array[String]): Array[OTGSample] = 
    otgSamples.samples(new t.sparql.SampleClass(), "id", 
        ids, instanceURI).map(asJavaSample(_)).toArray 

  //TODO compound_name is a dummy parameter below
  @throws[TimeoutException]
  def samples(sc: SampleClass): Array[OTGSample] =
    otgSamples.samples(scAsScala(sc), "compound_name", 
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
    val attrib = probeStore.withAttributes(ps)
    probes.map(pi => attrib.find(_.identifier == pi).
      map(_.symbolStrings.toArray).getOrElse(Array()))
  }

  @throws[TimeoutException]
  def probesForPathway(sc: SampleClass, pathway: String): Array[String] = {    
    val geneIds = b2rKegg.geneIds(pathway).map(Gene(_))
    println("Probes for " + geneIds.size + " genes")
    val probes = probeStore.forGenes(geneIds).toArray
    val pmap = context.matrix.unifiedProbes //TODO
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
      probeStore.forUniprots(oproteins ++ proteins)
      //      OTGOwlim.probesForEntrezGenes(genes)
    } else {
      probeStore.forUniprots(proteins)
    }
    val pmap = context.matrix.unifiedProbes //TODO context.probes(filter)
    pbs.toSet.map((p: Probe) => p.identifier).filter(pmap.isToken).toArray
  }

  @throws[TimeoutException]
  def goTerms(pattern: String): Array[String] =
    probeStore.goTerms(pattern).map(_.name).toArray

  @throws[TimeoutException]
  def probesForGoTerm(goTerm: String): Array[String] = {
    val pmap = context.matrix.unifiedProbes 
    probeStore.forGoTerm(GOTerm("", goTerm)).map(_.identifier).filter(pmap.isToken).toArray
  }

  import scala.collection.{ Map => CMap, Set => CSet }

  //TODO refactor this; instead of gathering all column logic here,
  //implement each column separately in a way that incorporates
  //both presentation and lookup code
  
  @throws[TimeoutException]
  def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] =    
    new AnnotationResolver(sc, types, _probes).resolve
  
  import Association._

  protected class AnnotationResolver(sc: SampleClass, types: Array[AType],
      _probes: Iterable[String]) {
    val probes = probeStore.withAttributes(_probes.map(Probe(_)))
    
    lazy val proteins = toBioMap(probes, (_: Probe).proteins)

    //    val sp = asSpecies(sc)
    //orthologous proteins if needed
    lazy val oproteins = {

      val r = if ((types.contains(AType.Chembl) ||
        types.contains(AType.Drugbank) ||
        types.contains(AType.OrthProts))
        //      && (sp != Human)
        && false // Not used currently due to performance issues!
        ) {
        // This always maps to Human proteins as they are assumed to contain the most targets
        val r = proteins combine ((ps: Iterable[Protein]) => uniprot.orthologsFor(ps, Human))
        r
      } else {
        emptyMMap[Probe, Protein]()
      }
      println(r.allValues.size + " oproteins")
      r
    }

    def getTargeting(sc: SampleClass, from: CompoundTargets): MMap[Probe, Compound] = {
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

    def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe]): BBMap =
      at match {
        case _: AType.Chembl.type    => getTargeting(sc, chembl)
        case _: AType.Drugbank.type  => getTargeting(sc, drugBank)

        // The type annotation :BBMap is needed on at least one (!) match pattern
        // to make the match statement compile. TODO: research this
        case _: AType.Uniprot.type   => proteins: BBMap
        case _: AType.OrthProts.type => oproteins
        case _: AType.GOMF.type      => probeStore.mfGoTerms(probes)
        case _: AType.GOBP.type      => probeStore.bpGoTerms(probes)
        case _: AType.GOCC.type      => probeStore.ccGoTerms(probes)
        case _: AType.GO.type        => probeStore.goTerms(probes)
        case _: AType.Homologene.type =>
          toBioMap(probes, (_: Probe).genes) combine
            homologene.homologousGenes(probes.flatMap(_.genes))
        case _: AType.KEGG.type =>
          val sp = asSpecies(sc)
          toBioMap(probes, (_: Probe).genes) combine
            b2rKegg.forGenes(probes.flatMap(_.genes), sp)
        case _: AType.Enzymes.type =>
          val sp = asSpecies(sc)
          b2rKegg.enzymes(probes.flatMap(_.genes), sp)
      }

       
    val emptyVal = CSet(DefaultBio("error", "(Timeout or error)"))
    val errorVals = Map() ++ probes.map(p => (Probe(p.identifier) -> emptyVal))
    
    def queryOrEmpty[T](f: () => BBMap): BBMap = {      
      gracefully(f, errorVals)
    }
    
    def lookupFunction(t: AType): BBMap =
      queryOrEmpty(() => associationLookup(t, sc, probes))

    def standardMapping(m: BBMap): MMap[String, (String, String)] =
      m.mapKValues(_.identifier).mapMValues(p => (p.name, p.identifier))

    def resolve(): Array[Association] = {

      val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)))).seq
      m1.map(p => new Association(p._1, convertPairs(p._2))).toArray
    }
  }

  @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[String] = {    
      probeStore.probesForPartialSymbol(partialName, sc).map(_.identifier).toArray
  }

}