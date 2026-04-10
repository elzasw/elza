import { Button, Menu, MenuButton, MenuItem, MenuPopover, MenuTrigger } from "@fluentui/react-components";
import { CheckmarkRegular, DismissRegular } from "@fluentui/react-icons";
import { OperationCompareType } from "elza-api";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { PropsWithChildren } from "react";

export interface Props {
  onClose?: () => void;
  onFilterConfirm?: () => void;
  onOperationChange?: (operation: OperationCompareType) => void;
  availableOperations?: OperationCompareType[];
  operation?: OperationCompareType;
  isValid?: boolean;
  isDirty?: boolean;
  filterName?: string;
}

export function FilterWindow({
  onClose = () => { return; },
  onFilterConfirm = () => { return; },
  onOperationChange = () => { return; },
  availableOperations = [],
  operation,
  isValid,
  isDirty,
  filterName,
  children,
}: PropsWithChildren<Props>) {
  const { formatMessage } = useIntl();

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Escape") {
      e.preventDefault()
      e.stopPropagation()
      onClose();
    }
    else if (e.key === "Enter") {
      e.preventDefault()
      e.stopPropagation()
      onFilterConfirm()
    }
  }

  return <>
    <div onKeyDown={onKeyDown} style={{
      padding: '10px 12px',
      background: 'var(--shade-0)',
      borderRadius: '5px',
      boxShadow: '0 0 6px 0 #0003',
      zIndex: 10,
    }} >
      <div>
        <label>
          {filterName}
          {/* {formatMessage(messages[filterName])} */}
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
                    onClick={() => onOperationChange(_operation)}
                  >{formatMessage(messages[_operation])}</MenuItem>
                })}
              </MenuPopover>
            </Menu>
          </div>
        }
        <div>
          {children}
        </div>
      </div>
      <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "10px" }}>
        <Button appearance="primary" disabled={!isValid || !isDirty} icon={<CheckmarkRegular />} onClick={onFilterConfirm}>Potvrdit</Button>
        <Button appearance="subtle" icon={<DismissRegular />} onClick={onClose}></Button>
      </div>
    </div>
    <div style={{ position: "fixed", background: "#0002", width: "100vw", height: "100vh", top: 0, left: 0, zIndex: -1 }} onClick={onClose}></div>
  </>
}
