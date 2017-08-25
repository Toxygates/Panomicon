package otgviewer.server.rpc

import otg.OTGBConfig
import t.BaseConfig
import t.DataConfig
import t.TriplestoreConfig

class IntermineServiceImpl extends
  t.viewer.server.intermine.IntermineServiceImpl with OTGServiceServlet {

  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig =
    OTGBConfig(ts, data)

}
