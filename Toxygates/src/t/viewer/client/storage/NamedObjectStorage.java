package t.viewer.client.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the storage of objects that can be saved and loaded by name. 
 * Backed by an instance of t.viewer.client.storage.Storage.
 */
public class NamedObjectStorage<T> {
  private Map<String, T> objectsByName;
  private Storage<List<T>> localStorage;
  private NameExtractor<T> nameExtractor;
  
  public interface NameExtractor<T> {
    String getName(T object);
  }
  
  public NamedObjectStorage (Storage<List<T>> localStorage, NameExtractor<T> nameExtractor) {
    this.localStorage = localStorage;
    this.nameExtractor= nameExtractor;
    objectsByName = new HashMap<String, T>();
    
    loadFromStorage();
  }
  
  public void loadFromStorage() {
    List<T> items = localStorage.getIgnoringException();
    for (T item : items) {
      objectsByName.put(nameExtractor.getName(item), item);
    }
  }
  
  public void saveToStorage() {
    localStorage.store(new ArrayList<T>(objectsByName.values()));
  }
  
  public T get(String name) {
    return objectsByName.get(name);
  }
  
  public void put(String name, T value) {
    objectsByName.put(name, value);
  }
  
  public void remove(String name) {
    objectsByName.remove(name);
  }
  
  public int size() {
    return objectsByName.size();
  }
  
  public void clear() {
    objectsByName.clear();
  }
  
  public List<T> allObjects() {
    return new ArrayList<T>(objectsByName.values());
  }
  
  public boolean containsKey(String key) {
    return objectsByName.containsKey(key);
  }
}
