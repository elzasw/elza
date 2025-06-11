import { Input, InputOnChangeData } from "@fluentui/react-components";
import { ChangeEvent } from "react";

export interface Props {
  onChange: (value: string) => void;
  value?: string;
}
export const FilterFieldText = ({ onChange, value = "" }: Props) => {
  const handleChange = (_e: ChangeEvent<HTMLInputElement>, data: InputOnChangeData) => {
    onChange(data.value);
  }

  return (
    <Input value={value} onChange={handleChange} />
  );
};
