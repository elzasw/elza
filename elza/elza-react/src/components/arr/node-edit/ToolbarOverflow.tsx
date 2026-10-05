import { Fragment, ReactElement, ReactNode, useEffect, useRef, useState } from "react";
import { MoreHorizontal20Filled } from "@fluentui/react-icons";
import {
  ToolbarButton,
  ToolbarDivider,
  Button,
  Menu,
  MenuDivider,
  MenuItem,
  MenuList,
  MenuPopover,
  MenuTrigger,
  OverflowItem,
  Popover,
  PopoverSurface,
  PopoverTrigger,
  makeStyles,
  mergeClasses,
  tokens,
  useOverflowMenu,
  useIsOverflowItemVisible,
  useIsOverflowGroupVisible,
  Tooltip,
} from "@fluentui/react-components";
import type {
  ToolbarButtonProps,
  MenuItemProps,
  PopoverProps,
} from "@fluentui/react-components";
import { useStyles } from "../item-form/styles";
import { useIntl } from 'react-intl';
import { toolbarMessages } from './toolbarMessages';

export interface ToolbarButtonDef {
  id: string;
  icon?: JSX.Element;
  label?: string;
  showLabel?: boolean;
  appearance?: "primary" | "subtle";
  action: () => void;
  isVisible?: boolean;
  disabled?: boolean;
  overflowOnly?: boolean;
  /** Number shown next to the icon (and in the overflow menu item); hidden when 0 or undefined. */
  counter?: number;
  /** Hover content replacing the label tooltip; it is interactive, unlike a tooltip. */
  popover?: ReactElement;
}

export interface ToolbarButtonGroupDef {
  groupId: string;
  items: ToolbarButtonDef[];
}

interface ToolbarOverflowMenuItemProps extends Omit<MenuItemProps, "id"> {
  id: string;
  action?: () => void;
  icon?: JSX.Element;
  label?: string;
  overflowOnly?: boolean;
}

const ToolbarOverflowMenuItem = ({
  id,
  label,
  icon,
  action,
  overflowOnly,
  ...rest
}: ToolbarOverflowMenuItemProps) => {
  const isVisible = useIsOverflowItemVisible(id);

  if (isVisible && !overflowOnly) {
    return null;
  }

  return (
    <MenuItem onClick={action} icon={icon} {...(rest as MenuItemProps)}>
      {label}
    </MenuItem>
  );
};

const ToolbarMenuOverflowDivider: React.FC<{
  id: string;
}> = (props) => {
  const isGroupVisible = useIsOverflowGroupVisible(props.id);

  if (isGroupVisible === "visible") {
    return null;
  }

  return <MenuDivider />;
};

interface OverflowMenuProps {
  items: ToolbarButtonGroupDef[];
}

export const OverflowMenu = ({ items }: OverflowMenuProps) => {
  const intl = useIntl();
  const { ref, isOverflowing } = useOverflowMenu<HTMLButtonElement>();

  const hasOverflowOnlyItems = items.some(({ items }) =>
    items.some(({ overflowOnly }) => overflowOnly),
  );

  if (!isOverflowing && !hasOverflowOnlyItems) {
    return null;
  }

  return (
    <Menu>
      <MenuTrigger disableButtonEnhancement>
        <Button
          ref={ref}
          icon={<MoreHorizontal20Filled />}
          aria-label={intl.formatMessage(toolbarMessages.moreItems)}
          appearance="subtle"
        />
      </MenuTrigger>

      <MenuPopover>
        <MenuList>
          {items.map(({ groupId, items }, index, arr) => {
            const isLast = index === arr.length - 1;
            return (
              <Fragment key={groupId}>
                {items.map(({ label, action, id, icon, overflowOnly, disabled, counter }) => (
                  <ToolbarOverflowMenuItem
                    key={id}
                    id={id}
                    label={label}
                    action={action}
                    icon={icon}
                    overflowOnly={overflowOnly}
                    disabled={disabled}
                    secondaryContent={counter || undefined}
                  />
                ))}
                {!isLast && <ToolbarMenuOverflowDivider id={groupId} />}
              </Fragment>
            );
          })}
        </MenuList>
      </MenuPopover>
    </Menu>
  );
};

type ToolbarOverflowDividerProps = {
  groupId: string;
};

export const ToolbarOverflowDivider = ({
  groupId,
}: ToolbarOverflowDividerProps) => {
  const groupVisibleState = useIsOverflowGroupVisible(groupId);

  if (groupVisibleState !== "hidden") {
    return <ToolbarDivider />;
  }

  return null;
};

const HOVER_OPEN_DELAY = 400;

const useHoverStyles = makeStyles({
  surface: {
    maxWidth: "480px",
    maxHeight: "60vh",
    overflowY: "auto",
    paddingBlock: tokens.spacingVerticalS,
    paddingInline: tokens.spacingHorizontalM,
  },
});

/** Popover opened by hovering, after a delay; a click on the trigger closes it. */
function HoverPopover({ trigger, children }: { trigger: ReactElement; children: ReactNode }) {
  const styles = useHoverStyles();
  const [isOpen, setIsOpen] = useState(false);
  const openTimeout = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => () => clearTimeout(openTimeout.current), []);

  // Fluent's Popover opens as soon as the pointer enters and toggles on click; delay the hover
  // opening and let a click (the button's own action) close it instead.
  const handleOpenChange: PopoverProps["onOpenChange"] = (event, { open }) => {
    clearTimeout(openTimeout.current);
    const isHoverOpen = open && event.type !== "click";
    if (isHoverOpen) {
      openTimeout.current = setTimeout(() => setIsOpen(true), HOVER_OPEN_DELAY);
    } else {
      setIsOpen(false);
    }
  };

  return (
    <Popover
      open={isOpen}
      onOpenChange={handleOpenChange}
      openOnHover
      mouseLeaveDelay={200}
      withArrow
      positioning="below"
    >
      <PopoverTrigger disableButtonEnhancement>{trigger}</PopoverTrigger>
      <PopoverSurface className={styles.surface}>{children}</PopoverSurface>
    </Popover>
  );
}

type ToolbarOverflowMenuProps = {
  overflowId: string;
  overflowGroupId: string;
  tooltip?: string | ReactElement;
  /** Replaces the tooltip with an interactive hover popover. */
  popover?: ReactElement;
    showDivider?: boolean;
} & ToolbarButtonProps;

export const ToolbarOverflowButton = ({
  overflowId,
  overflowGroupId,
  tooltip,
  popover,
  showDivider,
  ...props
}: ToolbarOverflowMenuProps) => {
  const styles = useStyles();
  const { className, ...buttonProps } = props;
  let button = (
      <ToolbarButton className={mergeClasses(styles.toolbarOverflowButton, className)} {...buttonProps} />
  );

  if (popover) {
    button = <HoverPopover trigger={button}>{popover}</HoverPopover>;
  } else if (tooltip) {
    button =  <Tooltip appearance="inverted" relationship="label" content={tooltip}>
        {button}
    </Tooltip>
  }

  return <OverflowItem id={overflowId} groupId={overflowGroupId}>
      <div>
          <div className={styles.toolbarOverflowInner}>
            {showDivider && <ToolbarDivider />}
            {button}
          </div>
      </div>
  </OverflowItem>
};
