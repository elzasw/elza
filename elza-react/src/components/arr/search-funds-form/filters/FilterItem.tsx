import { Button, Menu, MenuButton, MenuItem, MenuPopover, MenuTrigger } from "@fluentui/react-components";
import { DismissRegular } from "@fluentui/react-icons";
import { OperationCompareType } from "elza-api";
import { PropsWithChildren } from "react";
import { useIntl } from "react-intl";
import { messages } from "./messages";

export interface FilterItemProps {
  operation?: OperationCompareType;
  availableOperations?: OperationCompareType[];
  onOperationChange?: (operation: OperationCompareType) => void;
  onRemove?: () => void;
  canRemove?: boolean;
}

export function FilterItem({
  operation,
  availableOperations = [],
  onOperationChange = () => { return; },
  onRemove,
  canRemove = true,
  children,
}: PropsWithChildren<FilterItemProps>) {
  const { formatMessage } = useIntl();

  return <div style={{ display: "flex", alignItems: "center", gap: "5px", marginBottom: "5px" }}>
    {availableOperations.length > 1 &&
      <Menu>
        <MenuTrigger>
          <MenuButton size="medium">
            {operation ? formatMessage(messages[operation]) : ""}
          </MenuButton>
        </MenuTrigger>
        <MenuPopover>
          {availableOperations.map((_operation) => {
            return <MenuItem
              key={_operation}
              onClick={() => onOperationChange(_operation)}
            >{formatMessage(messages[_operation])}</MenuItem>
          })}
        </MenuPopover>
      </Menu>
    }
    <div style={{ flex: 1 }}>
      {children}
    </div>
    {onRemove && canRemove &&
      <Button
        appearance="subtle"
        size="small"
        icon={<DismissRegular />}
        onClick={onRemove}
      />
    }
  </div>
}
