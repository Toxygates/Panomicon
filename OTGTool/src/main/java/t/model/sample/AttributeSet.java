package t.model.sample;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A set of sample attributes.
 */
public class AttributeSet {
 
  /**
   * Construct a new attribute set. 
   * @param attributes All attributes in the set.
   * @param required The subset of attributes that are required to be present in new batches.
   */
  public AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    this.attributes = attributes;
    this.required = required;
    for (Attribute a: attributes) {
      byId.put(a.id(), a);
      byTitle.put(a.title(), a);
    }
  }
  
  protected Collection<Attribute> attributes;
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

  public @Nullable Attribute byId(String id) {
    return byId.get(id);
  }
  
  public @Nullable Attribute byTitle(String title) {
    return byTitle.get(title);
  }
  
  /**
   * Find the attribute with the given id or create it (and add it to this set) if it
   * doesn't exist.
   * @param id
   * @param title
   * @return
   */
  synchronized public Attribute findOrCreate(String id, @Nullable String title,
      @Nullable String kind) {
    if (byId.containsKey(id)) {
      return byId.get(id);
    }
    
    Attribute a = new BasicAttribute(id, title, kind);
    attributes.add(a);
    byId.put(id, a);
    byTitle.put(title, a);
    return a;
  }
  
  
}
