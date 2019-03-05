package t.viewer.client.future;

/**
 * Interface for objects that wait for some number of Dependables to complete,
 * and do something when they complete. The most common pattern will be to 
 * do something when all Dependables have completed.
 */
public interface Dependent {
  /**
   * Called by Dependable.addDependent to notify this object that it has been
   * added as a dependent. Should ONLY be called in the body of a
   * Dependable's addDependent implementation.
   * @param dependable the Dependable to start depending on
   */
  void onStartDepending(Dependable dependable);
  
  /**
   * Start depending on a Dependable. Implementations should merely call the
   * Dependable's addDependent method, passing itself as the argument.
   * @param dependable the Dependable to start depending on
   * @return this object itself, for method chaining
   */
  Dependent dependOn(Dependable dependable);
  
  /**
   * Called by a Dependable that this object is depending on, when it has
   * completed.
   * @param dependable the Dependable that just completed 
   */
  void dependableCompleted(Dependable dependable);
}
