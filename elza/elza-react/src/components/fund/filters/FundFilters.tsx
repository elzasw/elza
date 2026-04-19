import { Input, InteractionTag, InteractionTagPrimary, InteractionTagSecondary, Menu, MenuItem, MenuList, MenuPopover, MenuTrigger, Tag, TagDismissData, TagDismissEvent, TagGroup, makeStyles, tokens } from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { Icon } from "components"
import { Field, Form } from "react-final-form";
import { AbstractFilter, FilterType, FondsFieldName } from "elza-api";
import { useRef, useState } from "react";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { useFilterModal } from "./hooks";
import { FilterObject } from "./types";

interface Props {
  onChange: (filters: AbstractFilter[]) => void;
  currentFilters: FilterObject[];
}

interface FulltextValues {
  fulltext: string;
}

function formatPosition(x: number, y: number) {
  return { x, y };
}

const useStyles = makeStyles({
  tagGroup: {
    display: "flex",
    flexWrap: "wrap",
    rowGap: tokens.spacingVerticalXS,
  }
})

export function FundFilters({
  onChange,
  currentFilters,
}: Props) {
  const [filters, setFilters] = useState<FilterObject[]>(currentFilters as any || []);

  const showFilterModal = useFilterModal();
  const addFilterButtonRef = useRef<HTMLButtonElement>(null);
  const { formatMessage } = useIntl();
  const styles = useStyles();

  function getFiltersList(): FondsFieldName[] {
    return [
      FondsFieldName.InstitutionCode,
      FondsFieldName.InternalCode,
      FondsFieldName.FondsNumber,
      FondsFieldName.Mark,
      FondsFieldName.Name,
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
        data: { value: fulltext },
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

  function handleFilterConfirm(filter: FilterObject) {
    const _filters = [...filters];
    const _filter = _filters.find(f => f.name == filter.name)

    if (!_filter || _filter.getSerializedString(_filter) != filter.getSerializedString(filter)) {
      _filters.push(filter);
    }
    setFilters(_filters);
    onChange(_filters);
  }

  function handleFilterReplace(filter: FilterObject, index: number) {
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

  async function handleFilterEdit(e: React.MouseEvent, filter: FilterObject, index: number) {
    const rect = e.currentTarget.getBoundingClientRect();

    const { data } = await showFilterModal(filter, formatPosition(rect.left, rect.top + rect.height));
    if (data) {
      handleFilterReplace({
        ...data,
        name: filter.name
      }, index);
    }
  }

  const addFilterButtonRect = addFilterButtonRef.current?.getBoundingClientRect() || undefined;
  const initialPosition = addFilterButtonRect ? formatPosition(addFilterButtonRect.left, addFilterButtonRect.bottom) : undefined;

  const fulltextFilter = currentFilters?.find((filter) => filter.filterType === FilterType.Contains);
  const fulltextValue = fulltextFilter?.getSerializedString(fulltextFilter) || "";

  return <div style={{ display: "flex", flexGrow: 1, alignItems: "flex-start" }}>
    <div style={{ display: "flex", alignItems: "center", margin: "5px" }}>
      <Form<FulltextValues> initialValues={{ fulltext: fulltextValue }} onSubmit={handleFulltext}>
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
                key={fieldName}
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
      <TagGroup className={styles.tagGroup} onDismiss={handleFilterRemove}>
        {filters.filter(({ filterType }) => filterType === FilterType.FieldValue).map((filter, index) => {
          return <InteractionTag
            value={JSON.stringify({field: filter.name, value: filter.getSerializedString(filter)})}
            key={index}
          >
            <InteractionTagPrimary onClick={async (e) => {
              handleFilterEdit(e, filter, index);
            }}>
              <div style={{ display: "flex", alignItems: "center" }}>
                {filter.getDisplayValue(filter)}
              </div>
            </InteractionTagPrimary>
            <InteractionTagSecondary aria-label="remove" />
          </InteractionTag>
        })}
      </TagGroup>
    </div>
  </div>
}
