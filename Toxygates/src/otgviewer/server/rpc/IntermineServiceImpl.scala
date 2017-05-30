package otgviewer.server.rpc

import otg.OTGBConfig
import t.DataConfig
import t.TriplestoreConfig
import t.BaseConfig

class IntermineServiceImpl extends
  t.viewer.server.intermine.IntermineServiceImpl with OTGServiceServlet {

  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig =
    OTGBConfig(ts, data)

}
