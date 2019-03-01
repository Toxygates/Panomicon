package t.viewer.client.future;

/**
 * Interface for objects that can notify a number of Dependent objects at some
 * time in the future.
 * 
 * When whatever operation the Dependable represents is complete, it should call
 * the dependableCompleted method of each Dependent that has been added.
 */
public interface Dependable {
  /**
   * Add a Dependent to be notified in the future when this Dependable completes.
   * An object implementing this method must call dependent.startDepending,
   * passing in itself as an argument.
   * @param dependant the Dependent to notify in the future
   * @return this object itself, for method chaining
   */
  Dependable addDependent(Dependent dependent);
}
