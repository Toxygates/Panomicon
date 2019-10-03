package t.viewer.client;

import java.util.*;
import java.util.stream.Collectors;

import t.common.shared.DataSchema;
import t.common.shared.sample.*;

/**
 * Like a group, but encodes the notion of whether it is active or inactive,
 * which is relevant for the web client.
 */
@SuppressWarnings("serial")
public class ClientGroup extends Group {
  public boolean active;
  
  public ClientGroup(DataSchema schema, String name, Sample[] barcodes, String color,
      boolean active) {
    super(schema, name, barcodes, color);
    this.active = active;
    _units = Unit.formUnits(schema, barcodes);
  }
  
  public ClientGroup(DataSchema schema, String name, Unit[] units, boolean active, String color) {
    super(schema, name, Unit.collectSamples(units), color);
    this.active = active;
    _units = units;
  }
  
  /**
   * Construct a ClientGroup by copying all data from a Group
   * @param group group to be copied
   * @param active whether the new ClientGroup should be active 
   */
  public ClientGroup(Group group, boolean active) {
    super();
    _units = group.getUnits();
    name = group.getName();
    color = group.getColor();
    _samples = group.getSamples();
    schema = group.getSchema();
    this.active = active;
  }
  
  @Override
  public int compareTo(SampleGroup<?> other) {
    if (other instanceof ClientGroup) {
      ClientGroup otherGroup = (ClientGroup) other;
      if (active != otherGroup.active) {
        return active ? -1 : 1;
      }
    }
    return name.compareTo(other.getName());
  }
  
  public Group convertToGroup() {
    return new Group(schema, name, _samples, color);
  }
  
  public static List<Group> convertToGroups(Collection<ClientGroup> clientGroups) {
    return clientGroups.stream().map(cg -> cg.convertToGroup()).collect(Collectors.toList());
  }

  public static List<Sample> getAllSamples(List<ClientGroup> columns) {
    List<Sample> list = new ArrayList<Sample>();
    for (ClientGroup g : columns) {
      List<Sample> ss = Arrays.asList(g.getSamples());
      list.addAll(ss);
    }
    return list;
  }
}
