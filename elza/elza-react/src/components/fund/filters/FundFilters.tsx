import { Button, Input, Menu, MenuButton, MenuItem, MenuList, MenuPopover, MenuTrigger, SearchBox, Tag, TagDismissData, TagDismissEvent, TagGroup } from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { Icon } from "components"
import { FilterChange, FundFilterModal } from "./FundFilterModal";
import { Field, Form } from "react-final-form";
import { AbstractFilter, FieldValueFilter, FilterType, FondsFilterField, MultimatchContainsFilter, OperationCompareType } from "elza-api";
import { useRef, useState } from "react";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { useTestFluentModal, useTestModal } from "components/shared/dialog/FluentModalDialog";

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

export function FundFilters({
  onChange,
  currentFilters,
}: Props) {
  const [addFilter, setAddFilter] = useState<FondsFilterField>();
  const [filters, setFilters] = useState<(FieldValueFilter | MultimatchContainsFilter)[]>(currentFilters as any || []);

  const showModal = useTestModal();
  const showFluentModal = useTestFluentModal();

  const addFilterButtonRef = useRef<HTMLButtonElement>(null);
  const { formatMessage } = useIntl();

  function getFiltersList() {
    return [FondsFilterField.InstitutionCode, FondsFilterField.InternalCode, FondsFilterField.FundNumber]
  }

  function handleFulltext({ fulltext }: FulltextValues) {
    const _filters = [...filters];
    const index = _filters.findIndex((filter) => !isFieldValueFilter(filter));

    if (index >= 0) {
      console.log('#fp - remove fulltext', index, _filters[index]);
      _filters.splice(index, 1);
    }

    if (fulltext) {
      console.log('#fp - add fulltext', fulltext);
      _filters.push({
        filterType: "contains",
        value: fulltext,
      });
    }

    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterConfirm(data: FilterChange) {
    const _filters = [...filters];
    const filter = _filters.find(f => isFieldValueFilter(f) && f.field == data.name)

    if (!filter || filter[data.name] != data.value) {
      _filters.push({
        filterType: FilterType.FieldValue,
        field: data.name,
        value: data.value,
        operation: OperationCompareType.Eq,
      });
    }
    setFilters(_filters);
    setAddFilter(undefined);
    onChange(_filters);
  }

  function handleFilterClose() {
    setAddFilter(undefined);
  }

  function handleFilterRemove(_e: TagDismissEvent, data: TagDismissData<string>) {
    const [field, value] = data.value.split(";");
    console.log("#fp - remove filter", field, value, filters);

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

  async function handleTestModal() {
    const { result } = await showModal()
    console.log("#ff - modal result", result);
  }
  async function handleTestFluentModal() {
    const { result } = await showFluentModal()
    console.log("#ff - fluent modal result", result);
  }

  const addFilterButtonRect = addFilterButtonRef.current?.getBoundingClientRect() || undefined;
  const initialPosition = addFilterButtonRect ? { x: addFilterButtonRect.left, y: addFilterButtonRect.bottom } : undefined;

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
      <Button onClick={handleTestModal}>testmodal</Button>
      <Button onClick={handleTestFluentModal}>testfluent</Button>
      <Menu>
        <MenuTrigger disableButtonEnhancement={true}>
          <Tag
            appearance="outline"
            ref={addFilterButtonRef}
          >
            <AddRegular />{/* <Icon glyph="fa-plus" /> */}&nbsp;Filtr
          </Tag>
          {/* <MenuButton */}
          {/*   appearance='subtle' */}
          {/*   icon={<Icon glyph="fa-plus" />} */}
          {/*   ref={addFilterButtonRef} */}
          {/* >Filtr</MenuButton> */}
        </MenuTrigger>
        <MenuPopover>
          <MenuList>
            {getFiltersList().map((fieldName) => {
              return <MenuItem onClick={() => setAddFilter(fieldName)}>{formatMessage(messages[fieldName])}</MenuItem>
            })}
          </MenuList>
        </MenuPopover>
      </Menu>
    </div>
    <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <TagGroup onDismiss={handleFilterRemove}>
        {filters.filter((filter) => isFieldValueFilter(filter)).map((filter, index) => {
          if (isFieldValueFilter(filter)) {
            return <Tag
              dismissible={true}
              dismissIcon={{ "aria-label": "remove" }}
              value={`${filter.field};${filter.value}`}
              key={index}
            >
              {formatMessage(messages[filter.field])}: {filter.value}
            </Tag>
          }
          return;
        })}
      </TagGroup>
    </div>
    <FundFilterModal initialPosition={initialPosition} filterName={addFilter} onFilterChange={handleFilterConfirm} onClose={handleFilterClose} />
    {/* {JSON.stringify(filters)} */}
  </div>
}
