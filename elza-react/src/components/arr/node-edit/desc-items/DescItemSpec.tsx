import {
  Combobox,
  Option,
  OptionGroup,
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
import { useStyles } from "./styles";

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
const MAX_SPEC_LABEL_LENGTH = 30;

interface Props {
  value: number;
  isDisabled: boolean;
  isInhibited: boolean;
  isUndefined?: boolean;
  onChange: (specId: number) => void;
  typeRef: DescItemTypeRef;
  typeForm?: FormItemType;
  autoSize?: boolean;
  isSpec?: boolean;
  labelSource?: "shortcut" | "name";
  compact?: boolean;
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
  compact,
}: Props) {
  const { formatMessage } = useIntl();
  const styles = useStyles();
  const strictMode = useStrictMode();

  const formSpecs = typeForm?.specs ?? [];
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
  const maxSpecLabelLength = specs.reduce((longest, currentSpec) => Math.max(longest, getLabel(currentSpec).length), 0);
  const totalLabelLength = specs.reduce((total, currentSpec) => total + getLabel(currentSpec).length, 0);
  const averageSpecLabelLength = specs.length > 0 ? Math.round(totalLabelLength / specs.length) : 0;
  const longestSpecLabelLength = maxSpecLabelLength <= MAX_SPEC_LABEL_LENGTH ? maxSpecLabelLength : averageSpecLabelLength;

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

  function renderOption({ rule, form }: { rule: RulDescItemSpecExtVO, form?: FormItemSpec }) {
    const specType = form?.type;
    const config = specType ? mandatoryTypeConfig[specType] : mandatoryTypeConfig[MandatoryType.Impossible];
    const label = getLabel({ rule, form });
    const isImpossible = !form || specType === MandatoryType.Impossible;
    const showIndicator = isImpossible || (specType && specType !== MandatoryType.Possible);
    const Icon = config.icon;
    return (
      <Option key={rule.id} value={rule.id.toString()} text={label}>
        <div style={{ display: "flex", alignItems: "center", gap: INDICATOR_GAP, marginLeft: -(INDICATOR_SIZE + INDICATOR_GAP) }}>
          <Tooltip content={formatMessage(config.message)} relationship="label" appearance="inverted" positioning="before" visible={showIndicator ? undefined : false}>
            <div>
              <Icon style={{ width: INDICATOR_SIZE, height: INDICATOR_SIZE, color: showIndicator ? config.color : "transparent", flexShrink: 0 }} />
            </div>
          </Tooltip>
          <div style={{ opacity: isImpossible ? 0.6 : undefined, fontWeight: rule.id === value ? "bold" : undefined }}>
            {label}
          </div>
        </div>
      </Option>
    );
  }

  function renderSpecOptions(specsToRender: Array<{ rule: RulDescItemSpecExtVO; form?: FormItemSpec }>) {
    const favoriteSpecIds = typeForm?.favoriteSpecIds || [];
    const favoriteSet = new Set(favoriteSpecIds);

    if (favoriteSet.size === 0) {
      return specsToRender.map(renderOption);
    }

    const favoriteSpecs = specsToRender
      .filter(({ rule }) => favoriteSet.has(rule.id))
      .sort((a, b) => favoriteSpecIds.indexOf(a.rule.id) - favoriteSpecIds.indexOf(b.rule.id));
    const otherSpecs = specsToRender.filter(({ rule }) => !favoriteSet.has(rule.id));

    return (
      <>
        {favoriteSpecs.length > 0 && (
          <OptionGroup label={formatMessage({ id: "subNodeForm.descItemType.spec.favorite", defaultMessage: "Oblíbené" })}>
            {favoriteSpecs.map(renderOption)}
          </OptionGroup>
        )}
        {otherSpecs.length > 0 && (
          <OptionGroup label={formatMessage({ id: "subNodeForm.descItemType.spec.all", defaultMessage: "Vše" })}>
            {otherSpecs.map(renderOption)}
          </OptionGroup>
        )}
      </>
    );
  }

  return (
    <div
      style={{
        marginRight: isSpec ? "4px" : undefined,
        flexShrink: 2,
        display: "flex",
        flexGrow: autoSize ? 0 : 1,
        flexBasis: autoSize ? `${longestSpecLabelLength ? longestSpecLabelLength + 6 : 10}ch` : undefined,
        maxWidth: isSpec && "50%",
      }}
    >
      <Combobox
        size={compact ? "small" : "medium"}
        root={{ ref: comboboxRef }}
        selectedOptions={spec ? [spec.rule.code] : []}
        value={isUndefined ? "výjimka" : query}
        title={query}
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
        className={styles.comboboxSpecWrapper}
        input={{
          ref: fieldRef,
          style: {
            minWidth: "30px",
            fontSize: "1em",
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
        {renderSpecOptions(filteredSpecs)}
      </Combobox>
    </div>
  );
}
