import {
    FlatTree,
    TreeItemLayout,
    useHeadlessFlatTree_unstable,
    FlatTreeItem,
    TreeItemValue,
    TreeOpenChangeData,
    TreeOpenChangeEvent,
  } from "@fluentui/react-components";
import {
      AddSquare16Regular,
      SubtractSquare16Regular,
} from "@fluentui/react-icons";
import { useEffect, useState } from "react";
import "./Tree.scss";
  
  type FundTreeProps = {
      nodes: any;
      expandedIds?: Set<TreeItemValue>;
      selectedNode: TreeItemValue;
      setSelectedNode: (item: TreeItemValue) => void;
  }
  
  const Tree = ({nodes, expandedIds, selectedNode, setSelectedNode}: FundTreeProps) => {
    const [openItems, setOpenItems] = useState<Set<TreeItemValue>>(
        () => new Set()
    );

    console.log('nodes :>> ', nodes);
    const items = nodes;
       
    const handleOpenChange = (
        event: TreeOpenChangeEvent,
        data: TreeOpenChangeData
    ) => {
        setSelectedNode(data.value);
        if(selectedNode == data.value) {
            setOpenItems(data.openItems);
        }
    };
    
    useEffect(() => {
        if(expandedIds) {
            setOpenItems(new Set(Object.keys(expandedIds).map(Number)));
        }
    },[expandedIds]);


    const flatTree = useHeadlessFlatTree_unstable(items, {
        defaultOpenItems: openItems,
        onOpenChange: handleOpenChange,
        openItems: openItems
    });

    return (
        <FlatTree 
            {...flatTree.getTreeProps()}
            aria-label="Tree"
            className="tree"
        >
            {Array.from(flatTree.items(), (flatTreeItem) => {
                const { content, ...treeItemProps } = flatTreeItem.getTreeItemProps();
                
                return (
                    <FlatTreeItem 
                        {...treeItemProps} 
                        key={flatTreeItem.value}
                    >
                        <TreeItemLayout
                            expandIcon={
                                flatTreeItem.itemType == "branch" ? 
                                openItems.has(flatTreeItem.value) ? 
                                    <SubtractSquare16Regular color="black"/> : 
                                    <AddSquare16Regular color="black"/>
                                : undefined
                            }
                            className={selectedNode == flatTreeItem.value ? "selected-node" : undefined}
                        >
                            {content}
                        </TreeItemLayout>
                    </FlatTreeItem>
                );
            })}
        </FlatTree>
    );
  }
  
  export default Tree;