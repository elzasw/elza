export interface MaskViewDefinition{
    mask: string;
}

export function isMaskViewDefinition(viewDefinition: unknown): viewDefinition is MaskViewDefinition{
    return viewDefinition?.["mask"] != undefined;
}

export function maskString(raw: string, mask: string) {
  let ri = 0;
  let result = "";
  for (let mi = 0; mi < mask.length; mi++) {
    const mc = mask[mi];
    if (ri >= raw.length) break;
    if (mc === "#") {
      result += raw[ri++];
    } else if (mc === "*") {
      result += raw.slice(ri);
      ri = raw.length;
    } else {
      result += mc; // auto-insert literal
    }
  }
  return result;
}

export function unmaskString(masked: string, mask: string) {
  let raw = "";
  let fi = 0;
  for (let mi = 0; mi < mask.length && fi < masked.length; mi++) {
    const mc = mask[mi];
    if (mc === "#") {
      raw += masked[fi++];
    } else if (mc === "*") {
      const literalsAfter = mask.slice(mi + 1).replace(/#|\*/g, "").length;
      const hashesAfter = (mask.slice(mi + 1).match(/#/g) || []).length;
      const take = Math.max(0, masked.length - fi - literalsAfter - hashesAfter);
      raw += masked.slice(fi, fi + take);
      fi += take;
    } else {
      if (masked[fi] === mc) fi++; // skip literal
    }
  }
  return raw;
}
