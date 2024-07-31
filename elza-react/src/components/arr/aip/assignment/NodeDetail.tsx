import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import * as aipActions from 'actions/aip/aip';
import AipDetailBody from "components/aip/AipDetailBody";
import { Button } from "components/ui";
import { Icon } from "components/shared";
import { findNodeInTree, getTreeItems } from "./utils";
import { TreeItemValue } from "@fluentui/react-components";

type NodeDetailProps = {
    tree: any;
    nodes: any;
    selectedNode: TreeItemValue;
}

const NodeDetail = ({tree, nodes, selectedNode}: NodeDetailProps) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP))
    const dispatch = useThunkDispatch();
    const node = tree.nodes.find(n => n.UUID == selectedNode);
    

    useEffect(() => {
        if(node && !node.hasChildren) {
            dispatch(aipActions.aipFetchIfNeeded(node.value));
        }
    }, [selectedNode]);

    const renderHeader = () => (
        <>
            <h4><b>Popis</b></h4>
            <p><b>Název </b>{node.name}</p>
        </>
    );
    if (node && node.value?.length > 1) {
        return (
            <div>
                {renderHeader()}
                <h4><b>Související AIPy</b></h4>
                <b>Počet balíčků </b>{node.value?.length} <br />
                <b>Seznam balíčků </b>{node.value?.map((aipId, index) => (
                    <>
                        {index > 0 && ", "}
                        <a href={`/aip/${aipId}`}>
                            {aipId}
                        </a> 
                    </>
                ))}
            </div>
        );
    }

    return (
       <div className="py-2">
           {renderHeader()}

            <h4><b>Související AIP</b>
            {aip.data && <Button as="a" href={`/aip/${aip.data.aipId}`}>
                <Icon glyph="fa-sign-in" />
            </Button>}
            </h4> 
            {aip.data && <AipDetailBody detail={aip.data} />}
       </div>
    );
}

export default NodeDetail;