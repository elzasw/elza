import { showAsyncWaiting, modalDialogShow, modalDialogHide } from 'actions/global/modalDialog';
import * as perms from 'actions/user/Permission';
import { WebApi } from 'actions/WebApi';
import { ApAccessPointVO } from 'api/ApAccessPointVO';
import { ExtEntityBinding } from 'elza-api';
import { RulDataTypeCodeEnum } from 'api/RulDataTypeCodeEnum';
import { RulDataTypeVO } from 'api/RulDataTypeVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { SyncState } from 'elza-api';
import { defineMessages, FormattedMessage, MessageDescriptor, useIntl } from 'react-intl';
import { messageFor } from 'components/shared/lang/dynamicMessage';
import { apDetailMessages, extStateMessages } from '../messages';
import { Icon, TooltipTrigger } from 'components/shared';
import { FC, useCallback, useState } from 'react';
import { Dropdown, DropdownButton } from 'react-bootstrap';
import { useDispatch, useSelector } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState, RefTablesState, ApExternalSystemSimpleVO } from 'typings/store';
import { SyncIcon } from "../sync-icon";
import { BindingIssuesIcon } from './BindingIssuesIcon';
import { BindingHistoryDialog } from './BindingHistoryDialog';
import './DetailHeader.scss';
import { DetailDescriptionsItemWithButton } from './DetailDescriptionsItem';
import { showConfirmDialog } from 'components/shared/dialog';
import { ApPushToExt } from 'components/registry/modal/ApPushToExt';
import { Button } from 'components/ui';

import { AP_EXT_SYSTEM_TYPE } from '../../../../constants';

const messages = defineMessages({
    actionHistory: { id: 'ap.binding.action.history', defaultMessage: 'Historie revizí' },
});

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

const getProcessingMessage = (descriptor: MessageDescriptor) => {
    return <h4 className="processing"><FormattedMessage {...descriptor} /></h4>;
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
        const intl = useIntl();
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
                                    title={<FormattedMessage {...apDetailMessages.pushToExt} />}
                                    onClick={handlePushApToExt}
                                >
                                    <Icon glyph="fa-save" />
                                </Button> :
                                    <Button
                                        className="button"
                                        onClick={handlePushApToExt}
                                        title={<FormattedMessage {...apDetailMessages.pushToExt} />}
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
                                <span>{<FormattedMessage {...apDetailMessages.notInExt} />}</span>
                            </div>
                        </DetailDescriptionsItemWithButton>

                    </div>
                </div>
            );
        }

        const handleSynchronize = async (binding: ExtEntityBinding) => {
            const result = await dispatch(showConfirmDialog(intl.formatMessage(apDetailMessages.bindingActionSynchronizeConfirmation)));
            if (result) {
                dispatch(
                    showAsyncWaiting(
                        null,
                        getProcessingMessage(apDetailMessages.bindingProcessingSynchronize),
                        WebApi.synchronizeAccessPoint(item.id!, binding.externalSystemCode),
                        () => {
                            onInvalidateDetail && onInvalidateDetail();
                        },
                    ),
                );
            }
        };

        const handleUpdate = (binding: ExtEntityBinding) => {
            const extSystem = externalSystems.find((extSystem) => extSystem.code === binding.externalSystemCode);
            if (!extSystem) { throw Error("External system not found.") }
            dispatch(
                modalDialogShow(
                    this,
                    intl.formatMessage(apDetailMessages.pushToExtTitle),
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

        const handleTakeRelEntities = (binding: ExtEntityBinding) => {
            dispatch(
                showAsyncWaiting(
                    null,
                    getProcessingMessage(apDetailMessages.bindingProcessingTakeRelEntities),
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
        const [historyBindingId, setHistoryBindingId] = useState<number | null>(null);

        return (
            <div className="bindings" key="bindings">
            {historyBindingId !== null && (
                <BindingHistoryDialog
                    bindingId={historyBindingId}
                    open={true}
                    onClose={() => setHistoryBindingId(null)}
                />
            )}
                {item.bindings.map(binding => {
                    const externalSystem = externalSystems.find((externalSystem) => binding.externalSystemCode === externalSystem.code);
                    const tooltip = ('id: ' + binding.value) + (binding.extRevision ? (', uuid: ' + binding.extRevision) : '')
                        + (binding.extUser ? (', ' + intl.formatMessage(apDetailMessages.bindingUser) + ': ' + binding.extUser) : '');

                    const renderTooltip = () =>
                        <div style={{ textAlign: "left", padding: "4px" }}>
                            <div>id: {binding.value}</div>
                            {binding.extRevision && <div>rev_id: {binding.extRevision}</div>}
                            {binding.extUser && <div>{`${<FormattedMessage {...apDetailMessages.bindingUser} />}: ${binding.extUser}`}</div>}
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
                                        align={"end"}
                                    >
                                        {hasState(item.stateApproval, ["NEW", "TO_AMEND", "APPROVED"]) &&
                                            <Dropdown.Item key="synchronize" onClick={() => handleSynchronize(binding)}>
                                                {<FormattedMessage {...apDetailMessages.bindingActionSynchronize} />}
                                            </Dropdown.Item>}
                                        {hasUnimportedEntity(item, refTables) &&
                                            <Dropdown.Item
                                                key="take-rel-entities"
                                                onClick={() => handleTakeRelEntities(binding)}
                                            >
                                                {<FormattedMessage {...apDetailMessages.bindingActionTakeRelEntities} />}
                                            </Dropdown.Item>
                                        }
                                        <Dropdown.Item key="history" onClick={() => setHistoryBindingId(binding.id)}>
                                            <FormattedMessage {...messages.actionHistory} />
                                        </Dropdown.Item>
                                    </DropdownButton>
                                    {apExternalWr && hasState(item.stateApproval, ["NEW", "TO_AMEND", "APPROVED"])
                                        && binding.syncState === SyncState.LocalChange
                                        && (
                                            <Button
                                                className="button save-button"
                                                title={<FormattedMessage {...apDetailMessages.bindingActionUpdate} />}
                                                onClick={() => handleUpdate(binding)}
                                            >
                                                <Icon glyph="fa-save" />
                                            </Button>
                                        )}
                                </>}
                            >
                                <TooltipTrigger content={renderTooltip()}>
                                    <div className="info">
                                        {/* {<FormattedMessage {...apDetailMessages.bindingSource} />}{': '} */}
                                        <span className="system">{externalSystem?.name}</span>
                                        <span className="binding-id">id: <span className="binding-value">{binding.value}</span></span>
                                        <span><FormattedMessage {...messageFor(extStateMessages, binding.extState ?? "", apDetailMessages.bindingExtStateERS_NEW)} /></span>
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
                                    <BindingIssuesIcon binding={binding} />
                                </div>
                            </DetailDescriptionsItemWithButton>

                        </div>
                    );
                })}
            </div>
        );
    };
