package t.testing

import t.DataConfig
import t.TriplestoreConfig
import t.BaseConfig
import otg.OTGBConfig

object TestConfig {
	val dataConfig = new DataConfig("/shiba/toxygates/data_dev", 
	    "#bnum=80000000#pccap=1073741824#apow=1")
//	val tsConfig = new TriplestoreConfig("http://localhost:3030/data/sparql", 
//	    "http://localhost:3030/data/update", null, null, null)
	val tsConfig = new TriplestoreConfig("http://sontaran:8081/owlim-workbench-webapp-5.3.1", 
	    null, "ttest", "ttest", "ttest")
	val config = new OTGBConfig(tsConfig, dataConfig)
}