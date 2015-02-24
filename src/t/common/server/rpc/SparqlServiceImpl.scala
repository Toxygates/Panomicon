package t.common.server.rpc

import t.common.client.rpc.SparqlService
import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asJavaMap
import scala.collection.{Set => CSet}
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.Species.Human
import t.sparql.secondary._
import t.sparql._
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
import t.sparql.Probes
import t.common.shared.Dataset
import otgviewer.server.rpc.Conversions
import Conversions.asJava
import Conversions.asJavaSample
import Conversions.convertPairs
import t.common.server.SharedDatasets

object SparqlServiceImpl {
  //TODO consider moving these to an application-wide SparqlContext or similar
  
  var inited = false  
 
  //TODO update mechanism for this
  var platforms: Map[String, Iterable[String]] = _
  
  def staticInit(c: t.Context) = synchronized {    
    if (!inited) {    
      platforms = c.probes.platforms
      inited = true
    }
  }
}

/**
 * SPARQL query servlet.
 */
abstract class SparqlServiceImpl extends TServiceServlet with SparqlService {

  import Conversions._
  import SparqlServiceImpl._
  import t.viewer.server.Conversions._
  import ScalaUtils._

  type DataColumn = t.common.shared.sample.DataColumn[OTGSample]
 
  var instanceURI: Option[String] = None
  
  private def probeStore: Probes = context.probes
  private def sampleStore: Samples = context.samples
  
  var uniprot: Uniprot = _  
  var b2rKegg: B2RKegg = _

  override def localInit(conf: Configuration) {
    super.localInit(conf)
    staticInit(context)

    val ts = baseConfig.triplestore.triplestore
    uniprot = new LocalUniprot(ts)
    b2rKegg = new B2RKegg(ts)
    
    if (conf.instanceName == null || conf.instanceName == "") {
      instanceURI = None
    } else {
      instanceURI = Some(Instances.defaultPrefix + "/" + conf.instanceName)
    }    
  }
  
  protected class SparqlState(ds: Datasets) {
    var datasets: Set[String] = ds.list.toSet
  }

  def getSessionData(): SparqlState = {
    val r = getThreadLocalRequest().getSession().getAttribute("sparql").
      asInstanceOf[SparqlState]
    if (r == null) {
      val ds = new Datasets(baseConfig.triplestore)
      val ss = new SparqlState(ds)
      setSessionData(ss)
      ss
    } else {
      r
    }
  }
  
  def setSessionData(m: SparqlState) =
    getThreadLocalRequest().getSession().setAttribute("sparql", m)
  
  def datasets(): Array[Dataset] = {
    val ds = new Datasets(baseConfig.triplestore) with SharedDatasets
    ds.sharedList.toArray
  }
  
  def chooseDatasets(ds: Array[Dataset]): Unit = {
    println("Choose datasets: " + ds.toSet)
    getSessionData.datasets = ds.map(_.getTitle).toSet
  }

  @throws[TimeoutException]
  def parameterValues(sc: Array[SampleClass], parameter: String): Array[String] = {
    sc.flatMap(x => parameterValues(x, parameter)).toSet.toArray
  }
  
  @throws[TimeoutException]
  def parameterValues(sc: SampleClass, parameter: String): Array[String] = {    
    sampleStore.attributeValues(scAsScala(sc), parameter, instanceURI).
      filter(x => !schema.isMajorParamSharedControl(x)).toArray
  }
  
  def samplesById(ids: Array[String]): Array[OTGSample] = 
    sampleStore.samples(new t.sparql.SampleClass(), "id", 
        ids, instanceURI).map(asJavaSample(_)).toArray 

  //TODO compound_name is a dummy parameter below
  @throws[TimeoutException]
  def samples(sc: SampleClass): Array[OTGSample] =
    sampleStore.samples(scAsScala(sc), "compound_name", 
        List(), instanceURI).map(asJavaSample(_)).toArray

  @throws[TimeoutException]
  def samples(sc: SampleClass, param: String, 
      paramValues: Array[String]): Array[OTGSample] =
    sampleStore.samples(sc, param, paramValues, instanceURI).map(asJavaSample(_)).toArray

  @throws[TimeoutException]
  def samples(scs: Array[SampleClass], param: String, 
      paramValues: Array[String]): Array[OTGSample] =
        scs.flatMap(x => samples(x, param, paramValues)).toSet.toArray
  
