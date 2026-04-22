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
    TreeItemValue, 
    isTruncatableBreadcrumbContent, 
    makeStyles, 
    partitionBreadcrumbItems, 
    truncateBreadcrumbLongName,
    useOverflowMenu 
} from "@fluentui/react-components";
import {
    MoreHorizontalRegular,
    MoreHorizontalFilled,
    bundleIcon,
  } from "@fluentui/react-icons";
import React from "react";
import { FlatItem } from "./AipsLogicalContainer";
import { findNodeByValue } from "./utils";

type TreeNavigationProps = {
    selectedNode: TreeItemValue;
    nodes: any;
    onSelect: (node: TreeItemValue) => void;
}

const TreeNavigation = ({onSelect, selectedNode, nodes}: TreeNavigationProps) => {
    let items = [];
    let parent = findNodeByValue(nodes, selectedNode);
    while(parent != undefined) {
        items.push(parent);
        parent = findNodeByValue(nodes, parent.parentValue)
    }

    items = items.reverse();
    
    const {
        startDisplayedItems,
        overflowItems,
        endDisplayedItems,
      }: PartitionBreadcrumbItems<FlatItem> = partitionBreadcrumbItems({
        items,
        maxDisplayedItems: 5
      });

      const handleMoveUp = () => {
        const parent = findNodeByValue(nodes, findNodeByValue(nodes, selectedNode) ?.parentValue);
        if(parent) {
            onSelect(parent.value);
        }
      }

      const renderBreadcrumbItem = (item: FlatItem, isLastItem: boolean = false) => {
        return (
            <React.Fragment key={item.value}>
                {isTruncatableBreadcrumbContent(item.content, 20) ? (
                    <Tooltip
                        key={`bread-${item.value}`}
                        content={item.content}
                        relationship="label"
                    >
                        <BreadcrumbItem>
                            <BreadcrumbButton as="button" onClick={() => onSelect(item.value)}>
                                {truncateBreadcrumbLongName(item.content, 20)}
                            </BreadcrumbButton>
                            {!isLastItem && <BreadcrumbDivider />}
                        </BreadcrumbItem>
                    </Tooltip>
                ) : (
                    <BreadcrumbItem>
                        <BreadcrumbButton as="button" onClick={() => onSelect(item.value)}>{item.content}</BreadcrumbButton>
                        {!isLastItem && <BreadcrumbDivider />}
                    </BreadcrumbItem>
                )}
            </React.Fragment>
        );
      }

    return (
        <Breadcrumb 
          size="small"
          style={{overflowX: "auto"}}
          >
            <BreadcrumbButton 
              as="button" 
              onClick={handleMoveUp} 
              icon={<ArrowUp16Filled color="black"/>}
            />
            {startDisplayedItems.map((item) => {
             const isLastItem = item.value === selectedNode;
                return renderBreadcrumbItem(item, isLastItem)
            })}
            {overflowItems && overflowItems.length > 0 && 
                <OverflowMenu
                    overflowItems={overflowItems}
                    startDisplayedItems={startDisplayedItems}
                    endDisplayedItems={endDisplayedItems}
                    setSelectedItem={onSelect}
                />
            }
            {endDisplayedItems &&
                endDisplayedItems.map((item) => {
                const isLastItem = item.value === selectedNode;
                return renderBreadcrumbItem(item, isLastItem);
            })}
        </Breadcrumb>
    );
}
export default TreeNavigation;

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
} & PartitionBreadcrumbItems<FlatItem>

const OverflowMenu = (props: OverflowMenuProps) => {
    const { overflowItems, setSelectedItem } = props;
    const { ref, isOverflowing, overflowCount } =
      useOverflowMenu<HTMLButtonElement>();
  
    const tooltipStyles = useTooltipStyles();
  
    if (!isOverflowing && overflowItems && overflowItems.length === 0) {
      return null;
    }

    const getTooltipContent = (breadcrumbItems: readonly FlatItem[] | undefined) => {
        if (!breadcrumbItems) {
          return "";
        }
        return breadcrumbItems.reduce((acc, initialValue, idx, arr) => {
          return (
            <>
              {acc}
              {arr[0].value !== initialValue.value && " > "}
              {initialValue.content}
            </>
          );
        }, <React.Fragment />);
      };
  
    const overflowItemsCount = overflowItems
      ? overflowItems.length + overflowCount
      : overflowCount;
    const tooltipContent =
      overflowItemsCount > 3
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
                        key={`over-${item.value}`}
                        onClick={() => setSelectedItem(item.value)}
                    >
                        {item.content}
                    </MenuItem>
                ))}
            </MenuList>
          </MenuPopover>
        </Menu>
        <BreadcrumbDivider />
      </BreadcrumbItem>
    );
};

