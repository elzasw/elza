import {
  Button,
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
  Tooltip,
} from "@fluentui/react-components";
import { DatabasePersonRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { ApAccessPointVO } from "api";
// import { urlEntity } from "../../../../constants";
import { DataRecordRef, DataType, NodeItem } from "elza-api";
import { useEffect, useRef, useState } from "react";
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
import { MODAL_DIALOG_VARIANT } from "../../../../constants";
import { FormattedMessage, defineMessages } from "react-intl";
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
});

export function DescItemRecordRef({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  typeRef,
  selectedSpecId,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.RecordRef) {
    throw "Incorrect data type";
  }

  const itemTypeId = item.itemTypeId;
  const itemSpecId = item.itemSpecId;
  const dispatch = useAppThunkDispatch();
  const registryList: any = useAppSelector(({ app }) => app.registryList); // TODO add types
  const registryDetail = useAppSelector(({ app }) => app.registryDetail); // TODO add types
  const activeFund = useActiveFund();

  const [query, setQuery] = useState<string>(
    item.undefined ? "výjimka" : undefined,
  );
  const [accessPoints, setAccessPoints] = useState<ApAccessPointVO[]>([]);
  const [accessPoint, setAccessPoint] = useState<ApAccessPointVO>();

  const fieldRef = useRef<HTMLInputElement>(null);

  const data = item.data as DataRecordRef;

  const handleAccessPointSelect = (
    _e: SelectionEvents,
    data: OptionOnSelectData,
  ) => {
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
    setQuery(accessPoint?.name || "");
  }

  useEffect(() => {
    if (data?.value) {
      (async () => {
        const _accessPoint = await WebApi.getAccessPoint(data?.value);
        setAccessPoint(_accessPoint);
        setQuery(_accessPoint.name);
      })();
    } else if (item.undefined) {
      setQuery("výjimka");
    } else {
      setQuery("");
    }
  }, [data?.value, item.undefined]);

  useEffect(() => {
    if (!item.undefined && item.nodeId === nodeId) {
      (async () => {
        const accessPoints = await WebApi.findAccessPoint(
          query,
          undefined,
          undefined,
          activeFund.versionId,
          (itemSpecId != undefined && itemTypeId) || undefined,
          itemSpecId,
        );
        setAccessPoints(accessPoints.rows);
      })();
    }
  }, [
    itemTypeId,
    itemSpecId,
    query,
    item.undefined,
    item.nodeId,
    nodeId,
    activeFund?.versionId,
  ]);

  function handleSelectModule() {
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
          MODAL_DIALOG_VARIANT.FULLSCREEN,
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
    <div
      style={{
        position: "relative",
        display: "inline-flex",
        flex: 1,
        alignItems: "center",
      }}
    >
      <Combobox
        title={query}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onOptionSelect={handleAccessPointSelect}
        onOpenChange={(_e, open) => {
          if (open) {
            fieldRef.current?.setSelectionRange(0, query?.length || 0);
          }
        }}
        onBlur={handleBlur}
        style={{
          minWidth: "unset",
          flex: 1,
          flexGrow: 5,
          paddingRight: "37px",
        }}
        input={{
          ref: fieldRef,
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
        {accessPoints.map(({ name, id, description }) => {
          return (
            <Option text={name} value={id.toString()}>
              <div>
                <div>{name}</div>
                <div>
                  <small>{description}</small>
                </div>
              </div>
            </Option>
          );
        })}
      </Combobox>
      <div
        style={{
          position: "absolute",
          right: "1px",
        }}
      >
        <Tooltip
          relationship="label"
          appearance="inverted"
          content={<FormattedMessage {...messages.openInAccessPoints} />}
        >
          <Button
            style={{ height: "29px" }}
            appearance="subtle"
            disabled={
              (item.itemSpecId == undefined && selectedSpecId == undefined) ||
              isDisabled
            }
            icon={<DatabasePersonRegular />}
            onClick={handleSelectModule}
            tabIndex={-1}
          />
        </Tooltip>
      </div>
    </div>
  );
}
