import {
  Popover,
  PopoverProps,
  PopoverSurface,
  PopoverTrigger,
  makeStyles,
  tokens,
} from "@fluentui/react-components";
import { StructureView } from "components/arr/structure/StructureView";
import { DataStructureRef, DataType } from "elza-api";
import { useEffect, useRef, useState } from "react";
import { useActiveFund } from "utils/hooks";
import { DescItemProps } from "./types";
import { FormattedMessage } from "react-intl";
import { messages as commonMessages } from "components/arr/item-form/desc-items/commonMessages";

const HOVER_OPEN_DELAY = 600;

const useStyles = makeStyles({
  surface: {
    maxWidth: "480px",
    paddingBlock: tokens.spacingVerticalXS,
    paddingInline: tokens.spacingHorizontalXS,
  },
  complement: {
    marginInlineStart: tokens.spacingHorizontalXS,
    opacity: 0.6,
  },
});

export function DescItemStructured({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Structured) {
    throw "Incorrect data type";
  }

  const { id: fundId, versionId: fundVersionId } = useActiveFund();
  const styles = useStyles();
  const [isOpen, setIsOpen] = useState(false);
  const openTimeout = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => () => clearTimeout(openTimeout.current), []);

  const isInherited = item.nodeId !== nodeId;
  const data = item.data as DataStructureRef;

  // Popover opens as soon as the pointer enters; delay it so passing over a value does not pop
  // the whole structure up. Closing keeps Fluent's own mouseLeaveDelay.
  const handleOpenChange: PopoverProps["onOpenChange"] = (_event, { open }) => {
    clearTimeout(openTimeout.current);
    if (open) {
      openTimeout.current = setTimeout(() => setIsOpen(true), HOVER_OPEN_DELAY);
    } else {
      setIsOpen(false);
    }
  };

  const value = (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? (
        <FormattedMessage {...commonMessages.undefined} />
      ) : (
        <>
          {data.value}
          {data.complement && (
            <span className={styles.complement}>{data.complement}</span>
          )}
        </>
      )}
    </div>
  );

  const hasStructuredObject = data.structuredObjectId != undefined;
  if (item.undefined || !hasStructuredObject) {
    return value;
  }

  return (
    <Popover
      open={isOpen}
      onOpenChange={handleOpenChange}
      openOnHover
      mouseLeaveDelay={200}
      withArrow
      positioning="above"
    >
      <PopoverTrigger disableButtonEnhancement>{value}</PopoverTrigger>
      <PopoverSurface className={styles.surface}>
        <StructureView
          fundId={fundId}
          fundVersionId={fundVersionId}
          structureObjectId={data.structuredObjectId}
          plain
        />
      </PopoverSurface>
    </Popover>
  );
}
