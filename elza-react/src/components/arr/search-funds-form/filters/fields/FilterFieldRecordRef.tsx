import { Combobox, Option, OptionOnSelectData, SelectionEvents } from "@fluentui/react-components";
import { WebApi } from "actions";
import { ApAccessPointVO } from "api";
import { RulDescItemSpecExtVO } from "api/RulDescItemSpecExtVO";
import { useEffect, useState } from "react";
import { DescItemTypeRef } from "typings/store";

export interface Props {
  onChange: (value: string, isValid?: boolean, valueLabel?: string) => void;
  value?: string;
  label?: string;
  itemType?: DescItemTypeRef;
  itemSpec?: RulDescItemSpecExtVO;
}

export const FilterFieldRecordRef = ({ onChange, itemType, itemSpec, label = "" }: Props) => {
  const [query, setQuery] = useState(label);
  const [accessPoints, setAccessPoints] = useState<ApAccessPointVO[]>([]);

  useEffect(() => {
    (async () => {
      const accessPoints = await WebApi.findAccessPoint(query, undefined, undefined, undefined, itemType && itemSpec && itemType?.id, itemSpec?.id);
      setAccessPoints(accessPoints.rows);
    })()
  }, [itemType, itemSpec, query])

  const handleAccessPointSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    setQuery(data.optionText);
    onChange(data.optionValue, true, data.optionText);
  }

  return (
    <>
      <Combobox value={query} onChange={(e) => setQuery(e.target.value)} onOptionSelect={handleAccessPointSelect}>
        {accessPoints.map(({ name, id, description }) => {
          return <Option text={name} value={id.toString()}>
            <div>
              <div>{name}</div>
              <div><small>{description}</small></div>
            </div>
          </Option>
        })}
      </Combobox>
    </>
  );
};
