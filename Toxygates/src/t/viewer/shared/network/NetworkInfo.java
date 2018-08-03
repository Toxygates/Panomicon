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
      ManagedMatrixInfo sideInfo,
      Network network) {
    this.mainInfo = mainInfo;
    this.sideInfo = sideInfo;
    this.network = network;
  }
  
  private ManagedMatrixInfo mainInfo, sideInfo;
  
  /**
   * Information about the loaded main matrix  
   * @return
   */
  public ManagedMatrixInfo mainInfo() { return mainInfo; }
  /**
   * Information about the loaded side matrix
   * @return
   */
  public ManagedMatrixInfo sideInfo() { return sideInfo; }
  
  private Network network;
  /**
   * The interaction network
   * @return
   */
  public Network network() { return network; }
}
