import { FC, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import * as aipActions from 'actions/aip/aip';
import AipDetailBody from "components/aip/AipDetailBody";
import { Button } from "components/ui";
import { Icon, i18n } from "components/shared";
import { TreeItemValue } from "@fluentui/react-components";

type NodeDetailProps = {
    tree: any;
    selectedNode: TreeItemValue;
    setSelectedAips: (params: {aipIds: number[], daLevelViewId: number}) => void;
}

const NodeDetail: FC<NodeDetailProps> = ({tree, selectedNode, setSelectedAips}: NodeDetailProps) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP))
    const dispatch = useThunkDispatch();
    const node = tree.nodes.find(n => n.UUID == selectedNode);

    useEffect(() => {
        if(node) {
            setSelectedAips({aipIds: node.value, daLevelViewId: node.daLeveViewId})
            dispatch(aipActions.aipFetchIfNeeded(node.value[0]));
        }
    }, [selectedNode]);

    const renderHeader = () => (
        <>
            <h4><b>{i18n("aip.detail.assignment.description")}</b></h4>
            <p><b>{i18n("aip.detail.assignment.name")} </b>{node.name}</p>
        </>
    );

    if(!node) {
        return null;
    }

    if (node.value?.length > 1) {
        return (
            <div>
                {renderHeader()}
                <h4><b>{i18n("aip.detail.assignment.relatedAips")}</b></h4>
                <b>{i18n("aip.detail.assignment.packagesNo")} </b>{node.value?.length} <br />
                <b>{i18n("aip.detail.assignment.packages")} </b>{node.value?.map((aipId, index) => (
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
            <h4><b>{i18n("aip.detail.assignment.relatedAip")}</b>
            {aip.data && <Button as="a" href={`/aip/${aip.data.aipId}`}>
                <Icon glyph="fa-sign-in" />
            </Button>}
            </h4>
            {aip.data &&
                <AipDetailBody detail={aip.data} />
            }
       </div>
    );
}

export default NodeDetail;
