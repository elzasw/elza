/**
 * Stránka pro vytvoreni archivni entity.
 */

import { refApTypesFetchIfNeeded } from 'actions/refTables/apTypes';
import { descItemTypesFetchIfNeeded } from "actions/refTables/descItemTypes";
import { refPartTypesFetchIfNeeded } from "actions/refTables/partTypes";
import { refRulDataTypesFetchIfNeeded } from "actions/refTables/rulDataTypes";
import { requestScopesIfNeeded } from "actions/refTables/scopesData";
import { ApAccessPointCreateVO } from 'api/ApAccessPointCreateVO';
import React, { FC, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation } from "react-router-dom";
import { WebApi } from '../../actions';
import { modalDialogShow } from '../../actions/global/modalDialog.jsx';
import CreateAccessPointModal from '../../components/registry/modal/CreateAccessPointModal';
import { i18n } from '../../components/shared';
import { AP_VIEW_SETTINGS } from '../../constants';
import { DetailActions } from '../../shared/detail';
import PageLayout from "../shared/layout/PageLayout";
import './EntityCreatePage.scss';
import { ApAccessPointVO } from 'api';

function useQuery() {
    return new URLSearchParams(useLocation().search);
}

enum ResponseStatus {
    SUCCESS = "SUCCESS",
    CANCEL = "CANCEL",
}

const replaceStrings = (
    string: string, 
    replaceArray: Array<[string, string]>
) => {
    let newString = string;
    replaceArray.forEach((replace)=>{ newString = newString.replace(replace[0], replace[1]) })
    return newString;
}

export const EntityCreatePage:FC = () => {
    const dispatch = useDispatch();
    const splitter:any = useSelector<any>((store)=>(store.splitter));
    const query = useQuery();
    const responseUrl = query.get("response");
    const entityClass = query.get("entity-class");
    const entityClasses = entityClass ? entityClass.split(",") : [];

    const handleSubmit = async (formData: any) => {
        if (!formData.partForm) {
            return Promise.reject("");
        }
        const data = {
            ...formData,
            partForm: {
                ...formData.partForm,
                items: formData.partForm.items.filter((i:any) => i.value != null)
            }
        }
        const submitData:ApAccessPointCreateVO = {
            partForm: data.partForm,
            scopeId: data.scopeId,
            typeId: data.apType.id,
        };

        const entity = await WebApi.createAccessPoint(submitData);
        location.assign(getResponseUrl(ResponseStatus.SUCCESS, entity));
    }

    const handleDialogClose = () => {
        location.assign(getResponseUrl(ResponseStatus.CANCEL))
    }

    const getResponseUrl = (
        status: ResponseStatus, 
        entity?: ApAccessPointVO
    ) => {
        return replaceStrings(responseUrl || location.href, [
            ["{status}", status],
            ["{entityUuid}", entity?.id ? entity.uuid : ""],
            ["{entityId}", entity?.id ? entity.id.toString() : ""]
        ]);
    }

    const showDialog = () => {
        dispatch(
            modalDialogShow(
                this,
                i18n('registry.addRegistry'),
                <CreateAccessPointModal
                    initialValues={{}}
                    onSubmit={handleSubmit}
                    apTypeFilter={entityClasses}
                />,
                'dialog-lg',
                handleDialogClose,
            ),
        );
    }


    useEffect(()=>{
            dispatch(fetchApTypeViewIfNeeded());
            dispatch(refApTypesFetchIfNeeded());
            dispatch(refPartTypesFetchIfNeeded());
            dispatch(requestScopesIfNeeded());
            dispatch(descItemTypesFetchIfNeeded());
            dispatch(refRulDataTypesFetchIfNeeded());
    },[dispatch]);

    useEffect(showDialog, [dispatch])

    return (
        <PageLayout 
            splitter={splitter}
            className='entity-create-page' 
            centerPanel={<div/>}
        />
    )
}

const fetchApTypeViewIfNeeded = () => {
    return DetailActions.fetchIfNeeded(AP_VIEW_SETTINGS, '', () => {
        return WebApi.getApTypeViewSettings();
    })
}
