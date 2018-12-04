package t.viewer.client;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;

import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.*;
import t.viewer.shared.ItemList;

/**
 * Methods for packing objects into strings, and unpacking from strings,
 * factored out of StorageParser.
 */
public class Packer {
  protected static final Logger logger = SharedUtils.getLogger("storage");

  public static String packSampleClass(SampleClass sc) {
    StringBuilder sb = new StringBuilder();
    for (Attribute k : sc.getKeys()) {
      sb.append(k.id() + ",,,");
      sb.append(sc.get(k) + ",,,");
    }
    return sb.toString();
  }

  //Transitional method for upgrading from old format, as of Jan 2018
  private static void upgradeSampleClass(Map<Attribute, String> data) {
    data.put(CoreParameter.Type, "mRNA");
  }

  public static SampleClass unpackSampleClass(AttributeSet attributes, String value) {
    String[] spl = value.split(",,,");
    Map<Attribute, String> d = new HashMap<Attribute, String>();
    for (int i = 0; i < spl.length - 1; i += 2) {
      Attribute attribute = attributes.byId(spl[i]);
      if (attribute != null) {
        d.put(attribute, spl[i + 1]);
      }
    }

    if (!d.containsKey(CoreParameter.Type)) {
      upgradeSampleClass(d);
    }

    return new SampleClass(d);
  }

  public static String packSample(Sample sample) {
    final String sep = "$$$";
    StringBuilder sb = new StringBuilder();
    sb.append("Barcode_v3").append(sep);
    sb.append(sample.id()).append(sep);
    sb.append(packSampleClass(sample.sampleClass())).append(sep);
    return sb.toString();
  }

  public static @Nullable Sample unpackSample(String s, AttributeSet attributeSet) {
    String[] spl = s.split("\\$\\$\\$");
    String v = spl[0];
    if (!v.equals("Barcode_v3")) {
      Window.alert("Legacy data has been detected in your browser's storage. "
          + "Some of your older sample groups may not load properly.");
      return null;
    }
    String id = spl[1];
    SampleClass sc = unpackSampleClass(attributeSet, spl[2]);
    return new Sample(id, sc);
  }

  public static String packGroup(Group group) {
    StringBuilder s = new StringBuilder();
    s.append("Group:::");
    s.append(group.getName() + ":::"); // !!
    s.append(group.getColor() + ":::");
    for (Sample sample : group.samples()) {
      s.append(packSample(sample));
      s.append("^^^");
    }
    return s.toString();
  }

  public static Group unpackGroup(DataSchema schema, String s, AttributeSet attributeSet) {
    String[] s1 = s.split(":::"); // !!
    String name = s1[1];
    String color = "";
    String barcodes = "";

    color = s1[2];
    barcodes = s1[3];
    if (SharedUtils.indexOf(SampleGroup.groupColors, color) == -1) {
      // replace the color if it is invalid.
      // this lets us safely upgrade colors in the future.
      color = SampleGroup.groupColors[0];
    }

    String[] s2 = barcodes.split("\\^\\^\\^");
    Sample[] bcs = new Sample[s2.length];
    for (int i = 0; i < s2.length; ++i) {
      Sample b = unpackSample(s2[i], attributeSet);
      bcs[i] = b;
    }
    // DataFilter useFilter = (bcs[0].getUnit().getOrgan() == null) ? filter : null;
    return new Group(schema, name, bcs, color);

  }

  public static String packColumns(Collection<Group> columns) {
    List<String> xs = new ArrayList<String>();
    for (Group group : columns) {
      xs.add(packGroup(group));
    }
    return packList(xs, "###");
  }

  @Nullable
  public static Group unpackColumn(DataSchema schema, String s, AttributeSet attributes) {
    if (s == null) {
      return null;
    }
    String[] spl = s.split("\\$\\$\\$");
    if (!spl[0].equals("Barcode") && !spl[0].equals("Barcode_v3")) {
      return unpackGroup(schema, s, attributes);
    } else {
      // Legacy or incorrect format
      logger.warning("Unexpected column format: " + s);
      return null;
    }
  }

  public static String packProbes(String[] probes) {
    return packList(Arrays.asList(probes), "###");
  }

  public static String packPackableList(Collection<? extends Packable> items, String separator) {
    List<String> xs = new ArrayList<String>();
    for (Packable p : items) {
      xs.add(p.pack());
    }
    return packList(xs, separator);
  }

  public static String packList(Collection<String> items, String separator) {
    // TODO best location of this? handle viewer/common separation cleanly.
    return SharedUtils.packList(items, separator);
  }

  public static String packItemLists(Collection<ItemList> lists, String separator) {
    return packPackableList(lists, separator);
  }

  public static String packDatasets(Dataset[] datasets) {
    List<String> r = new ArrayList<String>();
    for (Dataset d : datasets) {
      r.add(d.getId());
    }
    return packList(r, "###");
  }

  public String packCompounds(List<String> compounds) {
    return packList(compounds, "###");
  }
}
