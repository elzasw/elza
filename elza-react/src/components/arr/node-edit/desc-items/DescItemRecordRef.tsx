import {
  Button,
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
  Spinner,
  tokens,
  Tooltip,
} from "@fluentui/react-components";
import { DatabasePersonRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { refRecordTypesFetchIfNeeded } from "actions/refTables/recordTypes";
import { ApAccessPointVO } from "api";
// import { urlEntity } from "../../../../constants";
import { DataRecordRef, DataType, NodeItem } from "elza-api";
import { useEffect, useRef, useState } from "react";
import { useDebouncedEffect } from "utils/hooks/hooks";
import { DescItemProps } from "./types";
import {
  goToAe,
  registryDetailClear,
  registryListFilter,
} from "actions/registry/registry";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { modalDialogShow } from "actions/global/modalDialog";
import { useActiveFund } from "../hooks";
import { RegistrySelectPage } from "pages";
import classNames from "classnames";
import { FIELD_HEIGHT, MODAL_DIALOG_VARIANT } from "../../../../constants";
import { FormattedMessage, defineMessages, useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useStyles } from "./styles";
// import { Link } from "react-router-dom";
// import { Input } from "@fluentui/react-components";

interface Props extends DescItemProps {
  onChange: (item: NodeItemRecordRef) => Promise<void>;
}

interface NodeItemRecordRef extends NodeItem {
  data: DataRecordRef;
}

const messages = defineMessages({
  openInAccessPoints: {
    id: "desc_item_record_ref_action_openInAccessPoints",
    defaultMessage: "Otevřít v archivních entitách",
  },
  noResults: {
    id: "desc_item_record_ref_no_results",
    defaultMessage: "Žádné výsledky",
  },
  startTyping: {
    id: "desc_item_record_ref_start_typing",
    defaultMessage: "Začněte psát pro vyhledávání",
  },
});

export function DescItemRecordRef({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  typeRef,
  selectedSpecId,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.RecordRef && !item.undefined) {
    throw "Incorrect data type";
  }

  const { formatMessage } = useIntl();
  const itemTypeId = item.itemTypeId;
  const itemSpecId = item.itemSpecId != undefined ? item.itemSpecId : selectedSpecId;
  const dispatch = useAppThunkDispatch();
  const registryList: any = useAppSelector(({ app }) => app.registryList); // TODO add types
  const registryDetail = useAppSelector(({ app }) => app.registryDetail); // TODO add types
  const apTypesMap = useAppSelector(({ refTables }) => refTables.recordTypes.itemsMap);
  const activeFund = useActiveFund();
  const styles = useStyles();

  const [query, setQuery] = useState<string>(
    item.undefined ? formatMessage(commonMessages.undefined) : "",
  );
  const [accessPoints, setAccessPoints] = useState<ApAccessPointVO[]>([]);
  const [accessPoint, setAccessPoint] = useState<ApAccessPointVO>();
  const [isFocused, setIsFocused] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const fieldRef = useRef<HTMLInputElement>(null);

  const data = item.data as DataRecordRef;

  const handleAccessPointSelect = (
    _e: SelectionEvents,
    data: OptionOnSelectData,
  ) => {
    if(!data.optionValue){return;}
    setQuery(data.optionText);
    onChange({
      ...item,
      data: {
        ...item.data,
        value: parseInt(data.optionValue),
      },
    });
  };

  function handleBlur() {
    setIsFocused(false);
    if (accessPoint?.name) {
      setQuery(accessPoint.name);
    }
  }

  useEffect(() => {
    if (data?.value) {
      (async () => {
        const _accessPoint = await WebApi.getAccessPoint(data?.value);
        setAccessPoint(_accessPoint);
        setQuery(_accessPoint.name);
      })();
    } else if (item.undefined) {
      setQuery(formatMessage(commonMessages.undefined));
    } else {
      setQuery("");
    }
  }, [data?.value, item.undefined]);

  useDebouncedEffect(() => {
    const hasEnoughCharacters = (query?.length ?? 0) >= 1;
    if (
      hasEnoughCharacters
      && !item.undefined
      && item.nodeId === nodeId
      && (!typeRef.useSpecification || itemSpecId != undefined) // spec id is required for types that use specification
    ) {
      (async () => {
        // clear before fetching so no option stays active across the result swap;
        // this lets the Combobox re-highlight the first item when the new list mounts
        setAccessPoints([]);
        setIsLoading(true);
        try {
          const accessPoints = await WebApi.findAccessPoint(
            query,
            undefined,
            undefined,
            activeFund.versionId,
            itemTypeId,
            itemSpecId,
          );
          setAccessPoints(accessPoints.rows);
        } finally {
          setIsLoading(false);
        }
      })();
    } else {
      setAccessPoints([]);
      setIsLoading(false);
    }
  }, 500, [
    itemTypeId,
    itemSpecId,
    query,
    item.undefined,
    item.nodeId,
    nodeId,
    activeFund?.versionId,
    typeRef.useSpecification,
  ]);

  useEffect(() => {
    dispatch(refRecordTypesFetchIfNeeded());
  }, []);

  function handleSelectModule(readOnly = false) {
    // const {hasSpecification, descItem, registryList, fund, nodeName, itemName, specName, history, dispatch} = this.props;
    const oldFilter = { ...registryList.filter };
    const oldAccessPointId = registryDetail.id;
    const specName = typeRef.descItemSpecs.find(
      ({ id }) => id === selectedSpecId || id === item.itemSpecId,
    )?.name;

    dispatch(
      registryListFilter({
        ...registryList.filter,
        registryTypeId: null,
        itemTypeId: typeRef.id,
        text: query,
        itemSpecId: typeRef.useSpecification
          ? selectedSpecId || item.itemSpecId
          : null,
        versionId: activeFund?.versionId,
      }),
    );

    // preselect entity, when value exists
    if (accessPoint?.id != undefined) {
      dispatch(
        goToAe(history, accessPoint ? accessPoint.id : null, false, false),
      );
    }

    dispatch(
      modalDialogShow(
        undefined,
        null,
        <RegistrySelectPage
          //@ts-expect-error TODO wrong type definitions
          titles={[
            activeFund?.name,
            "nodeName",
            typeRef.name + (typeRef.useSpecification ? ": " + specName : ""),
          ]}
          fund={activeFund}
          readOnly={readOnly}
          onSelect={(data: ApAccessPointVO) => {
            setQuery(data.name);
            onChange({
              ...item,
              data: {
                ...item.data,
                value: data.id,
              },
            });
            dispatch(registryListFilter({ ...oldFilter }));
            dispatch(registryDetailClear());
            if (oldAccessPointId != undefined) {
              dispatch(goToAe(history, oldAccessPointId, false, false));
            }
          }}
        />,
        classNames(
          "dialog-fullscreen-inset",
          MODAL_DIALOG_VARIANT.NO_HEADER,
        ),
        () => {
          dispatch(registryListFilter({ ...oldFilter }));
        },
      ),
    );
  }

  const isInherited = item.nodeId != nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;

  return (
    <div className={styles.comboboxWrapper}>
      <Combobox
        size={compact ? "small" : "medium"}
        title={query}
        value={query}
        selectedOptions={data?.value != null ? [data.value.toString()] : []}
        onChange={(e) => {
          const value = e.target.value;
          setQuery(value);
          // clear stale results immediately; the debounced effect refetches
          setAccessPoints([]);
          // show the spinner right away while waiting for the debounced fetch,
          // unless the query is too short to trigger a search
          setIsLoading(value.length >= 1);
        }}
        onOptionSelect={handleAccessPointSelect}
        onOpenChange={(_e, open) => {
          if (open) {
            fieldRef.current?.setSelectionRange(0, query?.length || 0);
          }
        }}
        onFocus={() => setIsFocused(true)}
        onBlur={handleBlur}
        style={{
          minWidth: "unset",
          flex: 1,
          flexGrow: 5,
          padding: 0,
          display: "flex",
        }}
        input={{
          ref: fieldRef,
          style: {
            minWidth: "30px",
            width: "30px",
            fontSize: "1em",
            textDecoration:
              item.inhibited || (!isFocused && data?.value == null)
                ? "line-through"
                : undefined,
            flex: 1,
            flexBasis: `${(query || "").length + 3}ch`,
          },
        }}
        listbox={{ style: { maxHeight: "400px", minWidth: "400px" } }}
        expandIcon={{
          style: { height: "100%", position: "relative", overflow: "hidden", flexShrink: 0 },
          children: (
            <Tooltip
              relationship="label"
              appearance="inverted"
              content={<FormattedMessage {...messages.openInAccessPoints} />}
            >
              <Button
                size={compact ? "small" : "medium"}
                appearance="subtle"
                disabled={
                  (typeRef.useSpecification && item.itemSpecId == undefined && selectedSpecId == undefined) ||
                  (isDisabled && item.undefined)
                }
                style={{
                  height: "100%",
                }}
                icon={<DatabasePersonRegular />}
                onMouseDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  handleSelectModule(isDisabled);
                }}
                tabIndex={-1}
              />
            </Tooltip>
          ),
        }}
        disabled={isDisabled}
      >
        {accessPoints.map(({ name, id, description, typeId, ...rest }) => {
          const typeName = (apTypesMap as any)?.[typeId]?.name;
          return (
            <Option text={name} value={id.toString()} >
              <div>
                <div>{name}</div>
                <div>
                  <div className={styles.dataPillWrapper}>
                    <div className={styles.dataPill} >
                      <small>{id}</small>
                    </div>
                    {typeName && (
                      <>
                        <Tooltip content={typeName} showDelay={1000} relationship="label" appearance="inverted">
                          <div className={styles.dataPill} >
                            <small>{typeName}</small>
                          </div>
                        </Tooltip>
                      </>
                    )}
                  </div>
                  <div>
                    <small>{description}</small>
                  </div>
                </div>
              </div>
            </Option>
          );
        })}
        {isLoading && (
          <div className={styles.noResults}>
            <Spinner size="tiny" />
          </div>
        )}
        {!isLoading && accessPoints.length === 0 && (
          <div className={styles.noResults}>
            {(query?.length ?? 0) < 1 ? (
              <FormattedMessage {...messages.startTyping} />
            ) : (
              <FormattedMessage {...messages.noResults} />
            )}
          </div>
        )}
      </Combobox>
    </div>
  );
}
