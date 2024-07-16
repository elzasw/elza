import { FC } from "react";
import {
    ArrowUp16Filled
  } from "@fluentui/react-icons";
import { Breadcrumb, BreadcrumbButton, BreadcrumbDivider, BreadcrumbItem, Tooltip } from "@fluentui/react-components";
import { isDaoFileFolderVO, useExplorerContext } from "./ExplorerContext";
import { turncate } from "./utils";


const ExplorerNavigationTab: FC = () => {
    const {selectedItem, setSelectedItem} = useExplorerContext();
    let parents = [];
    let curr = selectedItem;

    while(curr != null) {
        if(isDaoFileFolderVO(curr)) { //Nechceme v cestě zobrazovat file
            parents.push(curr);
        }
        curr = curr.parent;
    }

   parents = parents.reverse();

    const handleMoveUp = () => {
        if (parents.length - 1) {
            setSelectedItem(parents[parents.length - 2]);
        }
    }

    return (
        <Breadcrumb size="medium" style={{marginBottom: "5px"}}>
            <BreadcrumbButton as="button" onClick={handleMoveUp} icon={<ArrowUp16Filled color="black"/>}/>
            {parents.map((parent, index) => 
                <Tooltip
                    content={parent.label}
                    relationship="label"
                >
                    <BreadcrumbItem>
                        <BreadcrumbButton as="button" onClick={() => setSelectedItem(parent)}>{turncate(parent.label)}</BreadcrumbButton>
                        {index != parents.length - 1 && <BreadcrumbDivider />}
                    </BreadcrumbItem>
                </Tooltip>
            )}
        </Breadcrumb>
    );
}
export default ExplorerNavigationTab;

