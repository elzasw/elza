import { Input, InteractionTag, InteractionTagPrimary, InteractionTagSecondary, Menu, MenuItem, MenuList, MenuPopover, MenuTrigger, Tag, TagDismissData, TagDismissEvent, TagGroup } from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { Icon } from "components"
import { FilterChange } from "./FundFilterModal";
import { Field, Form } from "react-final-form";
import { AbstractFilter, FieldValueFilter, FilterType, FondsFilterField, MultimatchContainsFilter, OperationCompareType } from "elza-api";
import { useRef, useState } from "react";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { useFilterModal } from "./hooks";
import { useSelector } from "react-redux";
import { AppState } from "typings/store";
// import { useTestFluentModal, useTestModal } from "components/shared/dialog/FluentModalDialog";

interface Props {
  onChange: (filters: AbstractFilter[]) => void;
  currentFilters: AbstractFilter[];
}

interface FulltextValues {
  fulltext: string;
}



function isFieldValueFilter(filter: unknown): filter is FieldValueFilter {
  return filter["field"];
}

function formatPosition(x: number, y: number) {
  return { x, y };
}

export function FundFilters({
  onChange,
  currentFilters,
}: Props) {
  const [filters, setFilters] = useState<(FieldValueFilter | MultimatchContainsFilter)[]>(currentFilters as any || []);
  const allInstitutions = useSelector(({ refTables }: AppState) => refTables.institutions.items);

  const showFilterModal = useFilterModal();
  const addFilterButtonRef = useRef<HTMLButtonElement>(null);
  const { formatMessage } = useIntl();

  function getFiltersList(): FondsFilterField[] {
    return [
      FondsFilterField.InstitutionCode,
      FondsFilterField.InternalCode,
      FondsFilterField.FundNumber,
      FondsFilterField.Mark
    ]
  }

  function handleFulltext({ fulltext }: FulltextValues) {
    const _filters = [...filters];
    const index = _filters.findIndex((filter) => !isFieldValueFilter(filter));

    if (index >= 0) {
      _filters.splice(index, 1);
    }

    if (fulltext) {
      _filters.push({
        filterType: "contains",
        value: fulltext,
      });
    }

    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterConfirm({ name, value, operation }: FilterChange) {
    const _filters = [...filters];
    const filter = _filters.find(f => isFieldValueFilter(f) && f.field == name)

    if (!filter || filter[name] != value) {
      _filters.push({
        filterType: FilterType.FieldValue,
        field: name,
        value,
        operation,
      });
    }
    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterReplace({ name, value, operation }: FilterChange, index: number) {
    const _filters = [...filters];

    _filters.splice(index, 1, {
      filterType: FilterType.FieldValue,
      field: name,
      value,
      operation,
    });

    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterRemove(_e: TagDismissEvent, data: TagDismissData<string>) {
    const [field, value] = data.value.split(";");

    const index = filters.findIndex(f => isFieldValueFilter(f) && f.field == field && f.value == value);
    const _filters = [...filters];

    if (index >= 0) {
      _filters.splice(index, 1);
      setFilters(_filters);
      onChange(_filters);
    } else {
      throw Error(`Item not found in filters: ${field};${value}`);
    }
  }

  function formatFilterValue(value: string, field: FondsFilterField) {
    switch (field) {
      case FondsFilterField.InstitutionCode:
        return allInstitutions.find(({ code }) => code === value)?.name || value;
      case FondsFilterField.FundNumber:
      case FondsFilterField.InternalCode:
      case FondsFilterField.Mark:
      default:
        return value;
    }
  }

  function formatOperation(operation: OperationCompareType, field: FondsFilterField) {
    switch (operation) {
      case OperationCompareType.Eq:
        if (field === FondsFilterField.InstitutionCode) {
          return ": "
        }
        return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>=</div>
      case OperationCompareType.Neq:
        return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>≠</div>
      case OperationCompareType.Contains:
        return ": "
      default:
        return operation;
    }
  }

  const addFilterButtonRect = addFilterButtonRef.current?.getBoundingClientRect() || undefined;
  const initialPosition = addFilterButtonRect ? formatPosition(addFilterButtonRect.left, addFilterButtonRect.bottom) : undefined;

  return <div style={{ display: "flex" }}>
    <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <Form<FulltextValues> initialValues={{ fulltext: "" }} onSubmit={handleFulltext}>
        {({ handleSubmit, values, form }) => {
          return <form onSubmit={handleSubmit}>
            <Field name="fulltext">{({ input }) => {
              return <Input
                {...input}
                type="text"
                contentBefore={<Icon glyph="fa-search" />}
                contentAfter={values.fulltext && <Icon onClick={() => {
                  handleFulltext({ fulltext: "" });
                  form.reset()
                }} glyph="fa-times" />}
              />
            }}</Field>
          </form>
        }}
      </Form>
    </div>
    <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <Menu>
        <MenuTrigger disableButtonEnhancement={true}>
          <Tag
            appearance="outline"
            ref={addFilterButtonRef}
          >
            <AddRegular />&nbsp;Filtr
          </Tag>
        </MenuTrigger>
        <MenuPopover>
          <MenuList>
            {getFiltersList().map((fieldName) => {
              return <MenuItem
                onClick={async (e) => {
                  e.stopPropagation();
                  e.preventDefault();

                  const { data } = await showFilterModal({ name: fieldName }, initialPosition)
                  if (data) {
                    handleFilterConfirm({
                      ...data,
                      name: fieldName
                    });
                  }
                }}
              >{formatMessage(messages[fieldName])}</MenuItem>
            })}
          </MenuList>
        </MenuPopover>
      </Menu>
    </div>
    <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <TagGroup onDismiss={handleFilterRemove}>
        {filters.filter((filter) => isFieldValueFilter(filter)).map((filter, index) => {
          if (isFieldValueFilter(filter)) {
            return <InteractionTag
              value={`${filter.field};${filter.value}`}
              key={index}
            >
              <InteractionTagPrimary onClick={async (e) => {
                const rect = e.currentTarget.getBoundingClientRect();

                const { data } = await showFilterModal({
                  name: filter.field,
                  value: filter.value,
                  operation: filter.operation,
                }, formatPosition(rect.left, rect.top + rect.height));
                if (data) {
                  handleFilterReplace({
                    ...data,
                    name: filter.field
                  }, index);
                }
              }}>
                <div style={{ display: "flex", alignItems: "center" }}>
                  <b>{formatMessage(messages[filter.field])}</b>
                  {formatOperation(filter.operation, filter.field)}
                  {formatFilterValue(filter.value, filter.field)}
                </div>
              </InteractionTagPrimary>
              <InteractionTagSecondary aria-label="remove" />
            </InteractionTag>
          }
          return;
        })}
      </TagGroup>
    </div>
  </div>
}
