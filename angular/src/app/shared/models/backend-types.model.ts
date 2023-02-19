export interface Dataset {
  id: string;
  description: string;
}

export interface Batch {
  id: string;
}

export type Sample = Record<string, string>;

export interface Attribute {
  id: string;
  title: string;
  isNumerical: boolean;
}

export interface Matrix {
  columns: {
    name: string;
  }[];
  sorting: {
    column: number;
    ascending: boolean;
  };
  rows: Record<string, string | string[]>;
}

export interface GeneSet {
  name: string;
  items: string[];
}

export interface Platform {
  id: string;
  probes: number;
  date: string;
  publicComment: string;
  type: string;
}
