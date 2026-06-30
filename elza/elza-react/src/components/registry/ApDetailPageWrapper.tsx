import React, { ReactElement, useCallback, useEffect, useState, useRef, PropsWithChildren } from 'react';
import { connect, useSelector } from 'react-redux';
import { Action } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { objectByProperty } from "stores/app/utils";
import * as registry from '../../actions/registry/registry';
import { goToAe } from '../../actions/registry/registry';
import { WebApi } from '../../actions/WebApi';
import { ApAccessPointVO } from '../../api/ApAccessPointVO';
import { ApPartVO } from '../../api/ApPartVO';
import { ApValidationErrorsVO } from '../../api/ApValidationErrorsVO';
import { ApViewSettingRule, ApViewSettings } from '../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../api/PartValidationErrorsVO';
import { RulPartTypeVO } from '../../api/RulPartTypeVO';
import { AP_VALIDATION, AP_VIEW_SETTINGS, urlEntity, urlEntityRevision } from '../../constants';
import { DetailActions } from '../../shared/detail';
import { indexById } from '../../shared/utils';
import storeFromArea from '../../shared/utils/storeFromArea';
import { Bindings, DetailStoreState } from '../../types';
import { BaseRefTableStore } from '../../typings/BaseRefTableStore';
import { AppState, ApExternalSystemSimpleVO } from '../../typings/store';
import Loading from '../shared/loading/Loading';
import { DetailBodySection, DetailMultiSection } from './Detail/section';
import { DetailHeader } from './Detail/header';
import { showPartCreateModal, showPartEditModal } from './part-edit';
import i18n from 'components/i18n';
import { showConfirmDialog, showInfoDialog } from "components/shared/dialog";
import { modalDialogHide } from "../../actions/global/modalDialog";
import { formatExportIssues, IssueNavTarget } from "./formatExportIssues";
import './ApDetailPageWrapper.scss';
import { RevisionPart, getRevisionParts } from './revision';
import { Api } from '../../api';
import { RouteComponentProps, useHistory, withRouter } from "react-router";
import { RevStateApproval } from 'api/RevStateApproval';
import Icon from 'components/shared/icon/FontIcon';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { SyncProgress } from 'elza-api';
import { WebsocketEventType } from 'components/shared/web-socket/enums';
import { addToastrDanger } from 'components/shared/toastr/ToastrActions';
import { WaitingOverlay } from 'components/shared/waiting-overlay';
import { useThunkDispatch, useLocalStorageState } from 'utils/hooks';

function createBindings(accessPoint: ApAccessPointVO | undefined) {
    const bindingsMaps: Bindings = {
        itemsMap: {},
        partsMap: {},
    };

    const newItem = (id: number, sync: boolean, map: { [key: number]: boolean }) =>
        (map[id] || true) && sync;

    if (accessPoint) {
        const bindings = accessPoint.bindings || [];
        bindings.forEach(externalId => {
            const bindingItemList = externalId.bindingItemList || [];
            bindingItemList.forEach(item => {
                if (item.itemId) {
                    bindingsMaps.itemsMap[item.itemId] = newItem(item.itemId, item.sync, bindingsMaps.itemsMap);
                } else if (item.partId) {
                    bindingsMaps.partsMap[item.partId] = newItem(item.partId, item.sync, bindingsMaps.partsMap);
                }
            });
        });
    }
    return bindingsMaps;
}

export function sortPart(items: RulPartTypeVO[], data: ApViewSettingRule | undefined) {
    const parts = [...items];
    if (data && data.partsOrder) {
        parts.sort((a, b) => {
            const aIndex = indexById(data.partsOrder, a.code, 'code') || 0;
            const bIndex = indexById(data.partsOrder, b.code, 'code') || 0;
            return aIndex - bIndex;
        });
    }
    return parts;
}

