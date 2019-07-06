package t.viewer.client.storage;

import java.util.*;

/**
 * Manages the storage of objects that can be saved and loaded by name. 
 * Backed by an instance of t.viewer.client.storage.Storage.
 */
public class NamedObjectStorage<T> {
  private Map<String, T> objectsByName;
  private Storage<List<T>> localStorage;
  private NameExtractor<T> nameExtractor;
  private NameChanger<T> nameChanger;
  
  public interface NameExtractor<T> {
    String getName(T object);
  }
  
  public interface NameChanger<T> {
    void changeName(T object, String newName);
  }
  
  public NamedObjectStorage (Storage<List<T>> localStorage, NameExtractor<T> nameExtractor) {
    this.localStorage = localStorage;
    this.nameExtractor= nameExtractor;
    objectsByName = new HashMap<String, T>();
    
    loadFromStorage();
  }
  
  public NamedObjectStorage (Storage<List<T>> localStorage, NameExtractor<T> nameExtractor,
      NameChanger<T> nameChanger) {
    // We can't use our other constructor, because nameChanger needs to be assigned
    // before the loadFromStorage call
    this.localStorage = localStorage;
    this.nameExtractor= nameExtractor;
    this.nameChanger = nameChanger;
    objectsByName = new HashMap<String, T>();
    
    loadFromStorage();
  }
  
  /**
   * Loads objects from storage. If a NameChanger is available and multiple
   * objects with the same name are found, then suggestName is used to find 
   * new names for the ones with duplicate names.
   */
  public void loadFromStorage() {
    clear();
    List<T> items = localStorage.getIgnoringException();
    
    /* We first insert all the uniquely named objects from the list, THEN insert 
     * objects that need to be renamed. Otherwise, if we have networks named
     * ["Network", "Network", "Network 1"], then the second network would be renamed
     * to "Network 1" and the third network would be renamed to "Network 1 1", which
     * is not desirable.
     */
    List<T> secondBatch = new ArrayList<T>();
    
    for (T item : items) {
      String itemName = nameExtractor.getName(item);
      if (objectsByName.containsKey(itemName)) {
        if (nameChanger == null) {
          throw new RuntimeException("Duplicate name " + itemName + " while " +
              "loading from storage, with no NameChanger provided.");
        } else {
          secondBatch.add(item);
        }
      } else {
        objectsByName.put(itemName, item);
      }
    }
    
    secondBatch.forEach(item -> {
      nameChanger.changeName(item, suggestName(nameExtractor.getName(item)));
      objectsByName.put(nameExtractor.getName(item), item);
    });
  }
  
  public void saveToStorage() {
    localStorage.store(new ArrayList<T>(objectsByName.values()));
  }
  
  public T get(String name) {
    return objectsByName.get(name);
  }
  
  public void put(T value) {
    objectsByName.put(nameExtractor.getName(value), value);
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
  
  public String suggestName(String prefix) {
    String name = prefix;
    int i = 1;
    while (containsKey(name)) {
      name = prefix + " " + i;
      i++;
    }
    return name;
  }
}
