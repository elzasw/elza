import { setVisiblePolicyRequest, visiblePolicyFetchIfNeeded } from 'actions/arr/visiblePolicy.jsx';
import React, { FC, useEffect, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { useDispatch, useSelector } from 'react-redux';
import { BaseRefTableStore } from "../../../typings/BaseRefTableStore";
import { AppState, VisiblePolicyOtherData, VisiblePolicyRefItem } from "../../../typings/store";
import { Loading } from '../../shared/index';
import { NodeSettingsForm, NodeSettingsFormFields } from "./";
import './NodeSettingsForm.scss';
import { VIEW_POLICY_STATE } from "./static-data";

interface NodeSettingsModalProps {
    nodeId: number;
    fundVersionId: number;
    onSubmit: () => void;
    onSubmitSuccess: () => void;
    onClose: () => void;
}

const getInitialValues = (
    data: VisiblePolicyOtherData, 
    visiblePolicyTypes: BaseRefTableStore<VisiblePolicyRefItem>
) => {
    const rules = Object.values(data.nodePolicyTypeIdsMap).length > 0 ? 
            VIEW_POLICY_STATE.NODE : 
            VIEW_POLICY_STATE.PARENT;

    const idsMap = VIEW_POLICY_STATE.PARENT ? data.policyTypeIdsMap : data.nodePolicyTypeIdsMap;

    const records: Record<number | string, boolean> = {};
    if(visiblePolicyTypes.itemsMap) {
        Object.keys(visiblePolicyTypes.itemsMap || {}).forEach(id => {
            records[id] = idsMap[id] || false;
        });
    }

    const nodeExtensions: Record<number | string, boolean> = {};
    if(data.nodeExtensions){
        data.nodeExtensions.forEach((extension)=>{
            nodeExtensions[extension.id] = true;
        })
    }

    return {
        rules,
        records,
        nodeExtensions,
    };
}

// Workaround pro nespravny navratovy typ dispatch kvuli pouziti redux-thunk.
// Pro opraveni je potreba otypovat vytvareni store a vytvorit
// custom hook 'useThunkDispatch' a ten pouzivat misto 'useDispatch':
// `const useThunkDispatch = () => useDispatch<typeof store.dispatch>();`
// ^^ prevzato z poznamky u 'useDispatch' ^^
// Docasne typy a dispatch pro redux-thunk
type ThunkAction<R = any> = (func: (...args: any) => any) => (...args:any) => Promise<R>;
type ThunkDispatch = <F extends ReturnType<ThunkAction>>(func: F) => ReturnType<F>;
const useThunkDispatch = ():ThunkDispatch => useDispatch()

export const NodeSettingsModal:FC<NodeSettingsModalProps> = ({
    nodeId,
    fundVersionId,
    onClose,
}) => {
    const visiblePolicy = useSelector((state: AppState) => state.arrRegion.visiblePolicy)
    const visiblePolicyTypes = useSelector((state: AppState) => state.refTables.visiblePolicyTypes)
    const dispatch = useThunkDispatch();
    const [loading, setLoading] = useState(true);

    useEffect(()=>{
        setLoading(true);
        dispatch(visiblePolicyFetchIfNeeded(nodeId, fundVersionId) as ReturnType<ThunkAction>).then(()=>{
            setLoading(false);
        })
    },[nodeId, fundVersionId, dispatch]);

    if (
        loading ||
        !visiblePolicy ||
        !visiblePolicy.fetched || 
        !visiblePolicy.otherData?.nodePolicyTypeIdsMap
    ) {
        return (
            <Modal.Body>
                <Loading />
            </Modal.Body>
        );
    }

    const handleSetVisiblePolicy = (data: NodeSettingsFormFields) => {
        const {records, rules, nodeExtensions} = data;
        const mapIds = rules !== "PARENT" ? records : {};

        const nodeExtensionsIds = Object.keys(nodeExtensions)
            .filter(key => nodeExtensions[key])

        return dispatch(setVisiblePolicyRequest(nodeId, fundVersionId, mapIds, false, nodeExtensionsIds) as ReturnType<ThunkAction>)
    }

    const handleSubmitSuccess = () => onClose();

    return <NodeSettingsForm
        initialValues={getInitialValues(visiblePolicy.otherData, visiblePolicyTypes)}
        onSubmit={handleSetVisiblePolicy}
        onSubmitSuccess={handleSubmitSuccess}
        onClose={onClose}
    />
}
