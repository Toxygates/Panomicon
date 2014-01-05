package bioweb.server.array

import bioweb.shared.array.SampleFilter
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import bioweb.shared.array.Sample
import bioweb.shared.array.DataColumn

class ArrayServiceImpl[S <: Sample, F <: SampleFilter[S]] extends RemoteServiceServlet {
  // TODO: lift up code to this class from subclass
}