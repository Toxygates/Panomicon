export interface SampleGroup {
  name: string;
  organism: string;
  type: string;
  platform: string;
  samples: string[];
  enabled: boolean;
}

export interface GeneSet {
  name: string;
  platform: string;
  probes: string[];
}
