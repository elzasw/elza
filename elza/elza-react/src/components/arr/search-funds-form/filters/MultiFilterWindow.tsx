import { Button } from "@fluentui/react-components";
import { AddRegular, CheckmarkRegular, DismissRegular } from "@fluentui/react-icons";
import { PropsWithChildren } from "react";
import { FormattedMessage } from "react-intl";
import { messages } from "./messages";

export type MultiFilterWindowProps = Props;

interface Props {
  filterName?: string;
  isValid?: boolean;
  isDirty?: boolean;
  onClose?: () => void;
  onFilterConfirm?: () => void;
  onAddItem?: () => void;
  canAddItem?: boolean;
}

export function MultiFilterWindow({
  filterName,
  isValid,
  isDirty,
  onClose = () => { return; },
  onFilterConfirm = () => { return; },
  onAddItem,
  canAddItem = true,
  children,
}: PropsWithChildren<Props>) {
  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Escape") {
      e.preventDefault();
      e.stopPropagation();
      onClose();
    }
    else if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      onFilterConfirm();
    }
  }

  return <>
    <div onKeyDown={onKeyDown} style={{
      padding: "10px 12px",
      background: "var(--shade-0)",
      borderRadius: "5px",
      boxShadow: "0 0 6px 0 #0003",
      zIndex: 10,
      minWidth: "300px",
    }}>
      <div>
        <label>{filterName}</label>
        <div style={{ display: "flex", flexDirection: "column", marginTop: "5px" }}>
          {children}
        </div>
        {onAddItem
          && canAddItem
          && <Button
            size="medium"
            icon={<AddRegular />}
            disabled={!canAddItem}
            onClick={onAddItem}
            style={{ borderStyle: "dashed", color: "#666" }}
          >
            <FormattedMessage {...messages.filter_add_value} />
          </Button>
        }
      </div>
      <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "10px" }}>
        <Button appearance="primary" disabled={!isValid || !isDirty} icon={<CheckmarkRegular />} onClick={onFilterConfirm}><FormattedMessage {...messages.filter_confirm} /></Button>
        <Button appearance="subtle" icon={<DismissRegular />} onClick={onClose}></Button>
      </div>
    </div>
    <div style={{ position: "fixed", background: "#0002", width: "100vw", height: "100vh", top: 0, left: 0, zIndex: -1 }} onClick={onClose}></div>
  </>
}
