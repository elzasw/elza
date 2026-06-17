import {
  Button,
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
  Spinner,
  Tooltip,
  tokens,
} from "@fluentui/react-components";
import { DocumentAddRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { DataStructureRef, DataType, NodeItem } from "elza-api";
import { ChangeEvent, useCallback, useEffect, useMemo, useState } from "react";
import { useDebouncedEffect } from "utils/hooks/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useActiveFund } from "../hooks";
import { FIELD_HEIGHT } from "../../../../constants";
import { AnonymousStructure } from "./AnonymousStructure";
import { DescItemProps } from "./types";
import { i18n } from "components";
import AddStructureDataForm from "components/arr/structure/AddStructureDataForm";
import { modalDialogShow } from "actions/global/modalDialog";
import { useAppThunkDispatch } from "utils/hooks";
import DescItemFactory from "components/arr/nodeForm/DescItemFactory";
import { FormattedMessage, defineMessages } from "react-intl";
import { useStyles } from "./styles";

interface Props extends DescItemProps {
  onChange: (item: NodeItemStructureRef) => Promise<void>;
}

interface NodeItemStructureRef extends NodeItem {
  data: DataStructureRef;
}

const messages = defineMessages({
  addNewStructure: {
    id: "desc_item_structure_action_addNewStructure",
    defaultMessage: "Přidat",
  },
});

export function DescItemStructured({
  item,
  typeRef,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Structured && !item.undefined) {
    throw "Incorrect data type";
  }

  const dispatch = useAppThunkDispatch();

  const { id: fundId, versionId: fundVersionId } = useActiveFund();
  const structureTypes = useAppSelector(
    ({ refTables }) =>
      refTables.structureTypes.data?.find(
        ({ versionId }) => versionId === fundVersionId,
      )?.data || [],
  );

  const data = item.data as DataStructureRef;
  const [structure, setStructure] = useState<any>();
  const [structures, setStructures] = useState<any[]>([]);
  const [query, setQuery] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const structureType = useMemo(() => {
    if (typeRef?.structureTypeId != undefined) {
      return structureTypes.find(({ id }) => id === typeRef.structureTypeId);
    }
    return undefined;
  }, [typeRef?.structureTypeId, structureTypes]);

  useEffect(() => {
    if (!data.structuredObjectId || structureType?.anonymous) return;

    let cancelled = false;

    // Poll until structure value is available (may be null while server is processing)
    (async () => {
      while (!cancelled) {
        setIsLoading(true);

        const result = await WebApi.getStructureData(fundVersionId, data.structuredObjectId);
        if (cancelled) return;

        if (result.value != null) {
          setStructure(result);
          setQuery(result.value);
          setIsLoading(false);
          return;
        }

        await new Promise(resolve => setTimeout(resolve, 5000));
      }
    })();

    return () => { cancelled = true; setIsLoading(false); };
  }, [fundVersionId, data.structuredObjectId, structureType?.anonymous]);

  const loadStructures = useCallback(
    async (_query: string) => {
      if (structureType?.code && !structureType.anonymous) {
        const _structures = await WebApi.findStructureData(
          fundVersionId,
          structureType?.code,
          _query === structure?.value ? "" : _query,
        );
        setStructures(_structures.rows);
      }
    },
    [
      fundVersionId,
      structureType?.code,
      structureType?.anonymous,
      structure?.value,
    ],
  );

  function handleQueryChange(e: ChangeEvent<HTMLInputElement>) {
    setQuery(e.currentTarget.value);
  }

  useDebouncedEffect(() => {
    loadStructures(query);
  }, 300, [query, loadStructures]);

  async function handleSelect(_e: SelectionEvents, _data: OptionOnSelectData) {
    setQuery(_data.optionText);
    setStructure(
      structures.find(({ id }) => id === parseInt(_data.optionValue)),
    );

    const id = parseInt(_data.optionValue);
    if (!isNaN(id)) {
      return await handleChange(parseInt(_data.optionValue));
    }
  }

  async function handleChange(id: number) {
    return await onChange({
      ...item,
      data: {
        ...data,
        structuredObjectId: id,
      },
    });
  }

  function addNewStructure() {
    dispatch(
      modalDialogShow(
        this,
        i18n("arr.structure.modal.add.title", structureType.name),
        <AddStructureDataForm
          fundId={fundId}
          fundVersionId={fundVersionId}
          structureTypeCode={structureType.code}
          initialQuery={query}
          descItemFactory={DescItemFactory}
          onConfirm={(structureId) => handleChange(structureId)}
        />,
      ),
    );
  }

  async function handleCreateAnonymousStructure(_structureObjectId: number) {
    await onChange({
      ...item,
      data: {
        ...data,
        structuredObjectId: _structureObjectId,
      },
    });
  }

  if (structureType.anonymous) {
    return (
      <AnonymousStructure
        data={data}
        structureType={structureType}
        onCreate={handleCreateAnonymousStructure}
      />
    );
  }

  const styles = useStyles();
  const isInherited = item.nodeId != nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;

  return (
    <div className={styles.comboboxWrapperNoWidth}>
      {!structureType.anonymous && (
        // <Input
        //   style={{ flex: 1, minWidth: "60px" }}
        //   value={data.structuredObjectId?.toString()}
        // />
        <>
          <Combobox
            size={compact ? "small" : "medium"}
            title={`${query}${structure ? " " + structure?.complement : ""}`}
            value={`${query}`}
            onChange={handleQueryChange}
            onOptionSelect={handleSelect}
            // onOpenChange={(_e, open) => {
            //   if (open) {
            //     fieldRef.current?.setSelectionRange(0, query?.length || 0);
            //   }
            // }}
            // onBlur={handleBlur}
            style={{
              minWidth: "unset",
              flex: 1,
              flexGrow: 5,
              // paddingLeft: "80px",
              paddingRight: compact ? FIELD_HEIGHT.small + 2 : FIELD_HEIGHT.medium + 4,
            }}
            input={{
              style: {
                minWidth: "30px",
                fontSize: "1em",
                textDecoration: item.inhibited ? "line-through" : undefined,
                flex: 1,
                flexBasis: `${(query || "").length + 3}ch`,
                zIndex: 1,
              },
            }}
            listbox={{ style: { maxHeight: "400px", minWidth: "400px" } }}
            disabled={isDisabled || isLoading}
          >
            {structures.map(({ value, complement, id }) => {
              return (
                <Option text={value} value={id.toString()}>
                  <div>
                    <span>{value}</span>
                    &nbsp;
                    <span style={{ opacity: 0.5 }}>{complement}</span>
                  </div>
                </Option>
              );
            })}
          </Combobox>
          {isLoading && (
            <Spinner
              size="tiny"
              style={{
                position: "absolute",
                left: tokens.spacingHorizontalMNudge,
                zIndex: 2,
              }}
            />
          )}
          <div
            style={{
              position: "absolute",
              // right: "70px",
              // left: "15px",
              paddingLeft: `calc(${compact ? tokens.spacingHorizontalSNudge : tokens.spacingHorizontalMNudge} + ${tokens.spacingHorizontalXXS})`,
              maxWidth: `calc(100% - ${(compact ? FIELD_HEIGHT.small : FIELD_HEIGHT.medium) * 2}px)`,
              height: "90%",
              display: "flex",
              alignItems: "center",
              opacity: 0.5,
              background: "var(--shade-0)",
              pointerEvents: "none",
              zIndex: 0,
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
              fontSize: "1em",
            }}
          >
              <div className={styles.structureQueryHidden}>{query}</div>
              <div className={styles.structureComplement}>
                  {structure?.complement}
              </div>
          </div>
          <div className={styles.comboboxActionButton}>
            <Tooltip
              relationship="label"
              appearance="inverted"
              content={<FormattedMessage {...messages.addNewStructure} />}
            >
              <Button
                size={compact ? "small" : "medium"}
                style={{ height: (compact ? FIELD_HEIGHT.small : FIELD_HEIGHT.medium) - 2 }}
                appearance="subtle"
                icon={<DocumentAddRegular />}
                onClick={addNewStructure}
                tabIndex={-1}
              />
            </Tooltip>
          </div>
        </>
      )}
    </div>
  );
}
