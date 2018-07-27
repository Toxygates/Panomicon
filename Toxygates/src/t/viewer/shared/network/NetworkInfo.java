package t.viewer.shared.network;

import java.io.Serializable;

import t.viewer.shared.ManagedMatrixInfo;

/**
 * Information about a network loaded in a session on the server.
 * Corresponds loosely to ManagedMatrixInfo for single tables.
 *
 */
@SuppressWarnings("serial")
public class NetworkInfo implements Serializable {

  //GWT constructor
  NetworkInfo() {}
  
  public NetworkInfo(ManagedMatrixInfo mainInfo,
      ManagedMatrixInfo sideInfo) {
    this.mainInfo = mainInfo;
    this.sideInfo = sideInfo;
  }
  
  private ManagedMatrixInfo mainInfo, sideInfo;
  public ManagedMatrixInfo mainInfo() { return mainInfo; }
  public ManagedMatrixInfo sideInfo() { return sideInfo; }
  
}
