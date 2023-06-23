import { showAsyncWaiting, modalDialogShow, modalDialogHide } from 'actions/global/modalDialog';
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
import React, { FC, useCallback } from 'react';
import { Dropdown, DropdownButton } from 'react-bootstrap';
import { useDispatch, useSelector } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState, RefTablesState, ApExternalSystemSimpleVO } from 'typings/store';
import { SyncIcon } from "../sync-icon";
import './DetailHeader.scss';
import { DetailDescriptionsItemWithButton } from './DetailDescriptionsItem';
import { showConfirmDialog } from 'components/shared/dialog';
import { ApPushToExt } from 'components/registry/modal/ApPushToExt';
import { Button } from 'components/ui';
import { useHistory } from 'react-router';
import { Api } from 'api';
import { goToAe } from 'actions/registry/registry';
import { ApCopyModal } from 'components/registry/modal/ap-copy';
import { objectById } from 'stores/app/utils';

import { AP_EXT_SYSTEM_TYPE } from '../../../../constants';

const useThunkDispatch = <State,>(): ThunkDispatch<State, void, AnyAction> => useDispatch()

const hasUnimportedEntity = (accessPoint: ApAccessPointVO, refTables: RefTablesState) => {
    const externalEntity = accessPoint.parts.find((part) => {
        return part.items?.find((item: any) => {
            const itemType = refTables.descItemTypes.itemsMap[item.typeId] as RulDescItemTypeExtVO;
            const dataType = refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO;
            const isRef = dataType.code === RulDataTypeCodeEnum.RECORD_REF;
            if (isRef && !item.accessPoint || isRef && !item.value) {
                return item;
            }
        })
    })
    return !!externalEntity;
}

const getProcessingMessage = (key: string) => {
    return <h4 className="processing">{i18n(key)}</h4>;
};