  @throws[TimeoutException]
  def sampleClasses(): Array[SampleClass] = {    
  sampleStore.sampleClasses(instanceURI).map(x => 
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
        sampleStore.attributeValues(scAsScala(sc), majorParam, instanceURI)        
      val shared = allMajors.filter(schema.isMajorParamSharedControl(_))
      (shared.toSeq ++ paramValues.toSeq)
    } else {
      paramValues.toSeq
    }
    
    //TODO rethink how to use batch here
    val ss = sampleStore.samples(sc, param, useParamValues, instanceURI).    
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

  @throws[TimeoutException]
  def probes(columns: Array[OTGColumn]): Array[String] = {
    val samples = columns.flatMap(_.getSamples)
    val metadata = new TriplestoreMetadata(sampleStore, instanceURI)    
    val usePlatforms = samples.map(s => metadata.parameter(
        t.db.Sample(s.getCode), "platform_id")
        ).toSet
    usePlatforms.toVector.flatMap(platforms).toArray
  }
  
  //TODO move to OTG
  @throws[TimeoutException]
  def pathologies(barcode: OTGSample): Array[Pathology] = Array()
    
  //TODO move to OTG
  @throws[TimeoutException]
  def pathologies(column: OTGColumn): Array[Pathology] = Array()

  @throws[TimeoutException]
  def annotations(barcode: OTGSample): Annotation = 
    asJava( sampleStore.annotations(barcode.getCode, List(), instanceURI) )
    
  //TODO get these from schema, etc.
  @throws[TimeoutException]
  def annotations(column: HasSamples[OTGSample], importantOnly: Boolean = false): Array[Annotation] = {   
    val keys = if (importantOnly) {
      List("Dose", "Dose unit", "Dose level", "Exposure time", "Administration route")      
    } else {
      Nil //fetch everything
    }
    column.getSamples.map(x => sampleStore.annotations(x.getCode, keys, instanceURI)).map(asJava(_))
  }

  @throws[TimeoutException]
  def pathways(sc: SampleClass, pattern: String): Array[String] =
    b2rKegg.forPattern(pattern, sc).toArray

  //TODO: return a map instead
  @throws[TimeoutException]
  def geneSyms(_probes: Array[String]): Array[Array[String]] = {
    val ps = _probes.map(p => Probe(p))
    val attrib = probeStore.withAttributes(ps)
    _probes.map(pi => attrib.find(_.identifier == pi).
      map(_.symbolStrings.toArray).getOrElse(Array()))
  }

  @throws[TimeoutException]
  def probesForPathway(sc: SampleClass, pathway: String): Array[String] = {    
    val geneIds = b2rKegg.geneIds(pathway).map(Gene(_))
    println("Probes for " + geneIds.size + " genes")
    val prs = probeStore.forGenes(geneIds).toArray
    val pmap = context.matrix.probeMap //TODO
    prs.map(_.identifier).filter(pmap.isToken).toArray
  }
  
  //TODO move to OTG
  @throws[TimeoutException]
  def probesTargetedByCompound(sc: SampleClass, compound: String, service: String,
    homologous: Boolean): Array[String] = Array()
 
    //TODO move to OTG
  @throws[TimeoutException]
  def goTerms(pattern: String): Array[String] =    
    probeStore.goTerms(pattern).map(_.name).toArray

      //TODO move to OTG
  @throws[TimeoutException]
  def probesForGoTerm(goTerm: String): Array[String] = {
    val pmap = context.matrix.probeMap 
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
    val aprobes = probeStore.withAttributes(_probes.map(Probe(_)))
   
    lazy val proteins = toBioMap(aprobes, (_: Probe).proteins)

  
    def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe]): BBMap =
      at match {

        // The type annotation :BBMap is needed on at least one (!) match pattern
        // to make the match statement compile. TODO: research this
        case _: AType.Uniprot.type   => proteins: BBMap
        case _: AType.GO.type        => probeStore.goTerms(probes)
       
        case _: AType.KEGG.type =>
          val sp = asSpecies(sc)
          toBioMap(probes, (_: Probe).genes) combine
            b2rKegg.forGenes(probes.flatMap(_.genes), sp)
        case _: AType.Enzymes.type =>
          val sp = asSpecies(sc)
          b2rKegg.enzymes(probes.flatMap(_.genes), sp)
      }

       
    val emptyVal = CSet(DefaultBio("error", "(Timeout or error)"))
    val errorVals = Map() ++ aprobes.map(p => (Probe(p.identifier) -> emptyVal))
    
    def queryOrEmpty[T](f: () => BBMap): BBMap = {      
      gracefully(f, errorVals)
    }
    
    def lookupFunction(t: AType): BBMap =
      queryOrEmpty(() => associationLookup(t, sc, aprobes))

    def standardMapping(m: BBMap): MMap[String, (String, String)] =
      m.mapKValues(_.identifier).mapMValues(p => (p.name, p.identifier))

    def resolve(): Array[Association] = {

      val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)))).seq
      m1.map(p => new Association(p._1, convertPairs(p._2))).toArray
    }
  }

  @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[String] = {
      probeStore.probesForPartialSymbol(partialName).map(_.identifier).toArray
  }

}