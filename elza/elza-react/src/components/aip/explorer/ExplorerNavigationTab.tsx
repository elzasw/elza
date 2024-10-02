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
import { generateUUID } from "../utils";
import { DaoFileFolderVO } from "api/DaoFileFolderVO";

type Item = {
    key: number;
    item: DaoFileFolderVO
};

const ExplorerNavigationTab = () => {
    const {selectedItem, setSelectedItem} = useExplorerContext();

    let items = [];
    let curr = selectedItem;
    let index = 0;

    while(curr != null) {
        if(!curr.fileName) {
          items.push({key: index, item: {...curr}});
          index = index + 1;
        }
        curr = curr.parent;
    }

    items = items.reverse();

    const handleMoveUp = () => {
        if (items.length - 1) {
            setSelectedItem(items[items.length - 2].item);
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

      const renderBreadcrumbItem = (item, isLastItem: boolean = false) => {
        return (
            <React.Fragment key={generateUUID()}>
                {isTruncatableBreadcrumbContent(item.item.label, 20) ? (
                <Tooltip
                    key={generateUUID()}
                    content={item.item.label}
                    relationship="label"
                >
                        <BreadcrumbItem>
                            <BreadcrumbButton as="button" onClick={() => setSelectedItem(item.item)}>
                                {truncateBreadcrumbLongName(item.item.label, 20)}
                            </BreadcrumbButton>
                            {!isLastItem && <BreadcrumbDivider />}
                        </BreadcrumbItem>
                    </Tooltip>
                ) : (
                    <BreadcrumbItem>
                        <BreadcrumbButton as="button" onClick={() => setSelectedItem(item.item)}>{item.item.label}</BreadcrumbButton>
                        {!isLastItem && <BreadcrumbDivider />}
                    </BreadcrumbItem>
                )}
            </React.Fragment>
        );
      }

    return (
        <Breadcrumb size="medium">
            <BreadcrumbButton as="button" onClick={handleMoveUp} icon={<ArrowUp16Filled color="black"/>}/>
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
    setSelectedItem: (item) => void;
} & PartitionBreadcrumbItems<Item>

const OverflowMenu = (props: OverflowMenuProps) => {
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
              {initialValue.item.label}
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
                aria-label={`${overflowItemsCount} dalších složek`}
                role="button"
              />
            </Tooltip>
          </MenuTrigger>
          <MenuPopover>
            <MenuList>
              {overflowItems && overflowItems.length > 0 &&
                overflowItems.map((item) => (
                    <MenuItem
                        icon={null}
                        key={generateUUID()}
                        onClick={() => setSelectedItem(item.item)}
                    >
                        {item.item.label}
                    </MenuItem>
                ))}
            </MenuList>
          </MenuPopover>
        </Menu>
        <BreadcrumbDivider />
      </BreadcrumbItem>
    );
};

