import { RulDescItemSpecExtVO } from "api/RulDescItemSpecExtVO";
import { DescItemTypeRef } from "typings/store";

export interface FilterValueFieldProps {
  onChange: (value: string, isValid?: boolean, valueLabel?: string) => void;
  value: string;
  label?: string; // text value for descItems with reference id or code in value
  itemType?: DescItemTypeRef;
  itemSpec?: RulDescItemSpecExtVO;
}
