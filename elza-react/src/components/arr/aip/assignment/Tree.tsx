import {
    CounterBadge,
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
      /** Na začátku rozbalit všechny úrovně. */
      openAll?: boolean;
  }
  
  const Tree = ({nodes, expandedIds, selectedNode, setSelectedNode, openAll}: FundTreeProps) => {
    const [openItems, setOpenItems] = useState<Set<TreeItemValue>>(
        () => openAll
            ? new Set(nodes.filter(n => nodes.some(m => m.parentValue === n.value)).map(n => n.value))
            : new Set()
    );

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
                const { content, count, ...treeItemProps } = flatTreeItem.getTreeItemProps() as
                    ReturnType<typeof flatTreeItem.getTreeItemProps> & { count?: number };
                
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
                            aside={count != null
                                ? <CounterBadge count={count} overflowCount={9999} appearance="ghost" color="informative" />
                                : undefined}
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