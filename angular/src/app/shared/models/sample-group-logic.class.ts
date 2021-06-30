import { Sample } from "./backend-types.model";
import { ISampleGroup } from "./frontend-types.model";

export class SampleGroupLogic {
  static canSelectGroup(group: ISampleGroup,
      currentSelectedGroups: ISampleGroup[]): boolean {
    return (currentSelectedGroups.length == 0) ||
        ((currentSelectedGroups[0].organism == group.organism) &&
         (currentSelectedGroups[0].platform == group.platform))
  }

  static createSampleGroup(name: string, samples: Sample[],
      currentSelectedGroups: ISampleGroup[]): ISampleGroup {
    const type = samples[0]["type"]
    const organism = samples[0]["organism"]
    const platform = samples[0]["platform_id"];

    const sampleIds = samples.map(s => s.sample_id);

    const newGroup = <ISampleGroup>{
      name: name,
      organism: organism,
      type: type,
      platform: platform,
      samples: sampleIds,
    };

    newGroup.enabled = this.canSelectGroup(newGroup, currentSelectedGroups);

    return newGroup;
  }
}
