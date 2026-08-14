import {
  Button,
  Combobox,
  Input,
  Option,
  Textarea,
  Tooltip,
} from "@fluentui/react-components";
import { ArrowEnterRegular, SearchRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { routerNavigate } from "actions/router";
import { DataType, DataUriRef, NodeItem } from "elza-api";
import { useEffect, useMemo, useState } from "react";
import { ArrRefTemplateVO } from "types";
import { useAppThunkDispatch } from "utils/hooks";
import { ELZA_SCHEME_NODE } from "../../../../constants";
import { useActiveFund } from "utils/hooks";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { findInSources, useValueManager } from "./utils";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { SelectSearchFundsForm } from "components/arr/search-funds-form/SelectSearchFundsForm";
import { i18n } from "components";
import { FormattedMessage, defineMessages, useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useStyles } from "./styles";

interface Props extends DescItemProps {
  onChange: (item: NodeItemUriRef) => Promise<void>;
}

interface NodeItemUriRef extends NodeItem {
  data: DataUriRef;
}

const messages = defineMessages({
  description: {
    id: "desc_item_uri_ref_description",
    defaultMessage: "Popisek",
  },
  uri: {
    id: "desc_item_uri_ref_uri",
    defaultMessage: "Odkaz",
  },
  refTemplate: {
    id: "desc_item_uri_ref_refTemplate",
    defaultMessage: "Šablona synchronizace",
  },
  fundSearch: {
    id: "desc_item_uri_ref_fundSearch",
    defaultMessage: "Vyhledat v archivních souborech",
  },
  goToUri: {
    id: "desc_item_uri_ref_goToUri",
    defaultMessage: "Přejít na odkaz",
  },
});

export function DescItemUriRef({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.UriRef && !item.undefined) {
    throw "Incorrect data type";
  }

  const { formatMessage } = useIntl();
  const styles = useStyles();
  const dispatch = useAppThunkDispatch();
  const activeFund = useActiveFund();
  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;

  const data = item.data as DataUriRef;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data.value, item);

  const {
    value: description,
    setValue: setDescription,
    isDirty: isDescriptionDirty,
    conflictValue: descConflictValue,
    initialValue: descInitialValue,
    resetConflict: descResetConflict,
    finishChange: descFinishChange,
  } = useValueManager<string>(data.description, item);

  const [query, setQuery] = useState<string>("");
  const [templates, setTemplates] = useState<ArrRefTemplateVO[]>([]);

  useEffect(() => {
    (async function () {
      const templates = await WebApi.getRefTemplates(activeFund.id);
      setTemplates(templates);
    })();
  }, []);

  const filteredTemplates = useMemo(() => {
    return templates.filter(({ name }) => {
      return findInSources(query, [name]);
    });
  }, [query, templates]);

  async function handleChange(force?: boolean, refTemplateId?: number) {
    if (
      ((value && initialValue !== value) ||
        (description && descInitialValue !== description) ||
        refTemplateId != undefined) &&
      (!conflictValue || force)
    ) {
      await onChange({
        ...item,
        data: {
          ...item.data,
          value,
          description,
          refTemplateId,
        },
      });
      finishChange();
      descFinishChange();
    }
  }

  async function resolveConflict(resetValue?: boolean) {
    if (!resetValue) {
      await handleChange(true);
    }
    resetConflict();
  }

  async function descResolveConflict(resetValue?: boolean) {
    if (!resetValue) {
      await handleChange(true);
    }
    descResetConflict();
  }

  function handleValueInputChange({
    currentTarget,
  }: React.ChangeEvent<HTMLInputElement>) {
    // console.log(currentTarget);
    setValue(currentTarget.value);
  }

  function handleDescInputChange({
    currentTarget,
  }: React.ChangeEvent<HTMLInputElement>) {
    // console.log(currentTarget);
    setDescription(currentTarget.value);
  }

  function handleNavigate() {
    if (value?.startsWith(ELZA_SCHEME_NODE)) {
      if (data?.nodeId) {
        dispatch(routerNavigate(`/node/${data.nodeId}`));
      }
    } else {
      window.open(value, "_blank");
    }
  }

  function handleSearch() {
    dispatch(
      modalDialogShow(
        this,
        formatMessage(messages.fundSearch),
        <SelectSearchFundsForm
          onSubmit={async ({ node, fund }) => {
            // TODO new api
            const { uuid, fundName, name } = await WebApi.getNode(
              fund.fundVersionId,
              node.id,
            );
            await onChange({
              ...item,
              data: {
                ...item.data,
                value: ELZA_SCHEME_NODE + "://" + uuid,
                description:
                  fund.id !== activeFund.id ? fundName + "; " + name : name,
              },
            });
            dispatch(modalDialogHide());
          }}
        />,
      ),
    );
  }

  return (
    <div className={styles.descItemContainer}>
      <div className={styles.columnFlex}>
        <div className={styles.columnFlexRelative}>
          <Input
            size={compact ? "small" : "medium"}
            disabled={isDisabled}
            value={
              item.undefined ? formatMessage(commonMessages.undefined) : (value || "")
            }
            onChange={handleValueInputChange}
            onBlur={() => handleChange()}
            placeholder={formatMessage(messages.uri)}
            style={{
              flexGrow: 2,
              marginBottom: "4px",
              fontSize: "1em",
              textDecoration: item.inhibited ? "line-through" : undefined,
            }}
            contentAfter={
              <>
                <Tooltip
                  relationship="label"
                  appearance="inverted"
                  content={<FormattedMessage {...messages.fundSearch} />}
                >
                  <Button
                    size={compact ? "small" : "medium"}
                    appearance="subtle"
                    icon={<SearchRegular />}
                    onClick={handleSearch}
                    tabIndex={-1}
                  />
                </Tooltip>
                <Tooltip
                  relationship="label"
                  appearance="inverted"
                  content={<FormattedMessage {...messages.goToUri} />}
                >
                  <Button
                    size={compact ? "small" : "medium"}
                    appearance="subtle"
                    icon={<ArrowEnterRegular />}
                    onClick={handleNavigate}
                    tabIndex={-1}
                  />
                </Tooltip>
              </>
            }
          />
          {isDirty && <EditStateDisplay />}
          <ConflictValue
            value={value?.toString()}
            conflictValue={conflictValue?.toString()}
            isDirty={isDirty}
            onResolve={resolveConflict}
          >
            {(conflictValue) => (
              <Textarea
                size={compact ? "small" : "medium"}
                className={styles.conflictTextareaWithMinWidth}
                value={conflictValue}
                readOnly={true}
                style={{ fontSize: "1em" }}
              />
            )}
          </ConflictValue>
        </div>
        {!item.undefined && (
          <div className={styles.columnFlexRelative}>
            <Input
              size={compact ? "small" : "medium"}
              disabled={isDisabled || !data?.value}
              value={description}
              onChange={handleDescInputChange}
              onBlur={() => handleChange()}
              style={{
                flexGrow: 2,
                marginBottom: "4px",
                fontSize: "1em",
                textDecoration: item.inhibited ? "line-through" : undefined,
              }}
              placeholder={formatMessage(messages.description)}
            />
            {isDescriptionDirty && <EditStateDisplay />}
            <ConflictValue
              value={description?.toString()}
              conflictValue={descConflictValue?.toString()}
              isDirty={isDescriptionDirty}
              onResolve={descResolveConflict}
            >
              {(conflictValue) => (
                <Textarea
                  size={compact ? "small" : "medium"}
                  className={styles.conflictTextareaWithMinWidth}
                  value={conflictValue}
                  readOnly={true}
                  style={{ fontSize: "1em" }}
                />
              )}
            </ConflictValue>
          </div>
        )}
        {templates.length > 0 && !item.undefined && (
          <Combobox
            size={compact ? "small" : "medium"}
            title={query}
            value={query}
            placeholder={formatMessage(messages.refTemplate)}
            onChange={(e) => setQuery(e.target.value)}
            onOptionSelect={(_e, data) => {
              const _refTemplateId = parseInt(data.optionValue);
              if (!isNaN(_refTemplateId)) {
                handleChange(true, _refTemplateId);
              }
            }}
            onOpenChange={(_e, open) => {
              if (open) {
                // fieldRef.current?.setSelectionRange(0, query?.length || 0);
              }
            }}
            className={styles.comboboxUriRef}
            input={{
              // ref: fieldRef,
              style: {
                minWidth: "30px",
                fontSize: "1em",
                textDecoration: item.inhibited ? "line-through" : undefined,
                flex: 1,
                flexBasis: `${(query || "").length + 3}ch`,
              },
            }}
            listbox={{ style: { maxHeight: "400px", minWidth: "400px" } }}
            disabled={isDisabled}
          >
            {filteredTemplates.map(({ name, id }) => {
              return (
                <Option text={name} value={id.toString()}>
                  <div>
                    <div>{name}</div>
                  </div>
                </Option>
              );
            })}
          </Combobox>
        )}
      </div>
    </div>
  );
}
