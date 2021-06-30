export interface ISampleGroup {
  name: string;
  organism: string;
  type: string;
  platform: string;
  samples: string[];
  enabled: boolean;
}

export interface IGeneSet {
  name: string;
  platform: string;
  probes: string[];
}
