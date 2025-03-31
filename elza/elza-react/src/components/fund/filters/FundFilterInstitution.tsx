import { Combobox, OptionOnSelectData, SelectionEvents, Option } from "@fluentui/react-components";
import { WebApi } from "actions";
import { useEffect, useState } from "react";

interface InstitutionType {
  id: number;
  code: string;
  name: string;
}

interface Institution {
  id: number;
  intitutionType: InstitutionType;
  accessPointId: number;
  name: string;
  code: string;
}

interface InstitutionProps {
  onChange: (value: string) => void;
}

export function FundFilterInstitutionForm({ onChange }: InstitutionProps) {
  const [institutions, setInstitutions] = useState<Institution[]>([]);

  useEffect(() => {
    (async () => {
      const _institutions: Institution[] = await WebApi.getInstitutions(true);
      setInstitutions(_institutions)
    })()
  }, [])

  const handleInstitutionSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    onChange(data.optionValue);
  }

  return <Combobox onOptionSelect={handleInstitutionSelect}>
    {institutions.map(({ name, id }) => {
      return <Option key={id} value={id.toString()}>{name}</Option>
    })}
  </Combobox>
}
