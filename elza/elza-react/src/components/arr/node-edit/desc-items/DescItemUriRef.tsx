import {
  Button,
  Combobox,
  Input,
  Option,
  Textarea,
} from "@fluentui/react-components";
import { ArrowEnterRegular, SearchRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { routerNavigate } from "actions/router";
import { DataType, DataUriRef, NodeItem } from "elza-api";
import { useEffect, useMemo, useState } from "react";
import { ArrRefTemplateVO } from "types";
import { useAppThunkDispatch } from "utils/hooks";
import { ELZA_SCHEME_NODE } from "../../../../constants";
import { useActiveFund } from "../hooks";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { findInSources, useValueManager } from "./utils";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { SelectSearchFundsForm } from "components/arr/search-funds-form/SelectSearchFundsForm";
import { i18n } from "components";

interface Props extends DescItemProps {
  onChange: (item: NodeItemUriRef) => Promise<void>;
}

interface NodeItemUriRef extends NodeItem {
  data: DataUriRef;
}

export function DescItemUriRef({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.UriRef && !item.undefined) {
    throw "Incorrect data type";
  }

  const dispatch = useAppThunkDispatch();
  const activeFund = useActiveFund();
  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined || isInherited || item.inhibited || _isDisabled;

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
    console.log("#diur - navigate", value, data?.nodeId);
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
        i18n("arr.fund.title.search"),
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
    <div
      style={{
        display: "flex",
        flex: 1,
        position: "relative",
        flexDirection: "column",
      }}
    >
      <div style={{ display: "flex", flexDirection: "column" }}>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            position: "relative",
          }}
        >
          <Input
            disabled={isDisabled}
            value={item.undefined ? "Výjimka" : value}
            onChange={handleValueInputChange}
            onBlur={() => handleChange()}
            style={{
              flexGrow: 2,
              marginBottom: "4px",
              textDecoration: item.inhibited ? "line-through" : undefined,
            }}
            contentAfter={
              <>
                <Button
                  appearance="subtle"
                  icon={<SearchRegular />}
                  onClick={handleSearch}
                />
                <Button
                  appearance="subtle"
                  icon={<ArrowEnterRegular />}
                  onClick={handleNavigate}
                />
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
                style={{ borderColor: "var(--color-red)", minWidth: "100px" }}
                value={conflictValue}
                readOnly={true}
              />
            )}
          </ConflictValue>
        </div>
        {!item.undefined && (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              position: "relative",
            }}
          >
            <Input
              disabled={isDisabled || !data?.value}
              value={description}
              onChange={handleDescInputChange}
              onBlur={() => handleChange()}
              style={{
                flexGrow: 2,
                marginBottom: "4px",
                textDecoration: item.inhibited ? "line-through" : undefined,
              }}
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
                  style={{ borderColor: "var(--color-red)", minWidth: "100px" }}
                  value={conflictValue}
                  readOnly={true}
                />
              )}
            </ConflictValue>
          </div>
        )}
        {templates.length > 0 && !item.undefined && (
          <Combobox
            title={query}
            value={query}
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
            style={{
              minWidth: "unset",
              // maxWidth: "60px",
              flex: 1,
              flexGrow: 1,
              // paddingRight: "37px",
            }}
            input={{
              // ref: fieldRef,
              style: {
                minWidth: "30px",
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
