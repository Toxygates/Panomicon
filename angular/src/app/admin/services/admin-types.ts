export interface Batch {
  id: string;
  dataset: string;
  timestamp: string;
  enabledInstances: string[];
  numSamples: number;
  comment: string;
  publicComment: string;
}
