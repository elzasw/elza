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
import { useStyles } from "../item-form/styles";

export interface ToolbarButtonDef {
  id: string;
  icon?: JSX.Element;
  label?: string;
  showLabel?: boolean;
  appearance?: "primary" | "subtle";
  action: () => void;
  isVisible?: boolean;
  overflowOnly?: boolean;
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
          aria-label="More items"
          appearance="subtle"
        />
      </MenuTrigger>

      <MenuPopover>
        <MenuList>
          {items.map(({ groupId, items }, index, arr) => {
            const isLast = index === arr.length - 1;
            return (
              <Fragment key={groupId}>
                {items.map(({ label, action, id, icon, overflowOnly }) => (
                  <ToolbarOverflowMenuItem
                    key={id}
                    id={id}
                    label={label}
                    action={action}
                    icon={icon}
                    overflowOnly={overflowOnly}
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
    showDivider?: boolean;
} & ToolbarButtonProps;

export const ToolbarOverflowButton = ({
  overflowId,
  overflowGroupId,
  tooltip,
  showDivider,
  ...props
}: ToolbarOverflowMenuProps) => {
  const styles = useStyles();
  let button = (
      <ToolbarButton className={styles.toolbarOverflowButton} {...props} />
  );

  if (tooltip) {
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