export const EntityBindings: FC<{
    item?: ApAccessPointVO;
    onInvalidateDetail?: () => void;
    onPushApToExt?: (item: ApAccessPointVO, extSystems: ApExternalSystemSimpleVO[]) => void;
}> = ({
    item,
    onInvalidateDetail,
    onPushApToExt,
}) => {
        const dispatch = useThunkDispatch()
        const userDetail = useSelector((state: AppState) => state.userDetail)
        const externalSystems = useSelector((state: AppState) => state.app.apExtSystemList.rows)
        const refTables = useSelector((state: AppState) => state.refTables)

        const scopeBoundExternalSystem = externalSystems.find((externalSystem) => externalSystem.scope === item?.scopeId);
        const isBoundExternalSystemComplete = scopeBoundExternalSystem?.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE;

        const handlePushApToExt = useCallback(() => {
            if (!item) { throw Error("Item data missing.") }
            const extSystems: ApExternalSystemSimpleVO[] = [];

            if (scopeBoundExternalSystem && isBoundExternalSystemComplete) {
                extSystems.push(scopeBoundExternalSystem);
            } else {
                const unboundExtSystems = externalSystems.filter(extSystem => {
                    const unboundExtSystem = item.bindings.find((binding) => binding.externalSystemCode == extSystem.code);
                    return unboundExtSystem == null;
                });
                extSystems.push(...unboundExtSystems);
            }
            onPushApToExt?.(item, extSystems);
        }, [item, scopeBoundExternalSystem, isBoundExternalSystemComplete])

        if (!item || externalSystems.length === 0) { return <></> }

        if (item?.bindings?.length === 0) {
            return (
                <div className="bindings">
                    <div className="binding" key={'no-binding'}>
                        <DetailDescriptionsItemWithButton
                            renderButton={() => <>
                                {scopeBoundExternalSystem && isBoundExternalSystemComplete ? <Button
                                    className="button save-button"
                                    title={i18n('ap.push-to-ext')}
                                    onClick={handlePushApToExt}
                                >
                                    <Icon glyph="fa-save" />
                                </Button> :
                                    <Button
                                        className="button"
                                        onClick={handlePushApToExt}
                                        title={i18n('ap.push-to-ext')}
                                    >
                                        <Icon glyph="fa-cloud-upload" />
                                    </Button>
                                }
                            </>}
                        >
                            <div className="info">
                                {scopeBoundExternalSystem
                                    && isBoundExternalSystemComplete
                                    && <span className="system">{scopeBoundExternalSystem.name}</span>
                                }
                                <span>{i18n('ap.not-in-ext')}</span>
                            </div>
                        </DetailDescriptionsItemWithButton>

                    </div>
                </div>
            );
        }

        const handleSynchronize = async (binding: ApBindingVO) => {
            const result = await dispatch(showConfirmDialog(i18n("ap.binding.action.synchronize.confirmation")));
            if (result) {
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
            }
        };

        const handleUpdate = (binding: ApBindingVO) => {
            const extSystem = externalSystems.find((extSystem) => extSystem.code === binding.externalSystemCode);
            if (!extSystem) { throw Error("External system not found.") }
            dispatch(
                modalDialogShow(
                    this,
                    i18n('ap.push-to-ext.title'),
                    <ApPushToExt
                        detail={item}
                        onSubmit={async () => {
                            try {
                                await WebApi.updateArchiveEntity(item.id!, binding.externalSystemCode);
                            } catch (e) {
                                throw Error(e);
                            }
                            dispatch(modalDialogHide());
                            return;
                        }}
                        onClose={() => {
                            modalDialogHide();
                        }}
                        extSystems={[extSystem]}
                    />,
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
                    const externalSystem = externalSystems.find((externalSystem) => binding.externalSystemCode === externalSystem.code);
                    const tooltip = ('id: ' + binding.value) + (binding.extRevision ? (', uuid: ' + binding.extRevision) : '')
                        + (binding.extUser ? (', ' + i18n('ap.binding.user') + ': ' + binding.extUser) : '');

                    const renderTooltip = () =>
                        <div style={{ textAlign: "left", padding: "4px" }}>
                            <div>id: {binding.value}</div>
                            {binding.extRevision && <div>rev_id: {binding.extRevision}</div>}
                            {binding.extUser && <div>{`${i18n('ap.binding.user')}: ${binding.extUser}`}</div>}
                        </div>

                    return (
                        <div className="binding" key={'binding-' + binding.id}>
                            <DetailDescriptionsItemWithButton
                                renderButton={() => <>
                                    <DropdownButton
                                        variant="action"
                                        id={'binding-action-' + binding.id}
                                        title={<Icon glyph="fa-ellipsis-h" />}
                                        className={'binding-dropdown button'}
                                        alignRight={true}
                                    >
                                        {hasState(item.stateApproval, ["NEW", "TO_AMEND", "APPROVED"]) &&
                                            <Dropdown.Item key="synchronize" onClick={() => handleSynchronize(binding)}>
                                                {i18n('ap.binding.action.synchronize')}
                                            </Dropdown.Item>}
                                        {hasUnimportedEntity(item, refTables) &&
                                            <Dropdown.Item
                                                key="take-rel-entities"
                                                onClick={() => handleTakeRelEntities(binding)}
                                            >
                                                {i18n('ap.binding.action.take-rel-entities')}
                                            </Dropdown.Item>
                                        }
                                    </DropdownButton>
                                    {apExternalWr && hasState(item.stateApproval, ["NEW", "TO_AMEND", "APPROVED"])
                                        && binding.syncState === SyncState.LOCAL_CHANGE
                                        && (
                                            <Button
                                                className="button save-button"
                                                title={i18n('ap.binding.action.update')}
                                                onClick={() => handleUpdate(binding)}
                                            >
                                                <Icon glyph="fa-save" />
                                            </Button>
                                        )}
                                </>}
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
                                    <SyncIcon syncState={binding.syncState || undefined} />
                                </div>
                            </DetailDescriptionsItemWithButton>

                        </div>
                    );
                })}
            </div>
        );
    };
