import {
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
} from "@fluentui/react-components";
import { FormItemType, MandatoryType } from "elza-api";
import { useRef, useState } from "react";
import { DescItemTypeRef } from "typings/store";
import { useStrictMode } from "../hooks";
import { findInSources } from "./utils";

interface Props {
  value: number;
  isDisabled: boolean;
  isInhibited: boolean;
  isUndefined?: boolean;
  onChange: (specId: number) => void;
  typeRef: DescItemTypeRef;
  typeForm: FormItemType;
  autoSize?: boolean;
  isSpec?: boolean;
  labelSource?: "shortcut" | "name";
}

export function DescItemSpec({
  value,
  isDisabled,
  isInhibited,
  isUndefined,
  onChange,
  typeRef,
  typeForm,
  autoSize = true,
  isSpec = true,
  labelSource = "shortcut",
}: Props) {
  const strictMode = useStrictMode();

  const formSpecs = typeForm.specs;
  const refSpecs = typeRef.descItemSpecs;

  const specs = refSpecs
    .map((refSpec) => ({
      form: formSpecs.find(({ itemSpecId }) => itemSpecId === refSpec.id),
      rule: refSpec,
    }))
    .filter(
      // Hide impossible when in strict mode
      ({ form }) =>
        !strictMode || (form && form?.type != MandatoryType.Impossible),
    );

  const spec = specs.find(({ rule }) => rule && rule.id === value);

  // const [selectedSpec, setSelectedSpec] = useState(value);
  const [query, setQuery] = useState(spec?.rule[labelSource] || "");
  const [filteredSpecs, setFilteredSpecs] = useState(specs);
  const fieldRef = useRef<HTMLInputElement>(null);

  function handleOptionSelect(_e: SelectionEvents, data: OptionOnSelectData) {
    if (!data.optionValue) {
      return;
    }
    const specId = parseInt(data.optionValue);
    if (specId != undefined) {
      onChange(specId);
      setQuery(data.optionText);
    }
  }

  function handleQueryChange({
    currentTarget,
  }: React.ChangeEvent<HTMLInputElement>) {
    const _query = currentTarget.value;

    if (_query && _query != spec?.rule[labelSource]) {
      const filteredSpecs = specs.filter(({ rule: { name, shortcut } }) => {
        return findInSources(_query, [name, shortcut]);
      });
      setFilteredSpecs(filteredSpecs);
    }

    setQuery(_query);
  }

  return (
    <div
      style={{
        marginRight: isSpec ? "4px" : undefined,
        flexShrink: 2,
        display: "flex",
        flexGrow: autoSize ? 0 : 1,
        flexBasis: autoSize ? `${query ? query.length + 6 : 10}ch` : undefined,
      }}
    >
      <Combobox
        selectedOptions={spec ? [spec.rule.code] : []}
        value={isUndefined ? "výjimka" : query}
        disabled={isDisabled}
        onChange={handleQueryChange}
        onOpenChange={(_e, open) => {
          if (open) {
            fieldRef.current?.setSelectionRange(0, query?.length || 0);
            if (query == spec?.rule[labelSource]) {
              setFilteredSpecs(specs);
            }
          }
        }}
        onBlur={() => {
          setQuery(spec?.rule[labelSource] || "");
        }}
        onOptionSelect={handleOptionSelect}
        multiselect={false}
        style={{
          minWidth: "unset",
          flex: 1,
        }}
        input={{
          ref: fieldRef,
          style: {
            minWidth: "30px",
            textDecoration: isInhibited ? "line-through" : undefined,
          },
        }}
        listbox={{ style: { maxHeight: "400px" } }}
      >
        {filteredSpecs.map(({ rule, form }) => {
          const isImpossible = !form;
          return (
            <Option
              style={{
                textDecoration: isImpossible ? "line-through" : undefined,
              }}
              value={rule.id.toString()}
            >
              {rule[labelSource] || rule.name}
            </Option>
          );
        })}
      </Combobox>
    </div>
  );
}
