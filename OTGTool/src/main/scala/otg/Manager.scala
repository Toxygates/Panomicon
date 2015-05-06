package otg

import t.BaseConfig
import otg.platform.SSOrthTTL
import friedrich.util.CmdLineOptions
import t.DataConfig
import t.TriplestoreConfig
import otg.db.file.TSVMetadata

abstract class Manager extends t.Manager[Context] with CmdLineOptions {
  override protected def handleArgs(args: Array[String])(implicit context: Context) {
    
     args(0) match {      
      case "orthologs" => 
      val output = require(stringOption(args, "-output"),
          "Please specify an output file with -output")
      val inputs = require(stringListOption(args, "-inputs"),
          "Please specify input files with -inputs")
          
      new SSOrthTTL(context.probes, inputs, output).generate
      case _ => super.handleArgs(args)
    }
  }
  
  lazy val factory: Factory = new otg.Factory()
  
  def initContext(bc: OTGBConfig): Context = otg.Context(bc)
  
   def makeBaseConfig(ts: TriplestoreConfig, d: DataConfig): BaseConfig =
    OTGBConfig(ts, d)
}

case class OTGBConfig(triplestore: TriplestoreConfig, data: DataConfig) extends BaseConfig {
  def seriesBuilder = OTGSeries
  
  def sampleParameters = otg.db.SampleParameter
}

