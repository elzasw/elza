import { FC, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import * as aipActions from 'actions/aip/aip';
import { AipDetailBody } from "components/aip/AipDetailBody";
import { Button } from "components/ui";
import { Icon} from "components/shared";
import { FormattedMessage } from 'react-intl';
import { daoMessages } from 'components/arr/daoMessages';
import { TreeItemValue } from "@fluentui/react-components";
import {serverContextPath} from "../../../../api";
import { useNodeName } from "components/aip/explorer/levels";

type NodeDetailProps = {
    tree: any;
    selectedNode: TreeItemValue;
    setSelectedAips: (params: {aipIds: number[], daLevelViewId: number}) => void;
}

const NodeDetail: FC<NodeDetailProps> = ({tree, selectedNode, setSelectedAips}: NodeDetailProps) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP))
    const dispatch = useThunkDispatch();
    const node = tree.nodes.find(n => n.UUID == selectedNode);
    const nodeName = useNodeName();

    useEffect(() => {
        if(node) {
            setSelectedAips({aipIds: node.value, daLevelViewId: node.daLeveViewId})
            dispatch(aipActions.aipFetchIfNeeded(node.value[0]));
        }
    }, [selectedNode]);

    const renderHeader = () => (
        <>
            <h4><b>{<FormattedMessage {...daoMessages.aipDetailAssignmentDescription} />}</b></h4>
            <p><b>{<FormattedMessage {...daoMessages.aipDetailAssignmentName} />} </b>{nodeName(node)}</p>
        </>
    );

    if(!node) {
        return null;
    }

    if (node.value?.length > 1) {
        return (
            <div>
                {renderHeader()}
                <h4><b>{<FormattedMessage {...daoMessages.aipDetailAssignmentRelatedAips} />}</b></h4>
                <b>{<FormattedMessage {...daoMessages.aipDetailAssignmentPackagesNo} />} </b>{node.value?.length} <br />
                <b>{<FormattedMessage {...daoMessages.aipDetailAssignmentPackages} />} </b>{node.value?.map((aipId, index) => (
                    <>
                        {index > 0 && ", "}
                        <a href={`${serverContextPath}/aip/${aipId}`}>
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
            <h4><b>{<FormattedMessage {...daoMessages.aipDetailAssignmentRelatedAip} />}</b>
            {aip.data && <Button as="a" href={`${serverContextPath}/aip/${aip.data.aipId}`}>
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
