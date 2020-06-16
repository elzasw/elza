type DataceElement = {
  c?: number;
  y?: number;
  M?: number;
  d?: number;
  h?: number;
  m?: number;
  s?: number;
  bc: boolean;
};

type DataceInterval = {
  from?: DataceElement;
  to?: DataceElement;
};

export type Datace = DataceElement & DataceInterval;