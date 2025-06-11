import { Input, InputOnChangeData } from "@fluentui/react-components";
import { validateUnitDate } from "components/registry/field/UnitdateField";
import { ChangeEvent, useState } from "react";

export interface Props {
  onChange: (value: string, isValid?: boolean) => void;
  value?: string;
}
export const FilterFieldUnitdate = ({ onChange, value = "" }: Props) => {
  const [error, setError] = useState("");

  const handleChange = (_e: ChangeEvent<HTMLInputElement>, data: InputOnChangeData) => {
    const { valid, message } = validateUnitDate(data.value);
    if (!valid) setError(message);
    else setError("");

    onChange(data.value, valid);
  }

  return (
    <>
      <Input value={value} onChange={handleChange} />
      {error && <div style={{ color: "red" }}>{error}</div>}
    </>
  );
};
