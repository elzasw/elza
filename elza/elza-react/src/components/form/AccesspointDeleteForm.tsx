import React, { FC, useEffect, useState } from 'react';
import UsageForm from './UsageForm';
import * as types from 'actions/constants/ActionTypes';
import {i18n} from 'components/shared';
import {WebApi} from '../../actions/WebApi';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import {addToastrSuccess} from '../shared/toastr/ToastrActions';
import {useDispatch} from 'react-redux';
import {modalDialogHide} from '../../actions/global/modalDialog';

import { Api, ApAccessPointVO } from "../../api";

// Docasne definice
// bude nahrazeno typy z vygenerovaneho api
interface Occurence {
    id: number | string;
    type: "ARR_DATA_RECORD_REF";
}

interface Node {
    id: number | string;
    title: string;
    occurences: Occurence[];
}

interface RegistryUsage {
    id: number | string;
    name: string;
    nodeCount: number;
    nodes: Node[];
}

export const AccessPointDeleteForm:FC<{
    detail: ApAccessPointVO;
}> = ({
    detail
}) => {
    const [data, setData] = useState<RegistryUsage | null>(null)
    const dispatch = useDispatch();

    const handleReplace = (selectedReplacementNode: ApAccessPointVO) => {
        if (selectedReplacementNode) {
            Api.accesspoints.deleteAccessPoint(detail.id.toString(), {
                replacedBy: selectedReplacementNode.id.toString(),
            }).then(() => {
                dispatch(addToastrSuccess(i18n('registry.replaceSuccess')));
                dispatch(modalDialogHide());
            });
        }
    };

    useEffect(()=>{
        WebApi.findRegistryUsage(detail.id).then(data => {
            setData(data);
        });
    }, [detail])

    return data ? 
        <UsageForm
            detail={detail}
            treeArea={types.FUND_TREE_AREA_USAGE}
            onReplace={handleReplace}
            type="registry"
            replaceButtonText={`${i18n('accesspoint.removeDuplicity.resolve')}`}
            replaceText={`${i18n('accesspoint.removeDuplicity.replacingAccesspoint')}:`}
            replaceType="delete"
            nameLabel={`${i18n('accesspoint.removeDuplicity.replacedAccesspoint')}:`}
            data={data}
        /> : 
            <HorizontalLoader />;
}
