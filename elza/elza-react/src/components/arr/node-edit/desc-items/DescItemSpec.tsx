import {
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
  Tooltip,
} from "@fluentui/react-components";
import { CircleFilled, CircleHalfFillFilled, CircleHalfFillRegular, CircleOffRegular } from "@fluentui/react-icons";
import { FormItemSpec, FormItemType, MandatoryType } from "elza-api";
import { ComponentType, useEffect, useRef, useState } from "react";
import { defineMessages, MessageDescriptor, useIntl } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useStrictMode } from "../hooks";
import { findInSources } from "./utils";
import { RulDescItemSpecExtVO } from "api/RulDescItemSpecExtVO";

const mandatoryTypeMessages = defineMessages({
  [MandatoryType.Required]: { id: "mandatoryType.required", defaultMessage: "Povinný" },
  [MandatoryType.Recommended]: { id: "mandatoryType.recommended", defaultMessage: "Doporučený" },
  [MandatoryType.Possible]: { id: "mandatoryType.possible", defaultMessage: "Možný" },
  [MandatoryType.Impossible]: { id: "mandatoryType.impossible", defaultMessage: "Nemožný" },
});

interface IndicatorConfig {
  message: MessageDescriptor;
  icon: ComponentType<{ style?: React.CSSProperties }>;
  color: string;
}

const mandatoryTypeConfig: Record<string, IndicatorConfig> = {
  [MandatoryType.Required]: {
    message: mandatoryTypeMessages[MandatoryType.Required],
    icon: CircleFilled,
    color: "var(--color-blue)",
  },
  [MandatoryType.Recommended]: {
    message: mandatoryTypeMessages[MandatoryType.Recommended],
    icon: CircleHalfFillRegular,
    color: "var(--color-green)",
  },
  [MandatoryType.Possible]: {
    message: mandatoryTypeMessages[MandatoryType.Possible],
    icon: CircleHalfFillFilled,
    color: "transparent",
  },
  [MandatoryType.Impossible]: {
    message: mandatoryTypeMessages[MandatoryType.Impossible],
    icon: CircleOffRegular,
    color: "var(--color-red)",
  },
};

const INDICATOR_SIZE = 10;
const INDICATOR_GAP = 8;

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
  const { formatMessage } = useIntl();
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
  const mandatoryTypeOrder: Record<string, number> = {
    [MandatoryType.Required]: 0,
    [MandatoryType.Recommended]: 1,
    [MandatoryType.Possible]: 2,
    [MandatoryType.Impossible]: 3,
  };

  const specs = allSpecs
    .filter(
      // Hide impossible when in strict mode
      ({ form }) =>
        !strictMode || (form && form?.type != MandatoryType.Impossible),
    )
    .sort((a, b) => {
      const orderA = mandatoryTypeOrder[a.form?.type] ?? 3;
      const orderB = mandatoryTypeOrder[b.form?.type] ?? 3;
      return orderA - orderB;
    });

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

    if (!_query || _query === getLabel(spec)) {
      setFilteredSpecs(specs);
    } else {
      const filteredSpecs = specs.filter(({ rule: { name, shortcut } }) => {
        return findInSources(_query, [name, shortcut]);
      });
      setFilteredSpecs(filteredSpecs);
    }

    setQuery(_query);
  }

  useEffect(() => {
      setListboxMinWidth(comboboxRef.current?.offsetWidth);
  }, [])

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
          const specType = form?.type;
          const config = specType ? mandatoryTypeConfig[specType] : mandatoryTypeConfig[MandatoryType.Impossible];
          const label = getLabel({rule, form});
          const isImpossible = !form || specType === MandatoryType.Impossible;
          const showIndicator = isImpossible || (specType && specType !== MandatoryType.Possible);
          const Icon = config.icon;
          return (
            <Option
              value={rule.id.toString()}
              text={label}
            >
                <div style={{display: 'flex', alignItems: 'center', gap: INDICATOR_GAP, marginLeft: -(INDICATOR_SIZE + INDICATOR_GAP)}}>
                    <Tooltip content={formatMessage(config.message)} relationship="label" appearance="inverted" positioning="before" visible={showIndicator ? undefined : false}>
                        <div><Icon
                            style={{ width: INDICATOR_SIZE, height: INDICATOR_SIZE, color: showIndicator ? config.color : "transparent", flexShrink: 0 }}
                        /></div>
                    </Tooltip>
                      <div
                          style={{
                            opacity: isImpossible ? 0.6 : undefined,
                            fontWeight: rule.id === value ? "bold" : undefined,
                          }}
                      >
                          {label}
                      </div>
                </div>
            </Option>
          );
        })}
      </Combobox>
    </div>
  );
}
