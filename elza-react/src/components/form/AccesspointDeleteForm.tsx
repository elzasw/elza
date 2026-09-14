import React, { FC, useEffect, useState } from 'react';
import UsageFormUntyped from './UsageForm';
import * as types from 'actions/constants/ActionTypes';
import {} from 'components/shared';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny; "rejstříkové heslo" sjednoceno
// na "archivní entita", jak se ta věc jmenuje ve zbytku aplikace.
const messages = defineMessages({
    replaceSuccess: { id: 'registry.replaceSuccess', defaultMessage: 'Archivní entita byla úspěšně nahrazena' },
    inProgress: {
        id: 'accesspoint.removeDuplicity.inProgress',
        defaultMessage: 'Probíhá odstranění duplicity...',
    },
    resolve: { id: 'accesspoint.removeDuplicity.resolve', defaultMessage: 'Vyřešit duplicitu' },
    replacingAccesspoint: {
        id: 'accesspoint.removeDuplicity.replacingAccesspoint',
        defaultMessage: 'Nahrazující přístupový bod',
    },
    replacedAccesspoint: {
        id: 'accesspoint.removeDuplicity.replacedAccesspoint',
        defaultMessage: 'K nahrazení',
    },
});
import {WebApi} from '../../actions/WebApi';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import {addToastrSuccess} from '../shared/toastr/ToastrActions';
import {useDispatch} from 'react-redux';
import {modalDialogHide} from '../../actions/global/modalDialog';

import { Api, ApAccessPointVO } from "../../api";
import { ReplaceType } from "elza-api";
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
    const intl = useIntl();
    const [data, setData] = useState<RegistryUsage | null>(null)
    const [inProgress, setInProgress] = useState(false);
    const dispatch = useDispatch();


    const deleteAccessPoint = (newNode: ApAccessPointVO, replaceType: ReplaceType) => {
        if (newNode) {
            setInProgress(true);
            Api.accesspoints.accessPointDeleteAccessPoint(detail.id.toString(), {
                replacedBy: newNode.id.toString(),
                replaceType,
            }).then(() => {
                    onSubmitSuccess();
                    dispatch(addToastrSuccess(intl.formatMessage(messages.replaceSuccess)));
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
        {intl.formatMessage(messages.inProgress)}
    </div>}

    return <UsageForm
        detail={detail}
        treeArea={types.FUND_TREE_AREA_USAGE}
        onReplace={handleReplace}
        onMerge={handleMerge}
        type="registry"
        //replaceButtonText={`${intl.formatMessage(messages.resolve)}`}
        replaceText={`${intl.formatMessage(messages.replacingAccesspoint)}:`}
        replaceType="delete"
        nameLabel={`${intl.formatMessage(messages.replacedAccesspoint)}:`}
        data={data}
        />
}
