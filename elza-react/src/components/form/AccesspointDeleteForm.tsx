import React, { FC, useEffect, useState } from 'react';
import UsageFormUntyped from './UsageForm';
import * as types from 'actions/constants/ActionTypes';
import {i18n} from 'components/shared';
import {WebApi} from '../../actions/WebApi';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import {addToastrSuccess} from '../shared/toastr/ToastrActions';
import {useDispatch} from 'react-redux';
import {modalDialogHide} from '../../actions/global/modalDialog';

import { Api, ApAccessPointVO } from "../../api";
import { DeleteAccessPointDetailReplaceTypeEnum as ReplaceType} from "elza-api";
import Icon from 'components/shared/icon/FontIcon';
import './UsageForm.scss';

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

// Workaround pro rozbite typy v kombinaci 
// typovaneho 'withRouter' z 'react-router' 
// a netypovaneho 'UsageForm.jsx'
const UsageForm = UsageFormUntyped as any;

export const AccessPointDeleteForm:FC<{
    detail: ApAccessPointVO;
    onSubmitSuccess?: () => void;
}> = ({
    detail,
    onSubmitSuccess = () => {}
}) => {
    const [data, setData] = useState<RegistryUsage | null>(null)
    const [inProgress, setInProgress] = useState(false);
    const dispatch = useDispatch();


    const deleteAccessPoint = (newNode: ApAccessPointVO, replaceType: ReplaceType) => {
        if (newNode) {
            setInProgress(true);
            Api.accesspoints.deleteAccessPoint(detail.id.toString(), {
                replacedBy: newNode.id.toString(),
                replaceType,
            }).then(() => {
                    onSubmitSuccess();
                    dispatch(addToastrSuccess(i18n('registry.replaceSuccess')));
                    dispatch(modalDialogHide());
                    setInProgress(false);
                }).catch(() => {
                    dispatch(modalDialogHide());
                    setInProgress(false);
                });
        }
    }

    const handleReplace = (replacementNode: ApAccessPointVO) => {
        deleteAccessPoint(replacementNode, ReplaceType.Simple);
    };

    const handleMerge = (replacementNode: ApAccessPointVO) => {
        deleteAccessPoint(replacementNode, ReplaceType.CopyAll);
    }

    useEffect(()=>{
        WebApi.findRegistryUsage(detail.id).then(data => {
            setData(data);
        });
    }, [detail])

    if(!data){ return <HorizontalLoader />}
    if(inProgress){ return <div className="in-progress">
        <Icon glyph="fa-refresh" className="fa-spin"/>
        &nbsp;
        {i18n('accesspoint.removeDuplicity.inProgress')}
    </div>}

    return <UsageForm
        detail={detail}
        treeArea={types.FUND_TREE_AREA_USAGE}
        onReplace={handleReplace}
        onMerge={handleMerge}
        type="registry"
        //replaceButtonText={`${i18n('accesspoint.removeDuplicity.resolve')}`}
        replaceText={`${i18n('accesspoint.removeDuplicity.replacingAccesspoint')}:`}
        replaceType="delete"
        nameLabel={`${i18n('accesspoint.removeDuplicity.replacedAccesspoint')}:`}
        data={data}
        /> 
}
