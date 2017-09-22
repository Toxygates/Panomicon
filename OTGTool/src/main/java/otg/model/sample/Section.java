package otg.model.sample;

/**
 * Sections of parameters in Open TG-GATEs.
 * This enum defines essential sections only.
 * Additional sections may be defined in the triplestore.
 */
enum Section {
  SampleDetails("Sample details"),
  OrganWeight("Organ weight"), Meta("Meta");
  
  String title;
  Section(String title) {
    this.title = title;
  }
}
