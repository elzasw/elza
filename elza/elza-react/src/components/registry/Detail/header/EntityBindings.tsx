import { showAsyncWaiting } from 'actions/global/modalDialog';
import * as perms from 'actions/user/Permission';
import { WebApi } from 'actions/WebApi';
import { ApAccessPointVO } from 'api/ApAccessPointVO';
import { ApBindingVO } from 'api/ApBindingVO';
import { RulDataTypeCodeEnum } from 'api/RulDataTypeCodeEnum';
import { RulDataTypeVO } from 'api/RulDataTypeVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { SyncState } from 'api/SyncState';
import i18n from 'components/i18n';
import { Icon, TooltipTrigger } from 'components/shared';
import React, { FC } from 'react';
import { Dropdown, DropdownButton } from 'react-bootstrap';
import { useDispatch, useSelector } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState, RefTablesState } from 'typings/store';
import { SyncIcon } from "../sync-icon";
import './DetailHeader.scss';
import {DetailDescriptionsItemWithButton} from './DetailDescriptionsItem';

const useThunkDispatch = <State,>():ThunkDispatch<State, void, AnyAction> => useDispatch()

const hasUnimportedEntity = (accessPoint: ApAccessPointVO, refTables: RefTablesState) => {
    const externalEntity = accessPoint.parts.find((part)=>{
        return part.items?.find((item: any)=>{
            const itemType = refTables.descItemTypes.itemsMap[item.typeId] as RulDescItemTypeExtVO;
            const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;
            const isRef = dataType.code === RulDataTypeCodeEnum.RECORD_REF;
            if(isRef && !item.accessPoint || isRef && !item.value){
                return item;
            }
        })
    })
    return !!externalEntity;
}

const getProcessingMessage = (key: string) => {
    return <h4 className="processing">{i18n(key)}</h4>;
};

export const EntityBindings:FC<{
    item?: ApAccessPointVO;
    onInvalidateDetail?: () => void;
}> = ({
    item,
    onInvalidateDetail,
}) => {
    const dispatch = useThunkDispatch()
    const userDetail = useSelector((state: AppState) => state.userDetail)
    const externalSystems = useSelector((state: AppState) => state.app.apExtSystemList.rows)
    const refTables = useSelector((state: AppState) => state.refTables)

    if(!item || item?.bindings.length === 0 || externalSystems.length === 0){return <></>}

    const handleSynchronize = (binding: ApBindingVO) => {
        dispatch(
            showAsyncWaiting(
                null,
                getProcessingMessage('ap.binding.processing.synchronize'),
                WebApi.synchronizeAccessPoint(item.id!, binding.externalSystemCode),
                () => {
                    onInvalidateDetail && onInvalidateDetail();
                },
            ),
        );
    };

    const handleUpdate = (binding: ApBindingVO) => {
        dispatch(
            showAsyncWaiting(
                null,
                getProcessingMessage('ap.binding.processing.update'),
                WebApi.updateArchiveEntity(item.id!, binding.externalSystemCode),
                () => {
                    onInvalidateDetail && onInvalidateDetail();
                },
            ),
        );
    };

    const handleTakeRelEntities = (binding: ApBindingVO) => {
        dispatch(
            showAsyncWaiting(
                null,
                getProcessingMessage('ap.binding.processing.take-rel-entities'),
                WebApi.takeRelArchiveEntities(item.id!, binding.externalSystemCode),
                () => {
                    onInvalidateDetail && onInvalidateDetail();
                },
            ),
        );
    };

    const hasState = (state: string, approvedStates: string[]) => {
        return approvedStates.indexOf(state) >= 0;
    }

    const apExternalWr = userDetail.hasOne(perms.AP_EXTERNAL_WR);
    return (
        <div className="bindings" key="bindings">
            {item.bindings.map(binding => {
                const externalSystem = externalSystems.find((externalSystem)=>binding.externalSystemCode === externalSystem.code);
                const tooltip = ('id: '+binding.value)+(binding.extRevision?(', uuid: '+binding.extRevision):'')
                    + (binding.extUser?(', '+i18n('ap.binding.user')+': '+binding.extUser):'');

                const renderTooltip = () =>
                    <div style={{textAlign: "left", padding: "4px"}}>
                        <div>id: {binding.value}</div>
                        {binding.extRevision && <div>uuid: {binding.extRevision}</div>}
                        {binding.extUser && <div>{`${i18n('ap.binding.user')}: ${binding.extUser}`}</div>}
                    </div>
                                        
                return (
                    <div className="binding" key={'binding-' + binding.id}>
                        <DetailDescriptionsItemWithButton
                            renderButton={() => <DropdownButton
                                variant="action"
                                id={'binding-action-' + binding.id}
                                title={<Icon glyph="fa-ellipsis-h" />}
                                className={'binding-dropdown'}
                                alignRight={true}
                            >
                                { hasState(item.stateApproval, ["NEW", "TO_AMEND", "APPROVED", "REV_NEW", "REV_AMEND"]) &&
                                    <Dropdown.Item key="synchronize" onClick={() => handleSynchronize(binding)}>
                                        {i18n('ap.binding.action.synchronize')}
                                    </Dropdown.Item>}
                                { apExternalWr && hasState(item.stateApproval, ["NEW", "TO_AMEND", "APPROVED"]) 
                                    && binding.syncState === SyncState.LOCAL_CHANGE 
                                    && (
                                        <Dropdown.Item key="update" onClick={() => handleUpdate(binding)}>
                                            {i18n('ap.binding.action.update')}
                                        </Dropdown.Item>
                                    )}
                                {hasUnimportedEntity(item, refTables) &&
                                    <Dropdown.Item
                                        key="take-rel-entities"
                                        onClick={() => handleTakeRelEntities(binding)}
                                    >
                                        {i18n('ap.binding.action.take-rel-entities')}
                                    </Dropdown.Item>
                            }
                            </DropdownButton>}
                        >
                            <TooltipTrigger content={renderTooltip()}>
                                <div className="info">
                                    {/* {i18n('ap.binding.source')}{': '} */}
                                    <span className="system">{externalSystem?.name}</span>
                                    {i18n('ap.binding.extState.' + binding.extState)}
                                    {binding.extReplacedBy && (
                                        <span className="link">
                                            {' '}
                                            (
                                            <a
                                                href={binding.detailUrlExtReplacedBy}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                            >
                                                {binding.detailUrlExtReplacedBy}
                                            </a>
                                            )
                                        </span>
                                    )}                                    
                                </div>
                            </TooltipTrigger>
                            <div className="action">
                                <SyncIcon syncState={binding.syncState || undefined}/>
                            </div>
                        </DetailDescriptionsItemWithButton>
                        
                    </div>
                );
            })}
        </div>
    );
};