function sortPrefer(parts: ApPartVO[], preferredPart?: number) {
    if (preferredPart != null) {
        parts.sort((a, b) => {
            if (a.id == preferredPart) {
                return -1;
            } else if (b.id == preferredPart) {
                return 1;
            } else {
                return 0;
            }
        });
    }
    return parts;
}

type OwnProps = {
    id: number; // ap id
    apVersion: number;
    sider: ReactElement;
    editMode: boolean;
    globalCollapsed: boolean;
    apValidation: DetailStoreState<ApValidationErrorsVO>;
    apViewSettings: DetailStoreState<ApViewSettings>;
    globalEntity: boolean;
    select: boolean;
    revisionActive?: boolean;
    onPushApToExt?: (item: ApAccessPointVO, extSystems: ApExternalSystemSimpleVO[]) => void;
};

type Props = OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps>;

let scrollTop: number | undefined = undefined;

export enum ExportState {
    PENDING = "PENDING",
    STARTED = "STARTED",
    NEED_CONFIRM = "NEED_CONFIRM",
    COMPLETED = "COMPLETED",
}

/**
 * Detail globální archivní entity.
 */
const ApDetailPageWrapper: React.FC<Props> = ({
    id, // ap id
    apVersion,
    editMode,
    globalCollapsed,
    apValidation,
    apViewSettings,
    globalEntity,
    detail,
    refreshDetail,
    fetchViewSettings,
    refreshValidation,
    setPreferred,
    setRevisionPreferred,
    deletePart,
    deleteRevisionPart,
    showConfirmDialog,
    showPartEditModal,
    showPartCreateModal,
    refTables,
    select,
    onPushApToExt,
    revisionActive: revisionActiveUrl = false,
}) => {
    const currentUserId = useSelector((state: AppState) => state.userDetail?.id as number | null);
    const apTypeId = detail.fetched && detail.data ? detail.data.typeId : 0;

    const [collapsed, setCollapsed] = useState<boolean>(false);
    const [localGlobalCollapsed, setLocalGlobalCollapsed] = useLocalStorageState<boolean>('apDetail-globalCollapsed', true);
    const [exportState, setExportState] = useState<ExportState>(ExportState.COMPLETED);
    const [exportMessage, setExportMessage] = useState<string>(null);
    const [itemQueueId, setItemQueueId] = useState<number>(-1);
    const [revisionActive, setRevisionActive] = useState<boolean>(revisionActiveUrl);
    const autoEnabledForEntity = useRef<number | null>(null);

    const detailFetched = detail.fetched;
    const detailIsFetching = detail.isFetching;
    const detailHasRevision = !!detail.data?.revStateApproval;
    const loadedEntityId = detailFetched ? detail.data?.id : undefined;
    const loadedAssignedTo = detail.data?.assignedTo;

    const containerRef = useRef<HTMLDivElement>(null);

    const websocket = useWebsocket();
    const bindings = detail.data?.bindings || [];
    const dispatch = useThunkDispatch();
    const history = useHistory();

    // URL is the source of truth for revisionActive; keep local state in sync
    // when navigation (back/forward, external link, other effects) changes it.
    useEffect(() => {
        setRevisionActive(revisionActiveUrl);
    }, [revisionActiveUrl]);

    // Auto-enable revision view when the current user is the assignee of the
    // loaded entity. Runs once per entity load; a later manual toggle-off is
    // preserved because the ref remembers we've already applied the default.
    useEffect(() => {
        if (select || loadedEntityId == null) { return; }
        if (autoEnabledForEntity.current === loadedEntityId) { return; }

        const isAssignee = currentUserId != null && loadedAssignedTo === currentUserId;
        if (detailHasRevision && isAssignee) {
            setRevisionActive(true);
        }
        autoEnabledForEntity.current = loadedEntityId;
    }, [loadedEntityId, loadedAssignedTo, detailHasRevision, currentUserId, select]);

    useEffect(() => {
        if (id) {
            refreshDetail(id, false, false, revisionActive);
        }
    }, [id]);

    // Strip /revision from the URL when the loaded entity has no revision.
    // Reacts to URL state, not local toggle state, so back/forward navigation
    // can't ping-pong against a stale local value.
    useEffect(() => {
        if (select || detailIsFetching || !detailFetched) { return; }
        if (!detailHasRevision && revisionActiveUrl) {
            dispatch(goToAe(history, id, false, !select, false, true));
        }
    }, [revisionActiveUrl, select, detailFetched, detailIsFetching, detailHasRevision, id])

    // Push local toggle/auto-enable changes into the URL. Skips URL-driven
    // changes (those are handled by the sync-down effect above), so back/forward
    // navigation doesn't trigger a counter-redirect.
    const prevRevisionActiveUrl = useRef(revisionActiveUrl);
    useEffect(() => {
        const urlJustChanged = prevRevisionActiveUrl.current !== revisionActiveUrl;
        prevRevisionActiveUrl.current = revisionActiveUrl;
        if (urlJustChanged) { return; }
        if (select) { return; }
        if (revisionActive !== revisionActiveUrl) {
            dispatch(goToAe(history, id, false, !select, revisionActive, true));
        }
    }, [revisionActive, revisionActiveUrl, select, id])

    // Scrolls a part into view by its data-part-id attribute; silent no-op if the
    // part is not currently rendered (e.g. user navigated away between dialog open
    // and click, or part was deleted locally).
    const scrollToPart = useCallback((partId: number) => {
        // small delay so React can commit the modal-hide before we measure layout
        setTimeout(() => {
            const node = document.querySelector(`[data-part-id="${partId}"]`);
            if (node instanceof HTMLElement) {
                node.scrollIntoView({ behavior: "smooth", block: "center" });
            } else {
                // eslint-disable-next-line no-console
                console.debug("ApDetailPageWrapper: part not found in DOM for nav", partId);
            }
        }, 50);
    }, []);

    // Invoked from the export-issue dialog when the user clicks a resolved
    // part/item/entity name. Closes the dialog and performs the navigation.
    // For a NEED_CONFIRM dialog this also cancels the queue item server-side
    // so the queue doesn't stay stuck awaiting user input — the user can
    // re-submit later if they want to see the warnings again.
    const handleIssueNav = useCallback((target: IssueNavTarget) => {
        dispatch(modalDialogHide());
        if (exportState === ExportState.NEED_CONFIRM && itemQueueId > 0) {
            // fire-and-forget cancel; server moves queue item to EXPORT_CANCELLED
            Api.accesspoints.accessPointExportForceOrNo(itemQueueId, false);
            setExportState(ExportState.COMPLETED);
        }
        if (target.type === "part") {
            scrollToPart(target.id);
        } else if (target.type === "entity") {
            dispatch(goToAe(history, target.id, false, true, false, false));
        }
    }, [dispatch, exportState, itemQueueId, history, scrollToPart]);

    // show accesspoint export message on websocket message
    useEffect(() => {
        const eventMap = {
            [WebsocketEventType.ACCESS_POINT_EXPORT_NEW]: ({ accessPointId }) => {
                if (accessPointId.toString() === id.toString()) {
                    setExportState(ExportState.PENDING);
                }
            },
            [WebsocketEventType.ACCESS_POINT_EXPORT_STARTED]: ({ accessPointId }) => {
                if (accessPointId.toString() === id.toString()) {
                    setExportState(ExportState.STARTED);
                }
            },
            [WebsocketEventType.ACCESS_POINT_EXPORT_NEED_CONFIRM]: ({ accessPointId, state, itemQueueId }) => {
                if (accessPointId.toString() === id.toString()) {
                    setExportMessage(state || i18n("ap.push-to-ext.failed.message"));
                    setItemQueueId(itemQueueId);
                    setExportState(ExportState.NEED_CONFIRM);
                    refreshDetail(id, true, false, revisionActive);
                }
            },
            [WebsocketEventType.ACCESS_POINT_EXPORT_COMPLETED]: ({ accessPointId }) => {
                if (accessPointId.toString() === id.toString()) {
                    setExportState(ExportState.COMPLETED);
                    refreshDetail(id, true, false, revisionActive);
                }
            },
            [WebsocketEventType.ACCESS_POINT_EXPORT_FAILED]: ({ accessPointId, state }) => {
                if (accessPointId.toString() === id.toString()) {
                    const body = state
                        ? formatExportIssues(state, i18n("ap.push-to-ext.failed.intro"), "ERROR", handleIssueNav)
                        : i18n("ap.push-to-ext.failed.message");
                    dispatch(showInfoDialog({
                        title: i18n("ap.push-to-ext.failed.title"),
                        message: body,
                    }));
                    setExportState(ExportState.COMPLETED);
                    refreshDetail(id, true, false, revisionActive);
                }
            },
        }

        const listener = websocket?.addListener((message: any) => { // TODO create websocket message types
            const handler = eventMap[message.eventType];
            if (handler) { handler(message) }
        })

        return () => {
            websocket?.removeListener(listener);
        }
    }, [id, websocket, handleIssueNav])

    // show accesspoint export message on bindings state
    useEffect(() => {
        const stateMap = {
            [SyncProgress.UploadPending]: ExportState.PENDING,
            [SyncProgress.UploadStarted]: ExportState.STARTED,
        }
        bindings.forEach(({ syncProgress }) => {
            const state = stateMap[syncProgress];
            if (state) {
                setExportState(state);
            }
        })
    }, [bindings])

    useEffect(() => {
        fetchViewSettings();
        if (detail.fetched && detail.data) {
            refreshValidation(id, revisionActive);
        }
    }, [id, detail]);

    // Handler defined above the useEffect that uses it to avoid a TDZ
    // ReferenceError when early returns below skip the original declaration
    // (useEffect registers with a closure that references this binding by name).
    const handleExportConfirm = useCallback(async (message: string, qId: number) => {
        const body = formatExportIssues(message, i18n("ap.push-to-ext.needConfirm.intro"), "WARNING", handleIssueNav);
        const confirmResult = await showConfirmDialog(
            body,
            i18n("ap.push-to-ext.needConfirm.title"),
            i18n("ap.push-to-ext.needConfirm.confirm"),
            i18n("global.action.cancel"),
        );
        await Api.accesspoints.accessPointExportForceOrNo(qId, confirmResult);
        // reset state so a later NEED_CONFIRM for the same entity re-triggers the effect
        setExportState(ExportState.COMPLETED);
        refreshDetail(id, true, false, revisionActive);
    }, [showConfirmDialog, refreshDetail, id, revisionActive, handleIssueNav]);

    // processing the need to confirm accesspoint export
    useEffect(() => {
        if (exportState === ExportState.NEED_CONFIRM) {
            handleExportConfirm(exportMessage, itemQueueId);
        }
    }, [exportState, handleExportConfirm, exportMessage, itemQueueId])

    const isStoreLoading = (stores: Array<BaseRefTableStore<unknown> | DetailStoreState<unknown>>) =>
        stores.some((store) => !store.fetched || store.isFetching)

    if (isStoreLoading([
        refTables.partTypes,
        refTables.recordTypes as any,
        refTables.apTypes,
        refTables.descItemTypes,
        detail,
        apViewSettings
    ])) {
        return (
            <div className={'detail-page-wrapper'}>
                <Loading />
            </div>
        );
    }

    // Show message when entity with specified id does not exist
    if (id == null || (id && (!detail.id || !detail.data))) {
        return <div className="detail-page-wrapper missing-entity">
            <div className="message-container">
                <div className="message">
                    <div className="message-icon">
                        <Icon glyph="fa-regular fa-times-circle-o" />
                    </div>
                    <div className="message-text">
                        {i18n("ap.detail.entityMissing")}
                    </div>
                </div>
            </div>
        </div>;
    }

    const handleSetPreferred = async ({ part, updatedPart }: RevisionPart) => {
        const nextPreferredPart = part ? part : updatedPart;
        if (nextPreferredPart?.id) {
            saveScrollPosition();
            part ? await setPreferred(id, nextPreferredPart.id, apVersion, revisionActive) : await setRevisionPreferred(id, nextPreferredPart.id, apVersion, revisionActive);
            restoreScrollPosition();
            refreshValidation(id, revisionActive);
        }
    };

    const handleDelete = async ({ part, updatedPart }: RevisionPart) => {
        const deletedPart = part ? part : updatedPart;
        const message = deletedPart?.value ? i18n("ap.detail.delete.confirm.value", deletedPart.value) : i18n("ap.detail.delete.confirm");
        const confirmResult = await showConfirmDialog(message);

        if (confirmResult) {
            if (deletedPart?.id) {
                saveScrollPosition();
                part ? await deletePart(id, deletedPart.id, apVersion, revisionActive) : await deleteRevisionPart(id, deletedPart.id, apVersion, revisionActive);
                restoreScrollPosition();
            }
            refreshValidation(id, revisionActive);
        }
    };

    const handleRevert = async ({ part, updatedPart }: RevisionPart) => {
        if (!part || !updatedPart) { throw "No part to update." }
        const confirmResult = await showConfirmDialog(i18n("ap.detail.revert.confirm"));

        if (confirmResult) {
            saveScrollPosition();
            await deleteRevisionPart(id, updatedPart.id, apVersion, revisionActive);
            restoreScrollPosition();
            refreshValidation(id, revisionActive);
        }
    }

    const saveScrollPosition = () => {
        scrollTop = containerRef.current?.scrollTop || undefined;
    }

    const restoreScrollPosition = () => {
        if (containerRef.current && scrollTop) {
            containerRef.current.scrollTop = scrollTop;
            scrollTop = undefined;
        }
    }

    const handleEdit = (part: RevisionPart) => {
        const partTypeId = part.part?.typeId ? part.part.typeId : part.updatedPart?.typeId;
        const partType = refTables.partTypes.itemsMap && partTypeId ? refTables.partTypes.itemsMap[partTypeId].code : null;

        saveScrollPosition();
        detail.data &&
            showPartEditModal(
                part.part,
                part.updatedPart,
                partType,
                id,
                apVersion,
                apTypeId,
                detail.data.ruleSetId,
                detail.data.scopeId,
                refTables,
                apViewSettings,
                !!detail.data.revStateApproval,
                () => restoreScrollPosition()
            );
        refreshValidation(id, revisionActive);
    };

    const handleAdd = (partType: RulPartTypeVO, parentPartId?: number, revParentPartId?: number) => {
        if (detail.data) {
            saveScrollPosition();
            showPartCreateModal(
                partType,
                id,
                apVersion,
                apTypeId,
                detail.data.scopeId,
                parentPartId,
                () => restoreScrollPosition(),
                revParentPartId,
                revisionActive,
            );
        }
        refreshValidation(id, revisionActive);
    };

    const allParts = sortPrefer(detail.data ? detail.data.parts : [], detail.data?.preferredPart);
    const allRevisionParts = detail.data?.revStateApproval && revisionActive ? getRevisionParts(allParts, detail.data.revParts) : getRevisionParts(allParts, []);
    const filteredRevisionParts = allRevisionParts.filter(({ part, updatedPart }) =>
        !part?.partParentId
        && !updatedPart?.partParentId
        && !part?.revPartParentId
        && !updatedPart?.revPartParentId);

    const getRelatedPartSections = (parentParts: RevisionPart[]) => {
        if (parentParts.length === 0) { return []; }
        const parentIds: number[] = [];
        const updatedParentIds: number[] = [];

        parentParts.forEach(({ part, updatedPart }) => {
            if (part) { parentIds.push(part.id) }
            if (updatedPart) { updatedParentIds.push(updatedPart.id) }
        })

        // console.log(allRevisionParts, parentParts, parentIds, updatedParentIds)

        return allRevisionParts
            .filter(value =>
                value.part?.partParentId && parentIds.includes(value.part?.partParentId)
                || value.part?.partParentId && updatedParentIds.includes(value.part?.partParentId)
                || value.updatedPart?.revPartParentId && parentIds.includes(value.updatedPart?.revPartParentId)
                || value.updatedPart?.partParentId && parentIds.includes(value.updatedPart?.partParentId)
                || value.updatedPart?.partParentId && updatedParentIds.includes(value.updatedPart?.partParentId)
                || value.updatedPart?.revPartParentId && updatedParentIds.includes(value.updatedPart?.revPartParentId)
            );
    };

    const bindingsMaps = createBindings(detail.data);
    const groupPartsByType = (data: RevisionPart[]) => {
        return data.reduce<Record<string, RevisionPart[]>>((accumulator, value) => {
            const typeId = value.part?.typeId || value.updatedPart?.typeId;
            if (typeId != undefined) {
                const currentValue = accumulator[typeId] || [];
                accumulator[typeId.toString()] = [...currentValue, value];
            }
            return accumulator;
        }, {});
    }

    const groupedRevisionParts = groupPartsByType(filteredRevisionParts);
    const validationResult = apValidation.isFetching ? undefined : apValidation.data;

    const getSectionValidationErrors = (parts: RevisionPart[] = []) => {
        const errors: PartValidationErrorsVO[] = [];
        parts.forEach(({ part, updatedPart }) => {
            const error = part && objectByProperty(validationResult?.partErrors, part.id, "id");
            const updatedError = updatedPart && objectByProperty(validationResult?.partErrors, updatedPart.id, "id");
            if (error) { errors.push(error) }
            if (updatedError) { errors.push(updatedError) }
        })
        return errors;
    };

    const canEdit = () => {
        const revState = detail.data?.revStateApproval;
        if (!revState) { return editMode; }
        if (revState === RevStateApproval.TO_APPROVE) { return false; }
        return editMode && revisionActive;
    }

    const sortedParts = detail.data && refTables.partTypes.items
        ? sortPart(refTables.partTypes.items, apViewSettings.data?.rules[detail.data.ruleSetId])
        : [];

    return (
        <div className={'detail-page-wrapper'} ref={containerRef}>
            {exportState !== "COMPLETED" && <WaitingOverlay>
                {
                    exportState === ExportState.PENDING
                        ? i18n("ap.push-to-ext.pending.message")
                        : i18n("ap.push-to-ext.started.message")
                }
            </WaitingOverlay>}
            <div key="1" className="layout-scroll">
                <DetailHeader
                    item={detail.data!}
                    id={detail.data!.id}
                    collapsed={collapsed}
                    globalCollapsed={localGlobalCollapsed}
                    onToggleCollapsed={() => setCollapsed(!collapsed)}
                    onToggleGlobalCollapsed={() => setLocalGlobalCollapsed(!localGlobalCollapsed)}
                    onToggleRevision={() => {
                        setRevisionActive(!revisionActive);
                        refreshValidation(id, !revisionActive);
                    }}
                    validationErrors={validationResult?.errors}
                    validationPartErrors={validationResult?.partErrors}
                    onInvalidateDetail={() => refreshDetail(detail.data!.id, true, true, revisionActive)}
                    onInvalidateValidation={() => refreshValidation(id, !revisionActive)}
                    onPushApToExt={onPushApToExt}
                    revisionActive={revisionActive}
                />
                {detail.data?.comment && <div>
                    <div className="detail-multi-selection">
                        <div className="detail-section-header" style={{ display: "flex" }}>{i18n('ap.state.title.comment')}</div>
                        <div className={`parts single-part`}>
                            <div className="part comment">
                                {detail.data.comment}
                            </div>
                        </div>
                    </div>
                </div>}
                {detail.data?.revComment && revisionActive && <div>
                    <div className="detail-multi-selection">
                        <div className="detail-section-header" style={{ display: "flex" }}>{i18n('ap.state.title.revComment')}</div>
                        <div className={`parts single-part`}>
                            <div className="part comment">
                                {detail.data.revComment}
                            </div>
                        </div>
                    </div>
                </div>}

                {allParts && (
                    <div key="part-sections">
                        {sortedParts.map((partType: RulPartTypeVO) => {
                            // const parts = groupedParts[partType.id] || [];
                            const revisionParts = groupedRevisionParts[partType.id] || [];

                            const onAddRelated = partType.childPartId
                                ? (parentPartId?: number, revParentPartId?: number) => {
                                    const childPartType = partType.childPartId ? objectByProperty(
                                        refTables.partTypes.items,
                                        partType.childPartId,
                                        "id"
                                    ) : null;
                                    if (childPartType !== null) {
                                        handleAdd(childPartType, parentPartId, revParentPartId);
                                    } else {
                                        console.error('childPartType ' + partType.childPartId + ' not found');
                                    }
                                }
                                : undefined;
                            const apViewSettingRule = apViewSettings.data!.rules[detail.data!.ruleSetId];
                            if (partType.code === "PT_BODY" && revisionParts.length === 1) {
                                return (
                                    <DetailBodySection
                                        key={partType.code}
                                        label={partType.name}
                                        editMode={canEdit()}
                                        part={revisionParts[0]}
                                        onEdit={handleEdit}
                                        bindings={bindingsMaps}
                                        onAdd={() => handleAdd(partType)}
                                        partValidationErrors={getSectionValidationErrors(revisionParts)}
                                        itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                        globalEntity={globalEntity}
                                        partType={partType}
                                        onDelete={handleDelete}
                                        onRevert={handleRevert}
                                        revision={detail.data ? !!detail.data.revStateApproval && revisionActive : false}
                                        select={select}
                                    />
                                );
                            }
                            return (
                                <DetailMultiSection
                                    key={partType.code}
                                    label={partType.name}
                                    singlePart={!partType.repeatable && revisionParts.length === 1}
                                    editMode={canEdit()}
                                    parts={revisionParts}
                                    relatedParts={getRelatedPartSections(revisionParts)}
                                    preferred={detail.data ? detail.data.preferredPart : undefined}
                                    newPreferred={detail.data && revisionActive ? detail.data.newPreferredPart : undefined}
                                    revPreferred={detail.data && revisionActive ? detail.data.revPreferredPart : undefined}
                                    revision={detail.data ? !!detail.data.revStateApproval && revisionActive : false}
                                    globalCollapsed={localGlobalCollapsed}
                                    onSetPreferred={handleSetPreferred}
                                    onEdit={handleEdit}
                                    onDelete={handleDelete}
                                    onRevert={handleRevert}
                                    bindings={bindingsMaps}
                                    onAdd={() => handleAdd(partType)}
                                    onAddRelated={onAddRelated}
                                    partValidationErrors={getSectionValidationErrors(revisionParts)}
                                    itemTypeSettings={apViewSettingRule?.itemTypes || []}
                                    globalEntity={globalEntity}
                                    partType={partType}
                                    select={select}
                                />
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<AppState, any, Action<string>>, { history, select }: RouteComponentProps & { select: boolean }) => ({
    showConfirmDialog: (
        message: React.ReactNode,
        title?: string,
        confirmLabel?: string,
        cancelLabel?: string,
    ) => dispatch(showConfirmDialog(message, title, confirmLabel, cancelLabel)),
    showPartEditModal: (
        part: ApPartVO | undefined,
        updatedPart: ApPartVO | undefined,
        partType: unknown,
        apId: number,
        apVersion: number,
        apTypeId: number,
        ruleSetId: number,
        scopeId: number,
        refTables: unknown,
        apViewSettings: DetailStoreState<ApViewSettings>,
        revision: boolean,
        onUpdateFinish: () => void = () => { },
    ) => dispatch(showPartEditModal(part, updatedPart, partType as any, apId, apVersion, apTypeId, ruleSetId, scopeId, history, refTables as any, apViewSettings, revision, onUpdateFinish, select)),
    showPartCreateModal: (
        partType: RulPartTypeVO,
        apId: number,
        apVersion: number,
        apTypeId: number,
        scopeId: number,
        parentPartId?: number,
        onUpdateFinish: () => void = () => { },
        revParentPartId?: number,
        revisionActive?: boolean,
    ) => dispatch(showPartCreateModal(partType, apId, apVersion, apTypeId, scopeId, history, select, parentPartId, onUpdateFinish, revParentPartId, revisionActive)),
    setPreferred: async (apId: number, partId: number, apVersion: number, revisionActive: boolean = false) => {
        await Api.accesspoints.accessPointSetPreferName(apId, partId, apVersion);
        return dispatch(goToAe(history, apId, true, !select, revisionActive));
    },
    setRevisionPreferred: async (apId: number, partId: number, apVersion: number, revisionActive: boolean = false) => {
        await Api.accesspoints.accessPointSetPreferNameRevision(apId, partId, apVersion);
        return dispatch(goToAe(history, apId, true, !select, revisionActive));
    },
    deletePart: async (apId: number, partId: number, apVersion: number, revisionActive: boolean = false) => {
        await Api.accesspoints.accessPointDeletePart(apId, partId, apVersion);
        return dispatch(goToAe(history, apId, true, !select, revisionActive));
    },
    deleteRevisionPart: async (apId: number, partId: number, apVersion: number, revisionActive: boolean = false) => {
        await Api.accesspoints.accessPointDeleteRevisionPart(apId, partId, apVersion);
        return dispatch(goToAe(history, apId, true, !select, revisionActive));
    },
    deleteParts: async (apId: number, parts: ApPartVO[], revisionActive: boolean = false) => {
        for (let part of parts) {
            if (part.id) {
                await Api.accesspoints.accessPointDeletePart(apId, part.id);
            }
        }
        dispatch(goToAe(history, apId, true, !select, revisionActive));
    },
    updateRevisionPart: async (apId: number, part: ApPartVO, typeCode: string, apVersion: number) => {
        await WebApi.updateRevisionPart(apId, part.id, {
            parentPartId: part.partParentId,
            partId: part.id,
            items: part.items?.filter((item) => item) || [],
            partTypeCode: typeCode,
        }, apVersion)
    },
    refreshValidation: (apId: number, includeRevision?: boolean) => {
        dispatch(DetailActions.fetchIfNeeded(
            AP_VALIDATION,
            apId,
            async (id: number) => {
                const { data } = await Api.accesspoints.accessPointValidateAccessPoint(id, includeRevision);
                return data;
            },
            true
        ));
    },
    refreshDetail: (apId: number, force: boolean = true, redirect: boolean = true, revisionActive: boolean = false) => {
        dispatch(goToAe(history, apId, force, redirect, revisionActive, true));
    },
    fetchViewSettings: () => {
        dispatch(
            DetailActions.fetchIfNeeded(AP_VIEW_SETTINGS, '', () => {
                return WebApi.getApTypeViewSettings();
            }),
        );
    },
});

const mapStateToProps = (state: AppState) => {
    return {
        detail: storeFromArea(state, registry.AREA_REGISTRY_DETAIL) as DetailStoreState<ApAccessPointVO>,
        apValidation: storeFromArea(state, AP_VALIDATION) as DetailStoreState<ApValidationErrorsVO>,
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
        descItemTypesMap: state.refTables.descItemTypes.itemsMap,
        refTables: state.refTables,
    };
};

export default withRouter(connect<any, any, RouteComponentProps>(mapStateToProps, mapDispatchToProps)(ApDetailPageWrapper));
