import { Button, Tooltip, mergeClasses } from "@fluentui/react-components";
import { CopyAddRegular, CopyRegular } from "@fluentui/react-icons";
import { FormItemType } from "elza-api";
import { PropsWithChildren, useState } from "react";
import { FormattedMessage } from "react-intl";
import { DescItemTypeRef, NodeSettings } from "typings/store";
import { messages } from "./messages";
import { useStyles } from "./styles";

export interface Props extends PropsWithChildren {
  typeRef: DescItemTypeRef;
  typeForm: FormItemType;
  typeWidth: number;
  nodeSettings: NodeSettings;
  handleCopyFromPrev: (id: number) => void;
  canCopyFromPrev: boolean;
  handleCopyToggle: (id: number) => void;
}

export function FormItemTypeComp({
  children,
  typeRef,
  typeWidth,
  nodeSettings,
  handleCopyFromPrev,
  handleCopyToggle,
  canCopyFromPrev,
}: Props) {
  const styles = useStyles();
  const [isHovered, setIsHovered] = useState(false);
  const isCopied = nodeSettings?.descItemTypeCopyIds.includes(typeRef.id);

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
        styles.gridItem,
        styles[`gridItem_${typeWidth}`],
        styles.descItemTypeTitle,
      )}
      onMouseEnter={({ currentTarget }) => {
        currentTarget.style.outline = "none";
        setIsHovered(true);
      }}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div
        style={{
          flexShrink: 1,
          fontWeight: "bold",
          marginRight: "4px",
          display: "flex",
          alignItems: "flex-end",
          // opacity: typeWidth ? 1 - (4 - typeWidth) / 6 : 1,
          // fontSize: `${1 + (typeWidth ? typeWidth * 0.1 : 0.4)}em`,
          fontSize: '0.8em',
          lineHeight: '1.2em',
          // opacity: 0.6,
          // textTransform: 'uppercase',
        }}
      >
        <Tooltip
          relationship="label"
          appearance="inverted"
          content={typeRef.description}
        >
          <div>{typeRef.shortcut}</div>
        </Tooltip>
        {(
          <div className="actions" >
            <Tooltip
              relationship="label"
              appearance="inverted"
              content={<FormattedMessage {...messages.copyFromPrev} />}
            >
              <Button
                // className="hidable-button"
                style={{visibility: isHovered ? "visible" : "hidden"}}
                size="small"
                appearance="subtle"
                icon={<CopyAddRegular />}
                onClick={() => handleCopyFromPrev(typeRef.id)}
                disabled={canCopyFromPrev}
                tabIndex={-1}
              />
            </Tooltip>
            <Tooltip
              relationship="label"
              appearance="inverted"
              content={<FormattedMessage {...messages.copyToggle} />}
            >
              <Button
              style={{visibility: isHovered || isCopied ? "visible" : "hidden"}}
                className={
                  nodeSettings?.descItemTypeCopyIds.includes(typeRef.id)
                    ? undefined
                    : "hidable-button"
                }
                size="small"
                appearance={
                  nodeSettings?.descItemTypeCopyIds.includes(typeRef.id)
                    ? "primary"
                    : "subtle"
                }
                icon={<CopyRegular />}
                onClick={() => handleCopyToggle(typeRef.id)}
                tabIndex={-1}
              />
            </Tooltip>
          </div>
        )}
      </div>
      <div>{children}</div>
    </div>
  );
}
