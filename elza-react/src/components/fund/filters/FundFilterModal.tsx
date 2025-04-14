import { Button, Menu, MenuButton, MenuItem, MenuPopover, MenuTrigger, ToggleButton } from "@fluentui/react-components";
import { CheckmarkRegular, DismissRegular } from "@fluentui/react-icons";
import { DraggableWindow } from "components/shared";
import { Position } from "components/shared/draggable-window";
import { useCallback, useRef, useState } from "react";
import { FundFilterInstitutionForm } from "./FundFilterInstitutionRef";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { FondsFilterField, OperationCompareType } from "elza-api";
import { FundFilterTextForm } from "./FundFilterText";
import { FundFilterNumberForm } from "./FundFilterNumber";

export interface FilterChange {
  name: FondsFilterField;
  value: string;
  operation: OperationCompareType;
}

interface Props {
  filterName: FondsFilterField;
  onFilterChange: (data: FilterChange) => void;
  onClose: () => void;
  initialPosition?: Position;
  initialValue?: Partial<FilterChange>;
}

const getAvailableOperations = (field: FondsFilterField) => {
  const availableOperations: Partial<Record<FondsFilterField, OperationCompareType[]>> = {
    [FondsFilterField.InstitutionCode]: [OperationCompareType.Eq, OperationCompareType.Neq],
    [FondsFilterField.Mark]: [OperationCompareType.Contains, OperationCompareType.Eq],
    [FondsFilterField.InternalCode]: [OperationCompareType.Contains, OperationCompareType.Eq],
    [FondsFilterField.FundNumber]: [OperationCompareType.Eq],
  }
  return availableOperations[field] || [];
}

export function FundFilterModal({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialPosition,
  initialValue = { value: "" },
}: Props) {
  const availableOperations = getAvailableOperations(filterName);

  const [value, setValue] = useState<string>(initialValue.value);
  const [operation, setOperation] = useState<OperationCompareType>(initialValue.operation || availableOperations?.[0] || OperationCompareType.Eq);
  const { formatMessage } = useIntl();
  const modalRef = useRef<HTMLDivElement>(null);

  const isDirty = value != initialValue.value || (initialValue.operation && operation != initialValue.operation);

  const handleFilterChange = useCallback((e: React.MouseEvent | React.KeyboardEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (value && isDirty) {
      onFilterChange({
        name: filterName,
        value,
        operation,
      });
    }
  }, [filterName, onFilterChange, value, operation, isDirty]);

  const handleClose = useCallback(() => {
    onClose();
  }, [onClose])

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Escape") {
      handleClose();
    }
    else if (e.key === "Enter") {
      handleFilterChange(e)
    }
  }

  function getForm(filterName: FondsFilterField) {
    switch (filterName) {
      case FondsFilterField.InstitutionCode:
        return <FundFilterInstitutionForm
          onChange={(_value) => setValue(_value)}
          initialValue={value}
        />
      case FondsFilterField.FundNumber:
        return <FundFilterNumberForm
          onChange={(_value) => setValue(_value)}
          initialValue={value}
        />
      case FondsFilterField.InternalCode:
      case FondsFilterField.Mark:
      default:
        return <FundFilterTextForm
          onChange={(_value) => setValue(_value)}
          initialValue={value}
        />
    }
  }

  if (!filterName) {
    return <></>
  }

  return <DraggableWindow disableDrag={true} initialPosition={initialPosition}>
    <div ref={modalRef} onKeyDown={onKeyDown} style={{
      padding: '10px 12px',
      background: 'var(--shade-0)',
      borderRadius: '5px',
      boxShadow: '0 0 6px 0 #0003',
      zIndex: 10,
    }} >
      <div>
        <label>
          {formatMessage(messages[filterName])}
        </label>
        {availableOperations.length > 1 &&
          <div style={{ marginBottom: "5px" }}>
            <Menu>
              <MenuTrigger>
                <MenuButton size="small">
                  {formatMessage(messages[operation])}
                </MenuButton>
              </MenuTrigger>
              <MenuPopover>
                {availableOperations.map((_operation) => {
                  return <MenuItem
                    // checked={operation == _operation}
                    onClick={() => setOperation(_operation)}
                  // size="small"
                  >{formatMessage(messages[_operation])}</MenuItem>
                })}
              </MenuPopover>

            </Menu>
          </div>
          // <div style={{ marginBottom: "5px" }}>
          //
          //   {availableOperations.map((_operation) => {
          //     return <ToggleButton
          //       checked={operation == _operation}
          //       onClick={() => setOperation(_operation)}
          //       size="small"
          //     >{_operation}</ToggleButton>
          //   })}
          // </div>
        }
        <div>
          {getForm(filterName)}
        </div>
      </div>
      <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "10px" }}>
        <Button appearance="primary" disabled={value == "" || !isDirty} icon={<CheckmarkRegular />} onClick={handleFilterChange}>Potvrdit</Button>
        <Button appearance="subtle" icon={<DismissRegular />} onClick={handleClose}></Button>
      </div>
    </div>
    <div style={{ position: "fixed", background: "#0002", width: "100vw", height: "100vh", top: 0, left: 0, zIndex: -1 }} onClick={handleClose}></div>
  </DraggableWindow>;
}

