import { ArrowUp16Filled } from "@fluentui/react-icons";
import {
    Breadcrumb,
    BreadcrumbButton,
    BreadcrumbDivider,
    BreadcrumbItem,
    Button,
    Menu,
    MenuItem,
    MenuList,
    MenuPopover,
    MenuTrigger,
    PartitionBreadcrumbItems,
    Tooltip,
    isTruncatableBreadcrumbContent,
    makeStyles,
    partitionBreadcrumbItems,
    truncateBreadcrumbLongName,
    useOverflowMenu
} from "@fluentui/react-components";
import { isDaoFileFolderVO, useExplorerContext } from "./ExplorerContext";
import {
    MoreHorizontalRegular,
    MoreHorizontalFilled,
    bundleIcon,
  } from "@fluentui/react-icons";
import React from "react";
import { generateUUID } from "utils/uuid";
import { DaoFileFolderVO } from "api/DaoFileFolderVO";
import { useIntl } from "react-intl";
import { explorerMessages } from "../messages";
import { levelIcon, useNodeName } from "./levels";

type Item = {
    key: number;
    item: DaoFileFolderVO
};

const ExplorerNavigationTab = () => {
    const {selectedItem, setSelectedItem, hideRoot} = useExplorerContext();
    const nodeName = useNodeName();

    let chain: Item[] = [];
    let curr = selectedItem;
    let index = 0;

    while(curr != null) {
        if(!curr.filename) {
          chain.push({key: index, item: {...curr}});
          index = index + 1;
        }
        curr = curr.parent;
    }

    chain = chain.reverse();

    // With the root hidden it is still the level above the sections, so moving up walks the
    // whole chain; only the crumbs leave it out.
    const items = hideRoot ? chain.slice(1) : chain;
    const parent = chain.length > 1 ? chain[chain.length - 2].item : null;

    const handleMoveUp = () => {
        if (parent) {
            setSelectedItem(parent);
        }
    }

    const {
        startDisplayedItems,
        overflowItems,
        endDisplayedItems,
      }: PartitionBreadcrumbItems<Item> = partitionBreadcrumbItems({
        items,
        maxDisplayedItems: 5,
      });

      const renderBreadcrumbItem = (item: Item, isLastItem: boolean = false) => {
        const name = nodeName(item.item);
        const icon = levelIcon(item.item.levelType);
        return (
            <React.Fragment key={generateUUID()}>
                {isTruncatableBreadcrumbContent(name, 20) ? (
                <Tooltip
                    key={generateUUID()}
                    content={name}
                    relationship="label"
                >
                        <BreadcrumbItem>
                            <BreadcrumbButton as="button" icon={icon} onClick={() => setSelectedItem(item.item)}>
                                {truncateBreadcrumbLongName(name, 20)}
                            </BreadcrumbButton>
                            {!isLastItem && <BreadcrumbDivider />}
                        </BreadcrumbItem>
                    </Tooltip>
                ) : (
                    <BreadcrumbItem>
                        <BreadcrumbButton as="button" icon={icon} onClick={() => setSelectedItem(item.item)}>{name}</BreadcrumbButton>
                        {!isLastItem && <BreadcrumbDivider />}
                    </BreadcrumbItem>
                )}
            </React.Fragment>
        );
      }

    return (
        <Breadcrumb size="medium">
            <BreadcrumbButton as="button" onClick={handleMoveUp} disabled={!parent}
                              icon={<ArrowUp16Filled color="black"/>}/>
            {startDisplayedItems.map((item) =>
                renderBreadcrumbItem(item, false)
            )}
            {overflowItems && overflowItems.length > 0 &&
                <OverflowMenu
                    overflowItems={overflowItems}
                    startDisplayedItems={startDisplayedItems}
                    endDisplayedItems={endDisplayedItems}
                    setSelectedItem={setSelectedItem}
                />
            }
            {endDisplayedItems &&
                endDisplayedItems.map((item) => {
                const isLastItem = item.key === 0;
                return renderBreadcrumbItem(item, isLastItem);
            })}
        </Breadcrumb>
    );
}
export default ExplorerNavigationTab;

const MoreHorizontal = bundleIcon(MoreHorizontalFilled, MoreHorizontalRegular);

const useTooltipStyles = makeStyles({
    tooltip: {
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis",
    },
});

type OverflowMenuProps = {
    setSelectedItem: (item: Item["item"]) => void;
} & PartitionBreadcrumbItems<Item>

const OverflowMenu = (props: OverflowMenuProps) => {
    const intl = useIntl();
    const nodeName = useNodeName();
    const { overflowItems, setSelectedItem } = props;
    const { ref, isOverflowing, overflowCount } =
      useOverflowMenu<HTMLButtonElement>();

    const tooltipStyles = useTooltipStyles();

    if (!isOverflowing && overflowItems && overflowItems.length === 0) {
      return null;
    }

    const getTooltipContent = (breadcrumbItems: readonly Item[] | undefined) => {
        if (!breadcrumbItems) {
          return "";
        }
        return breadcrumbItems.reduce((acc, initialValue, idx, arr) => {
          return (
            <>
              {acc}
              {arr[0].item !== initialValue.item && " > "}
              {nodeName(initialValue.item)}
            </>
          );
        }, <React.Fragment />);
      };

    const overflowItemsCount = overflowItems
      ? overflowItems.length + overflowCount
      : overflowCount;
    const tooltipContent =
      overflowItemsCount > 5
        ? `${overflowItemsCount} items`
        : {
            children: getTooltipContent(overflowItems),
            className: tooltipStyles.tooltip,
          };

    return (
      <BreadcrumbItem>
        <Menu hasIcons>
          <MenuTrigger disableButtonEnhancement>
            <Tooltip withArrow content={tooltipContent} relationship="label">
              <Button
                id="menu"
                appearance="subtle"
                ref={ref}
                icon={<MoreHorizontal />}
                aria-label={intl.formatMessage(explorerMessages.moreFolders, { count: overflowItemsCount })}
                role="button"
              />
            </Tooltip>
          </MenuTrigger>
          <MenuPopover>
            <MenuList>
              {overflowItems && overflowItems.length > 0 &&
                overflowItems.map((item) => (
                    <MenuItem
                        icon={levelIcon(item.item.levelType) ?? null}
                        key={generateUUID()}
                        onClick={() => setSelectedItem(item.item)}
                    >
                        {nodeName(item.item)}
                    </MenuItem>
                ))}
            </MenuList>
          </MenuPopover>
        </Menu>
        <BreadcrumbDivider />
      </BreadcrumbItem>
    );
};

