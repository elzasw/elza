import { Button, Divider, Input, InteractionTag, InteractionTagPrimary, InteractionTagSecondary, Menu, MenuItem, MenuList, MenuPopover, MenuTrigger, Tag, TagDismissData, TagDismissEvent, TagGroup, Tooltip, makeStyles, tokens } from "@fluentui/react-components";
import { AddRegular, DismissRegular, ArrowSyncRegular } from "@fluentui/react-icons";
import { Icon } from "components"
import { Field, Form } from "react-final-form";
import { FieldType, FilterType, FondsFieldName, NodeFieldName, OperationCompareType } from "elza-api";
import { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { useFilterModal } from "./hooks";
import { FilterEntry, MultiFilterObject } from "./types";
import { DescItemValue } from "./FilterDescItemModal";
import { useSelector } from "react-redux";
import { AppState, DescItemTypeRef } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import { descItemTypesFetchIfNeeded } from "actions/refTables/descItemTypes";
import { SettingsType } from "api/settings/SettingsType";

interface Props {
  onChange: (filters: MultiFilterObject[]) => void;
  onRefresh: () => void;
  currentFilters: MultiFilterObject[];
}

interface FulltextValues {
  fulltext: string;
}

function formatPosition(x: number, y: number) {
  return { x, y };
}

const useStyles = makeStyles({
  tagsContainer: {
    display: "flex",
    alignItems: "center",
    margin: "5px",
    flex: "1 1 100%",
    minWidth: 0,
    maxWidth: "100%",
    overflow: "hidden",
  },
  tagGroup: {
    display: "flex",
    flexWrap: "wrap",
    rowGap: tokens.spacingVerticalXS,
    maxWidth: "100%",
    minWidth: 0,
    width: "100%",
  },
  tag: {
    maxWidth: "100%",
    minWidth: 0,
    "& > *": {
      maxWidth: "100%",
      minWidth: 0,
    },
    "& *": {
      minWidth: 0,
    },
    // Targets Fluent UI's internal InteractionTagPrimary content slot so we can
    // enforce overflow/ellipsis on rich JSX content. The class name is an
    // implementation detail of @fluentui/react-components and may need updating
    // on major Fluent UI upgrades.
    '& [class*="__primaryText"]': {
      display: "block",
      overflow: "hidden",
      minWidth: 0,
      flex: "1 1 auto",
    },
  },
  tagContent: {
    display: "block",
    overflow: "hidden",
    whiteSpace: "nowrap",
    textOverflow: "ellipsis",
    minWidth: 0,
    maxWidth: "100%",
  },
  tagDismiss: {
    flexShrink: 0,
  },
})

interface SearchNodeFilterSetting {
  itemType: string;
  itemSpec?: string | null;
  operation?: OperationCompareType;
  name?: string;
  fixedField?: boolean | null;
}

interface SearchNodeFilterSettings {
  options: SearchNodeFilterSetting[];
}

function convertSearchNodeFilterSettings(valueString: string): SearchNodeFilterSettings {
  return JSON.parse(valueString);
}

export function NodeSearchFilters({
  onChange,
  onRefresh,
  currentFilters,
}: Props) {
  const [filters, setFilters] = useState<MultiFilterObject[]>(currentFilters as any || []);

  const showFilterModal = useFilterModal();
  const addFilterButtonRef = useRef<HTMLButtonElement>(null);
  const { formatMessage } = useIntl();
  const styles = useStyles();
  // userDetail.settings || [] used to prevent error when default user is used
  const filterSettings = useSelector(({ userDetail }: AppState) => (userDetail.settings || []).filter(({ settingsType }) => settingsType === SettingsType.SEARCH_NODE_FILTERS))
  const descItemTypes = useSelector(({ refTables }: AppState) => refTables.descItemTypes.items);

  const dispatch = useThunkDispatch();
  // Load used refTables data, if not present
  useEffect(() => {
    dispatch(descItemTypesFetchIfNeeded());
  }, [])

  function getPresetFilters() {
    const presetFilters: SearchNodeFilterSetting[] = [];
    filterSettings.forEach(({ value }) => {
      presetFilters.push(...convertSearchNodeFilterSettings(value).options);
    })
    return presetFilters;
  }

  function formatPresetFilter(presetFilter: SearchNodeFilterSetting): Partial<MultiFilterObject<DescItemValue>> {
    const presetType = descItemTypes.find(({ code }) => presetFilter.itemType === code);
    const presetSpec = presetType?.descItemSpecs.find(({ code }) => presetFilter.itemSpec === code);

    return {
      name: "DescItem",
      data: presetType ? [{
        value: {
          itemType: presetType,
          itemSpec: presetSpec || undefined,
          itemValue: undefined,
          itemLabel: undefined,
        },
        operation: presetFilter.operation!,
      }] : [],
    };
  }

  const presetMenuFilters = getPresetFilters()?.filter(({ fixedField }) => !fixedField);
  const presetFixedFilters = getPresetFilters()?.filter(({ fixedField }) => fixedField);

  function getNodeFiltersList(): string[] {
    return [
      NodeFieldName.ConformityError,
      NodeFieldName.ConformityMissing,
      NodeFieldName.Uuid,
    ]
  }

  function handleFulltext({ fulltext }: FulltextValues) {
    const _filters = [...filters];
    const index = _filters.findIndex((filter) => filter.filterType === FilterType.Contains);

    if (index >= 0) {
      _filters.splice(index, 1);
    }

    if (fulltext) {
      _filters.push({
        filterType: FilterType.Contains,
        data: [{ value: fulltext, operation: OperationCompareType.Contains }],
        getFilterValue: () => ({
          filterType: FilterType.Contains,
          value: fulltext,
        }),
        getSerializedString: () => fulltext,
        getDisplayValue: () => fulltext,
      });
    }

    setFilters(_filters);
    onChange(_filters);
  }

  function handleFixedFilter({
    name,
    itemType,
    itemSpec,
    value,
    operation
  }: {
    value: string,
    itemType: DescItemTypeRef,
    itemSpec: string,
    name: string,
    operation: OperationCompareType,
  }) {
    const _filters = [...filters];
    const index = _filters.findIndex((filter) => filter.name === name);

    // replace or delete when filter already exists
    if (index >= 0) {
      _filters.splice(index, 1);
    }

    if (value) {
      _filters.push({
        name,
        filterType: FilterType.FieldValue,
        data: [{
          value: {
            itemType,
            itemSpec: itemType.descItemSpecs?.find(({ code }) => code === itemSpec),
            itemValue: value,
            itemLabel: value,
          },
          operation,
        }],
        getFilterValue: () => ({
          filterType: FilterType.FieldValue,
          field: {
            fieldType: FieldType.DescItem,
            typeCode: itemType.code,
            specCode: itemSpec,
          },
          operation,
          value,
        }),
        getSerializedString: () => `${name}:${value}`,
        getDisplayValue: () => `${name}:${value}`,
      });
    }

    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterConfirm(filter: MultiFilterObject) {
    const _filters = [...filters];
    const _filter = _filters.find(f => {
      f.getSerializedString(f) == filter.getSerializedString(filter)
    }
    )

    if (!_filter || _filter.getSerializedString(_filter) != filter.getSerializedString(filter)) {
      _filters.push(filter);
    }
    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterReplace(filter: MultiFilterObject, index: number) {
    const _filters = [...filters];

    _filters.splice(index, 1, filter);

    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterRemove(_e: TagDismissEvent, data: TagDismissData<string>) {
    const {field, value} = JSON.parse(data.value) as {field:string; value:string};

    const index = filters.findIndex(f => f.name == field && f.getSerializedString(f) == value);
    const _filters = [...filters];

    if (index >= 0) {
      _filters.splice(index, 1);
      setFilters(_filters);
      onChange(_filters);
    } else {
      throw Error(`Item not found in filters: ${field};${value}`);
    }
  }

  async function handleFilterEdit(e: React.MouseEvent, filter: MultiFilterObject, index: number) {
    const rect = e.currentTarget.getBoundingClientRect();

    const { data } = await showFilterModal(filter, formatPosition(rect.left, rect.top + rect.height));
    if (data) {
      handleFilterReplace({
        ...data,
        name: filter.name
      }, index);
    }
  }

  const fulltextFilter = currentFilters?.find((filter) => filter.filterType === FilterType.Contains);
  const fulltextValue = fulltextFilter?.getSerializedString(fulltextFilter) || "";

  return <div style={{ display: "flex", flexWrap: "wrap" }}>
    {<div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <Button appearance="outline" onClick={onRefresh} icon={<ArrowSyncRegular />} disabled={currentFilters.length === 0} />
    </div>}
    <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <Form<FulltextValues> initialValues={{ fulltext: fulltextValue }} onSubmit={handleFulltext}>
        {({ handleSubmit, values, form }) => {
          return <form onSubmit={handleSubmit}>
            <Field name="fulltext">{({ input }) => {
              return <Input
                {...input}
                type="text"
                contentBefore={<Icon glyph="fa-search" />}
                contentAfter={values.fulltext && <DismissRegular onClick={() => {
                  handleFulltext({ fulltext: "" });
                  form.reset()
                }} />}
              />
            }}</Field>
          </form>
        }}
      </Form>
    </div>
    {presetFixedFilters.length > 0 && presetFixedFilters.map(({ name, itemType, itemSpec, operation }) => {
      const _itemType = descItemTypes.find(({ code }) => itemType === code);
      const initialValues = { value: "", itemType: _itemType, itemSpec, name, operation }
      return <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
        <Form initialValues={initialValues} onSubmit={handleFixedFilter}>
          {({ handleSubmit, values, form }) => {
            return <form onSubmit={handleSubmit}>
              <Field name="value">{({ input }) => {
                return <Input
                  {...input}
                  placeholder={name}
                  type="text"
                  contentBefore={<Icon glyph="fa-filter" />}
                  contentAfter={values.value && <DismissRegular onClick={() => {
                    handleFixedFilter(initialValues);
                    form.reset()
                  }} />}
                />
              }}</Field>
            </form>
          }}
        </Form>
      </div>
    })
    }
    <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <Menu>
        <MenuTrigger disableButtonEnhancement={true}>
          <Tag
            appearance="outline"
            ref={addFilterButtonRef}
            style={{ cursor: "pointer" }}
          >
            <AddRegular />&nbsp;Filtr
          </Tag>
        </MenuTrigger>
        <MenuPopover>
          <MenuList>
            {presetMenuFilters.length > 0 && <>
              {presetMenuFilters.map((filter) => {
                return <MenuItem
                  onClick={async (e) => {
                    e.stopPropagation();
                    e.preventDefault();

                    const addFilterButtonRect = addFilterButtonRef.current?.getBoundingClientRect() || undefined;
                    const initialPosition = addFilterButtonRect ? formatPosition(addFilterButtonRect.left, addFilterButtonRect.bottom) : undefined;

                    const _presetFilter = formatPresetFilter(filter);

                    const { data } = await showFilterModal(_presetFilter, initialPosition)
                    if (data) {
                      handleFilterConfirm({
                        ...data,
                        name: _presetFilter.name
                      });
                    }
                  }}
                >{filter.name}</MenuItem>
              })}
              <Divider />
            </>}
            {["DescItem"].map((fieldName) => {
              return <MenuItem
                onClick={async (e) => {
                  e.stopPropagation();
                  e.preventDefault();

                  const addFilterButtonRect = addFilterButtonRef.current?.getBoundingClientRect() || undefined;
                  const initialPosition = addFilterButtonRect ? formatPosition(addFilterButtonRect.left, addFilterButtonRect.bottom) : undefined;

                  const { data } = await showFilterModal({ name: fieldName }, initialPosition)
                  if (data) {
                    handleFilterConfirm({
                      ...data,
                      name: fieldName
                    });
                  }
                }}
              >{messages[fieldName] ? formatMessage(messages[fieldName]) : fieldName}</MenuItem>
            })}
            <Divider />
            {[FondsFieldName.InstitutionId, FondsFieldName.FondsId].map((fieldName) => {
              return <MenuItem
                key={fieldName}
                onClick={async (e) => {
                  e.stopPropagation();
                  e.preventDefault();

                  const addFilterButtonRect = addFilterButtonRef.current?.getBoundingClientRect() || undefined;
                  const initialPosition = addFilterButtonRect ? formatPosition(addFilterButtonRect.left, addFilterButtonRect.bottom) : undefined;

                  const { data } = await showFilterModal({ name: fieldName }, initialPosition)
                  if (data) {
                    handleFilterConfirm({
                      ...data,
                      name: fieldName
                    });
                  }
                }}
              >{messages[fieldName] ? formatMessage(messages[fieldName]) : fieldName}</MenuItem>
            })}
            {getNodeFiltersList().length > 0 && <Divider />}
            {getNodeFiltersList().map((fieldName) => {
              return <MenuItem
                onClick={async (e) => {
                  e.stopPropagation();
                  e.preventDefault();

                  const addFilterButtonRect = addFilterButtonRef.current?.getBoundingClientRect() || undefined;
                  const initialPosition = addFilterButtonRect ? formatPosition(addFilterButtonRect.left, addFilterButtonRect.bottom) : undefined;

                  const { data } = await showFilterModal({ name: fieldName }, initialPosition)
                  if (data) {
                    handleFilterConfirm({
                      ...data,
                      name: fieldName
                    });
                  }
                }}
              >{messages[fieldName] ? formatMessage(messages[fieldName]) : fieldName}</MenuItem>
            })}
          </MenuList>
        </MenuPopover>
      </Menu>
    </div>
    <div className={styles.tagsContainer}>
      <TagGroup className={styles.tagGroup} onDismiss={handleFilterRemove}>
        {filters.filter(({ filterType, name }) =>
          filterType !== FilterType.Contains // hide fulltext filter
          && presetFixedFilters.find(({ name: _name }) => _name !== name) // hide fixed filters
        ).map((filter, index) => {
          const serialized = filter.getSerializedString(filter);
          const displayValue = filter.getDisplayValue(filter);
          return <InteractionTag
            value={JSON.stringify({field: filter.name, value: serialized})}
            key={index}
            className={styles.tag}
          >
            <Tooltip appearance="inverted" content={<>{displayValue}</>} relationship="description" withArrow showDelay={1000}>
              <InteractionTagPrimary
                onClick={async (e) => {
                  handleFilterEdit(e, filter, index);
                }}
              >
                <div className={styles.tagContent}>
                  {displayValue}
                </div>
              </InteractionTagPrimary>
            </Tooltip>
            <InteractionTagSecondary aria-label="remove" className={styles.tagDismiss} />
          </InteractionTag>
        })}
      </TagGroup>
    </div>
  </div>
}
