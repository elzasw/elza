import { Input, InputOnChangeData } from "@fluentui/react-components";
import { useInitialFocus } from "./utils";
import { useRef, useState } from "react";

interface Props {
  onChange: (value: string) => void;
  initialValue?: string;
}

export function FundFilterNumberForm({
  onChange,
  initialValue = "",
}: Props) {
  const [value, setValue] = useState<string>(initialValue);
  const inputRef = useRef(null)
  useInitialFocus(inputRef);

  const handleChange = (_e: React.ChangeEvent, data: InputOnChangeData) => {
    const value = data.value.replace(/\D/g, '');
    setValue(value);
    onChange(value);
  }

  return <Input
    ref={inputRef}
    value={value}
    onChange={handleChange}
  />
}
