export interface Platform {
  id: string;
  comment: string;
  date: string;
  probes: number;
  publicComment: string;
}

export interface Batch {
  id: string;
  dataset: string;
  timestamp: string;
  enabledInstances: string[];
  numSamples: number;
  comment: string;
  publicComment: string;
}

export interface Dataset {
  id: string;
  timestamp: string;
  comment: string;
  publicComment: string;
  description: string;
  numBatches: number;
}

export interface Instance {
  id: string;
  timestamp: string;
  comment: string;
}
