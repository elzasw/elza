import { Tree, TreeItem, TreeItemLayout, TreeItemValue, TreeOpenChangeData, TreeOpenChangeEvent } from "@fluentui/react-components";
import { AREA_AIP } from "actions/aip/aip";
import { FC, useState } from "react";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {
    AddSquare16Regular,
    SubtractSquare16Regular,
  } from "@fluentui/react-icons";

const AipTree: FC = () => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const [openItems, setOpenItems] = useState<TreeItemValue[]>([]);

    const handleOpenChange = (
      event: TreeOpenChangeEvent,
      data: TreeOpenChangeData
    ) => {
      setOpenItems((curr) =>
        data.open
          ? [...curr, data.value]
          : curr.filter((value) => value !== data.value)
      );
    };


    return(
        <Tree
            aria-label="Průzkumník"
            openItems={openItems}
            onOpenChange={handleOpenChange}
        >
            <TreeItem itemType="branch" value="tree-item-2">
                <TreeItemLayout
                    expandIcon={
                        openItems.includes("tree-item-2") ? (
                        <SubtractSquare16Regular />
                        ) : (
                        <AddSquare16Regular />
                        )
                    }
                >
                    level 1, item 1
                </TreeItemLayout>
                <Tree>
                    <TreeItem itemType="branch" value="tree-item-3">
                        <TreeItemLayout
                            expandIcon={
                                openItems.includes("tree-item-3") ? (
                                <SubtractSquare16Regular />
                                ) : (
                                <AddSquare16Regular />
                                )
                            }
                        >
                            level 2, item 1
                        </TreeItemLayout>
                        <Tree>
                            <TreeItem itemType="leaf">
                                <TreeItemLayout>level 3, item 1</TreeItemLayout>
                            </TreeItem>
                        </Tree>
                    </TreeItem>
                </Tree>
            </TreeItem>
        </Tree>
    );
}

export default AipTree;

