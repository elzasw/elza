import { Fragment, ReactElement, ReactNode } from "react";
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
  useOverflowMenu,
  useIsOverflowItemVisible,
  useIsOverflowGroupVisible,
  Tooltip,
} from "@fluentui/react-components";
import type {
  ToolbarButtonProps,
  MenuItemProps,
} from "@fluentui/react-components";

export interface ToolbarButtonDef {
  id: string;
  icon?: JSX.Element;
  label?: string;
  showLabel?: boolean;
  appearance?: "primary" | "subtle";
  action: () => void;
  isVisible?: boolean;
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
}

const ToolbarOverflowMenuItem = ({
  id,
  label,
  icon,
  action,
  ...rest
}: ToolbarOverflowMenuItemProps) => {
  const isVisible = useIsOverflowItemVisible(id);

  if (isVisible) {
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
  const { ref, isOverflowing } = useOverflowMenu<HTMLButtonElement>();

  if (!isOverflowing) {
    return null;
  }

  return (
    <Menu>
      <MenuTrigger disableButtonEnhancement>
        <Button
          ref={ref}
          icon={<MoreHorizontal20Filled />}
          aria-label="More items"
          appearance="subtle"
        />
      </MenuTrigger>

      <MenuPopover>
        <MenuList>
          {items.map(({ groupId, items }, index) => {
            const isLast = index === items.length - 1;
            return (
              <Fragment key={groupId}>
                {items.map(({ label, action, id, icon }) => (
                  <ToolbarOverflowMenuItem
                    key={id}
                    id={id}
                    label={label}
                    action={action}
                    icon={icon}
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

type ToolbarOverflowMenuProps = {
  overflowId: string;
  overflowGroupId: string;
  tooltip?: string | ReactElement;
} & ToolbarButtonProps;

export const ToolbarOverflowButton = ({
  overflowId,
  overflowGroupId,
  tooltip,
  ...props
}: ToolbarOverflowMenuProps) => {
  const button = (
    <OverflowItem id={overflowId} groupId={overflowGroupId}>
      <ToolbarButton style={{ flexShrink: 0 }} {...props} />
    </OverflowItem>
  );

  if (tooltip) {
    return (
      <Tooltip appearance="inverted" relationship="label" content={tooltip}>
        {button}
      </Tooltip>
    );
  }
  return button;
};
