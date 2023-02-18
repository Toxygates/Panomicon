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

interface Node {
  id: string;
  type: 'mRNA' | 'miRNA';
  weights: Record<string, string | number>;
  symbols: string[];
}

interface Interaction {
  from: string;
  to: string;
  label: string;
  weight: number;
}

export interface Network {
  nodes: Node[];
  interactions: Interaction[];
}
