import {
  Menu,
  MenuDivider,
  MenuItem,
  MenuItemLink,
  MenuList,
  MenuPopover,
  MenuTrigger,
  Button,
  Tooltip,
  mergeClasses,
  tokens,
} from "@fluentui/react-components";
import { CheckmarkRegular, ClipboardPasteRegular, CopyRegular, MoreHorizontal20Filled, Table20Regular } from "@fluentui/react-icons";
import { FormItemType } from "elza-api";
import { PropsWithChildren, ReactNode } from "react";
import { FormattedMessage } from "react-intl";
import { DescItemTypeRef, NodeSettings } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useUserSettings } from "contexts/user";
import { dataTypeFormatMessages, messages } from "./messages";
import { DescItemTypeDebugInfo } from "./NodeDebugInfo";
import { useStyles } from "./styles";

const richTextValues = {
  b: (chunks: ReactNode) => <b>{chunks}</b>,
  i: (chunks: ReactNode) => <i>{chunks}</i>,
  p: (chunks: ReactNode) => <p style={{ margin: "2px 0" }}>{chunks}</p>,
};

export interface Props extends PropsWithChildren {
  typeRef: DescItemTypeRef;
  typeForm?: FormItemType;
  typeWidth: number;
  nodeSettings: NodeSettings;
  handleCopyFromPrev: (id: number) => void;
  canCopyFromPrev: boolean;
  handleCopyToggle: (id: number) => void;
  // Href for the "open in datagrid" menu item, so it behaves as a real link (ctrl/middle-click,
  // open in new tab). onOpenInDataGrid handles the in-app (SPA) navigation on a plain click.
  getOpenInDataGridHref?: (id: number) => string;
  onOpenInDataGrid?: (id: number) => void;
  hideCopyButtons?: boolean;
  extraActions?: ReactNode;
}

export function DescItemTypeHeader({
  children,
  typeRef,
  typeForm,
  typeWidth,
  nodeSettings,
  handleCopyFromPrev,
  handleCopyToggle,
  getOpenInDataGridHref,
  onOpenInDataGrid,
  canCopyFromPrev,
  hideCopyButtons = false,
  extraActions,
}: Props) {
  const styles = useStyles();
  const isCopied = nodeSettings?.descItemTypeCopyIds.includes(typeRef.id);
  const { settings } = useUserSettings();
  const compact = settings.compact;

  const dataType = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap[typeRef.dataTypeId]);
  const formatDescriptor = dataType ? dataTypeFormatMessages[dataType.code] : undefined;
  const tooltipContent = typeRef.description || formatDescriptor ? (
    <>
      {typeRef.description && <div>{typeRef.description}</div>}
          {formatDescriptor && <div style={{ marginTop: "8px" }}>
              <FormattedMessage {...formatDescriptor} values={richTextValues} />
          </div>}
    </>
  ) : undefined;

  return (
    <div
      key={typeRef.id}
      style={{
        outlineColor: "transparent",
        outlineOffset: "4px",
        borderRadius: "1px",
        transition: "outline-color 300ms ease-out",
      }}
      className={mergeClasses(
        compact ? styles.gridItemCompact : styles.gridItem,
        styles[`gridItem_${typeWidth}`],
        styles.descItemTypeTitle,
      )}
      onMouseEnter={({ currentTarget }) => {
        currentTarget.style.outline = "none";
      }}
    >
      <div
        style={{
          flexShrink: 1,
          fontWeight: "bold",
          marginRight: "4px",
          display: "flex",
          alignItems: "center",
          // opacity: typeWidth ? 1 - (4 - typeWidth) / 6 : 1,
          // fontSize: `${1 + (typeWidth ? typeWidth * 0.1 : 0.4)}em`,
          // fontSize: '0.8em',
          fontSize: compact ? '0.95em' : undefined,
          lineHeight: '1.3em',
          // opacity: 0.6,
          // textTransform: 'uppercase',
        }}
      >
        <Tooltip
          relationship="label"
          appearance="inverted"
          content={tooltipContent}
        >
          <div>{typeRef.shortcut}</div>
        </Tooltip>
        <DescItemTypeDebugInfo typeRef={typeRef} typeForm={typeForm} />
        {extraActions && (
          <span style={{ marginLeft: tokens.spacingHorizontalXS }}>{extraActions}</span>
        )}
        {!hideCopyButtons && (
          <div className="actions" style={{ marginLeft: tokens.spacingHorizontalXS }}>
            <Menu>
              <MenuTrigger disableButtonEnhancement>
                <Button
                  size="small"
                  appearance="subtle"
                  icon={<MoreHorizontal20Filled />}
                  tabIndex={-1}
                />
              </MenuTrigger>
              <MenuPopover>
                <MenuList>
                  <MenuItem
                    icon={<ClipboardPasteRegular />}
                    onClick={() => handleCopyFromPrev(typeRef.id)}
                    disabled={!canCopyFromPrev}
                  >
                    <FormattedMessage {...messages.copyFromPrev} />
                  </MenuItem>
                  <MenuItem
                    icon={isCopied ? <CheckmarkRegular /> : <CopyRegular />}
                    onClick={() => handleCopyToggle(typeRef.id)}
                  >
                    <FormattedMessage {...messages.copyToggle} />
                  </MenuItem>
                  {onOpenInDataGrid && getOpenInDataGridHref && (
                    <>
                      <MenuDivider />
                      <MenuItemLink
                        href={getOpenInDataGridHref(typeRef.id)}
                        icon={<Table20Regular />}
                        onClick={event => {
                          // Let ctrl/cmd/shift/middle-click fall through to the browser (open in new tab);
                          // handle a plain click as in-app navigation.
                          const opensNewTab = event.ctrlKey || event.metaKey || event.shiftKey || event.button === 1;
                          if (opensNewTab) {
                            return;
                          }
                          event.preventDefault();
                          onOpenInDataGrid(typeRef.id);
                        }}
                      >
                        <FormattedMessage {...messages.openInDataGrid} />
                      </MenuItemLink>
                    </>
                  )}
                </MenuList>
              </MenuPopover>
            </Menu>
            {isCopied && (
              <Tooltip relationship="label" content={<FormattedMessage {...messages.copyToggle} />}>
                <Button
                  size="small"
                  appearance="primary"
                  icon={<CopyRegular />}
                  onClick={() => handleCopyToggle(typeRef.id)}
                  tabIndex={-1}
                />
              </Tooltip>
            )}
          </div>
        )}
      </div>
      <>{children}</>
    </div>
  );
}
