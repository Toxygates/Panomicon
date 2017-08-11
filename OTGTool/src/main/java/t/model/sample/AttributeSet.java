package t.model.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A set of sample attributes.
 */
abstract public class AttributeSet {
 
  /**
   * Construct a new attribute set. 
   * Throughout an application, only one attribute set should be used in most cases.
   * @param attributes All attributes in the set.
   * @param required The subset of attributes that are required to be present in new batches.
   */
  public AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    this.required = required;
    for (Attribute a: attributes) {
      add(a);
    }
  }
  
  protected Collection<Attribute> attributes = new ArrayList<Attribute>();
  protected Collection<Attribute> required;
  
  protected Map<String, Attribute> byId = new HashMap<String, Attribute>();  
  protected Map<String, Attribute> byTitle = new HashMap<String, Attribute>();
  
  public Collection<Attribute> getAll() {
    return attributes;
  }
  
  /**
   * Get all attributes that are required to be present in new batches.
   * @return
   */
  public Collection<Attribute> getRequired() {
    return required;
  }

  /**
   * Get all attributes that are suitable for a preview display (a brief overview of a 
   * set of samples).
   * @return
   */
  public Collection<Attribute> getPreviewDisplay() {
    return required;
  }
  
  /**
   * Get all attributes that are suitable for a high level grouping of samples.
   * @return
   */
  abstract public Collection<Attribute> getHighLevel();
  
  
  public @Nullable Attribute byId(String id) {
    return byId.get(id);
  }
  
  public @Nullable Attribute byTitle(String title) {
    return byTitle.get(title);
  }
  
  private void add(Attribute a) {
    attributes.add(a);
    byId.put(a.id(), a);
    byTitle.put(a.title(), a);
  }
  
  /**
   * Find the attribute with the given id or create it (and add it to this set) if it
   * doesn't exist.
   * @param id
   * @param title
   * @param kind
   * @return
   */
  public Attribute findOrCreate(String id, @Nullable String title,
                                @Nullable String kind) {
    return findOrCreate(id, title, kind, null);
  }
  
  
  /**
   * Find the attribute with the given id or create it (and add it to this set) if it
   * doesn't exist.
   * @param id
   * @param title
   * @param kind
   * @param section
   * @return
   */
  synchronized public Attribute findOrCreate(String id, @Nullable String title,
      @Nullable String kind, @Nullable String section) {
    if (byId.containsKey(id)) {
      return byId.get(id);
    }
    
    Attribute a = new BasicAttribute(id, title, kind, section);
    add(a);
    return a;
  }
}
