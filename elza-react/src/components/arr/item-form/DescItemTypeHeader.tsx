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
import {
  CheckmarkRegular,
  ClipboardArrowRightFilled,
  ClipboardPasteRegular,
  CopyArrowRightRegular,
  CopyRegular,
  MoreHorizontal20Filled,
  Table20Regular,
} from "@fluentui/react-icons";
import { FormItemType } from "elza-api";
import { MouseEvent, PropsWithChildren, ReactNode } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { unitdateHelpValues } from "components/shared/unitdate/helpValues";
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
  // Copies the type's own values for pasting into another node; the button is hidden without it.
  // `append` (Ctrl/Cmd+click) adds them to the values copied before.
  handleCopyValues?: (id: number, append: boolean) => void;
  canCopyValues?: boolean;
  // Pastes copied values into this type; the button is shown only when the clipboard holds some.
  handlePasteValues?: (id: number) => void;
  canPasteValues?: boolean;
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
  handleCopyValues,
  canCopyValues = false,
  handlePasteValues,
  canPasteValues = false,
  getOpenInDataGridHref,
  onOpenInDataGrid,
  canCopyFromPrev,
  hideCopyButtons = false,
  extraActions,
}: Props) {
  const styles = useStyles();
  const widthClasses = [
    styles.gridItem_0,
    styles.gridItem_1,
    styles.gridItem_2,
    styles.gridItem_3,
    styles.gridItem_4,
  ];
  const isCopied = nodeSettings?.descItemTypeCopyIds.includes(typeRef.id);  const { settings } = useUserSettings();
  const compact = settings.compact;

  const intl = useIntl();
  const dataType = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap[typeRef.dataTypeId]);
  const formatDescriptor = dataType ? dataTypeFormatMessages[dataType.code] : undefined;
  // the unit-date hint takes its examples from the UI language; other hints ignore the extra values
  const formatValues = formatDescriptor ? { ...richTextValues, ...unitdateHelpValues(intl) } : richTextValues;
  const tooltipContent = typeRef.description || formatDescriptor ? (
    <>
      {typeRef.description && <div>{typeRef.description}</div>}
          {formatDescriptor && <div style={{ marginTop: "8px" }}>
              <FormattedMessage {...formatDescriptor} values={formatValues} />
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
        widthClasses[typeWidth],
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
            <Tooltip relationship="label" content={<FormattedMessage {...messages.copyFromPrev} />}>
              <Button
                size="small"
                appearance="subtle"
                icon={<ClipboardPasteRegular />}
                onClick={() => handleCopyFromPrev(typeRef.id)}
                disabled={!canCopyFromPrev}
                tabIndex={-1}
              />
            </Tooltip>
            {handleCopyValues && (
              <Tooltip relationship="label" content={<FormattedMessage {...messages.copyValues} />}>
                <Button
                  size="small"
                  appearance="subtle"
                  icon={<CopyArrowRightRegular />}
                  onClick={(event: MouseEvent<HTMLButtonElement>) =>
                    handleCopyValues(typeRef.id, event.ctrlKey || event.metaKey)
                  }
                  disabled={!canCopyValues}
                  tabIndex={-1}
                />
              </Tooltip>
            )}
            {handlePasteValues && canPasteValues && (
              <Tooltip relationship="label" content={<FormattedMessage {...messages.pasteValues} />}>
                <Button
                  size="small"
                  appearance="subtle"
                  icon={<ClipboardArrowRightFilled />}
                  onClick={() => handlePasteValues(typeRef.id)}
                  tabIndex={-1}
                />
              </Tooltip>
            )}
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
