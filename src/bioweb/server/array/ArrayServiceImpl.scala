package bioweb.server.array

import bioweb.shared.array.SampleFilter
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import bioweb.shared.array.Sample
import bioweb.shared.array.DataColumn

class ArrayServiceImpl[S <: Sample, F <: SampleFilter[S]] extends RemoteServiceServlet {

  /**
   * Various parameters for the data view
   */
  class DataViewParams {
    var sortAsc: Boolean = _
    var sortColumn: Int = _
    var mustSort: Boolean = _
    var filter: F = _
  }
}