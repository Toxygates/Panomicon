export interface IDataset {
  id: string;
  description: string;
}

export interface IBatch {
  id: string;
}

export type Sample = Record<string, string>;

export interface IAttribute {
  id: string;
  title: string;
  isNumerical: boolean;
}

export interface IMatrix {
  columns: {
    name: string,
  }[],
  sorting: {
    column: number,
    ascending: boolean
  },
  rows: Record<string, string | string[]>
}
