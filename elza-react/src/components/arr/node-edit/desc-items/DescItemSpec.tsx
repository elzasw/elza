import {
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
} from "@fluentui/react-components";
import { FormItemSpec, FormItemType, MandatoryType } from "elza-api";
import { useEffect, useRef, useState } from "react";
import { DescItemTypeRef } from "typings/store";
import { useStrictMode } from "../hooks";
import { findInSources } from "./utils";
import { RulDescItemSpecExtVO } from "api/RulDescItemSpecExtVO";

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

  function getLabel(spec: {form:FormItemSpec, rule: RulDescItemSpecExtVO}){
    if (!spec) { return ""; }
    return spec?.rule[labelSource] || spec?.rule.name || `spec_${spec?.rule.id}`;
  }

  const allSpecs = refSpecs.map((refSpec) => ({
    form: formSpecs.find(({ itemSpecId }) => itemSpecId === refSpec.id),
    rule: refSpec,
  }));
  const specs = allSpecs.filter(
    // Hide impossible when in strict mode
    ({ form }) =>
      !strictMode || (form && form?.type != MandatoryType.Impossible),
  );

  const spec = allSpecs.find(({ rule }) => rule && rule.id === value);

  // const [selectedSpec, setSelectedSpec] = useState(value);
  const [query, setQuery] = useState(getLabel(spec));
  const [filteredSpecs, setFilteredSpecs] = useState(specs);
  const [listboxMinWidth, setListboxMinWidth] = useState(undefined);
  const fieldRef = useRef<HTMLInputElement>(null);
  const comboboxRef = useRef<HTMLInputElement>(null);

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

    if (_query && _query != getLabel(spec)) {
      const filteredSpecs = specs.filter(({ rule: { name, shortcut } }) => {
        return findInSources(_query, [name, shortcut]);
      });
      setFilteredSpecs(filteredSpecs);
    }

    setQuery(_query);
  }

  useEffect(() => {
      setListboxMinWidth(comboboxRef.current?.offsetWidth);
  }, [comboboxRef.current?.offsetWidth])

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
        root={{ ref: comboboxRef }}
        selectedOptions={spec ? [spec.rule.code] : []}
        value={isUndefined ? "výjimka" : query}
        disabled={isDisabled}
        onChange={handleQueryChange}
        onOpenChange={(_e, open) => {
          if (open) {
            fieldRef.current?.setSelectionRange(0, query?.length || 0);
            if (query == getLabel(spec)) {
              setFilteredSpecs(specs);
            }
          }
        }}
        onBlur={() => {
          setQuery(getLabel(spec));
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
        positioning={{
            matchTargetSize: undefined,
            // position: "above",
            align: "start"
        }}
        listbox={{
            style: {
                minWidth: `${listboxMinWidth || 100}px`,
                maxHeight: "400px",
                maxWidth: "300px",
            },
        }}
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
              {getLabel({rule, form})}
            </Option>
          );
        })}
      </Combobox>
    </div>
  );
}
