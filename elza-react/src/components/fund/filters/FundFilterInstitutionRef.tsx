import { Combobox, OptionOnSelectData, SelectionEvents, Option } from "@fluentui/react-components";
import { WebApi } from "actions";
import { useEffect, useRef, useState } from "react";
import { useInitialFocus } from "./utils";
import { useSelector } from "react-redux";
import { AppState } from "typings/store";

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
  initialValue?: string;
}

export function FundFilterInstitutionForm({
  onChange,
  initialValue = "",
}: InstitutionProps) {
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [query, setQuery] = useState<string>("");
  // const [value, setValue] = useState<string>("");
  const allInstitutions = useSelector(({ refTables }: AppState) => refTables.institutions.items)

  const inputRef = useRef(null)
  useInitialFocus(inputRef);

  useEffect(() => {
    (async () => {
      const _institutions: Institution[] = await WebApi.getInstitutions(true);
      const initialInstitution = allInstitutions.find(({ code }) => code === initialValue);
      if (initialInstitution) {
        setQuery(initialInstitution.name);
      }
      setInstitutions(_institutions)
    })()
  }, [initialValue, allInstitutions])

  const handleInstitutionSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    setQuery(data.optionText);
    // setValue(data.optionText);
    onChange(data.optionValue);
  }

  return <Combobox ref={inputRef} clearable={true} value={query} onChange={(e) => setQuery(e.target.value)} onOptionSelect={handleInstitutionSelect}>
    {institutions.filter((institution) => institution.name.toLowerCase().indexOf(query.toLowerCase()) >= 0).map(({ name, id, code }) => {
      return <Option key={id} value={code.toString()}>{name}</Option>
    })}
  </Combobox>
}
