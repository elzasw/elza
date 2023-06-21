import { StateApprovalEx } from 'api/StateApproval';
import i18n from "components/i18n";
import { TooltipTrigger } from 'components/shared';
import React, { FC } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { ApAccessPointVO } from '../../../../api/ApAccessPointVO';
import { objectById } from '../../../../shared/utils';
import { FundScope } from '../../../../types';
import { Icon } from '../../../index';
import { Button } from '../../../ui';
import ValidationResultIcon from '../../../ValidationResultIcon';
import { RevisionApTypeNames } from './ApTypeNames';
import { DescriptionEntityRef } from "./DescriptionEntityRef";
import DetailDescriptions from './DetailDescriptions';
import DetailDescriptionsItem, { DetailDescriptionsItemWithButton } from './DetailDescriptionsItem';
import './DetailHeader.scss';
import DetailRevState from "./DetailRevState";
import DetailState from './DetailState';
import { EntityBindings } from './EntityBindings';
import { ReplacedEntities } from './ReplacedEntities';
import { PartValidationErrorsVO } from 'api/PartValidationErrorsVO';
import { Api } from 'api';

interface Props {
    item: ApAccessPointVO;
    onToggleCollapsed?: () => void;
    onInvalidateDetail?: () => void;
    onInvalidateValidation?: () => void;
    onPushApToExt?: (item: ApAccessPointVO) => void;
    onToggleRevision?: () => void;
    collapsed: boolean;
    id?: number;
    validationErrors?: string[];
    validationPartErrors?: PartValidationErrorsVO[]
    revisionActive?: boolean;
}

const formatDateTime = (dateString: string) => new Date(dateString).toLocaleString();
const getItemState = (item: ApAccessPointVO) => {
    if (item.replacedById != undefined) { return StateApprovalEx.REPLACED }
    if (item.invalid) { return StateApprovalEx.INVALID }
    return item.stateApproval
}

const DetailHeader: FC<Props> = ({
    onInvalidateDetail,
    onInvalidateValidation,
    item,
    id,
    collapsed,
    onPushApToExt,
    onToggleCollapsed,
    onToggleRevision,
    validationErrors,
    validationPartErrors,
    revisionActive,
}) => {
    const scopes = useSelector(({ refTables: { scopesData } }: AppState) =>
        scopesData.scopes.find((scope) => scope.versionId === -1)?.scopes || []) // všechny scope
    const apTypesMap = useSelector(({ refTables }: AppState) => refTables.recordTypes.itemsMap);
    const apType = apTypesMap[item.typeId] as any;
    const apTypeNew = apTypesMap[item.newTypeId] as any;
    const itemState = getItemState(item);
    const errorsFetched = validationErrors && validationPartErrors;
    const hasErrors = (validationErrors && validationErrors.length > 0) || (validationPartErrors && validationPartErrors.length > 0);

    const renderValidationIcon = () => {
        if (!validationErrors || !validationPartErrors) {
            return <Icon glyph="fa-spinner fa-spin" />
        } else if (validationErrors.length > 0 || validationPartErrors.length > 0) {
            return <Icon glyph="fa-exclamation-triangle" />
        } else {
            return <Icon glyph="fa-check-circle" />
        }
    };

    let scope: FundScope | null = null;
    if (item.scopeId) {
        scope = objectById(scopes, item.scopeId);
    }


    return (
        <div className={'detail-header-wrapper'}>
            <div className='header-container'>
                {collapsed && (
                    <div className="header collapsed">
                        <h4 className="name">
                            <Icon glyph={'fa-file-o'} />
                            <span className="text">{item.name}</span>
                        </h4>
                        {item.description &&
                            <div title={item.description} className="description">
                                {item.description}
                            </div>
                        }
                    </div>
                )}

                {!collapsed && (
                    <div className="header expanded">
                        <div>
                            <div className="name">
                                <h1 style={{ margin: 0 }}>
                                    <Icon glyph={'fa-file-o'} />
                                    <span className="text">{item.name}</span>
                                </h1>
                                {item.replacedById != undefined &&
                                    <div style={{ fontSize: "1rem" }}>
                                        <DescriptionEntityRef entityId={item.replacedById} />
                                    </div>
                                }
                            </div>
                            {item.description &&
                                <div className="description">
                                    {item.description}
                                </div>
                            }
                        </div>
                        <DetailDescriptions>
                            {id &&
                                <TooltipTrigger
                                    style={{ width: "auto" }}
                                    content={
                                        <>
                                            <div>id: {id}</div>
                                            <div>uuid: {item.uuid}</div>
                                        </>
                                    }
                                >
                                    <DetailDescriptionsItem>
                                        {`id: ${id}`}
                                    </DetailDescriptionsItem>
                                </TooltipTrigger>
                            }
                            {itemState && (
                                <TooltipTrigger
                                    style={{ width: "auto" }}
                                    content={item.lastChange ?
                                        <>
                                            <div>{i18n("ap.detail.lastChange")}: {formatDateTime(item.lastChange.change)}</div>
                                            <div>{i18n("ap.detail.modifiedBy")}: {item.lastChange.user?.displayName || i18n("ap.detail.lastChange.user.notAvailable")}</div>
                                        </>
                                        : <div>{i18n("ap.detail.lastChange.notAvailable")}</div>
                                    }
                                >
                                    <DetailDescriptionsItem className={itemState.toLowerCase()}>
                                        <DetailState state={itemState} />
                                    </DetailDescriptionsItem>
                                </TooltipTrigger>
                            )}
                            <TooltipTrigger style={{ width: "auto" }} content={<div>
                                {hasErrors
                                    ?
                                    <div>
                                        <div>
                                            <b>{i18n('arr.node.status.err.errors')}</b>
                                        </div>
                                        <div>
                                            {validationErrors?.map((error) => { return <div> {error}</div> })}
                                        </div>
                                        <div>
                                            {validationPartErrors?.map((partErrors) => <div>
                                                {partErrors.errors?.map((error) => <div> {error}</div>)}
                                            </div>)}
                                        </div>
                                    </div>
                                    : errorsFetched ? i18n('arr.node.status.ok') : i18n('global.validation.loading')}
                                {!revisionActive && errorsFetched && <div>
                                    <button className="tooltip-link" onClick={async () => {
                                        if (id != undefined) {
                                            await Api.accesspoints.validateAccessPoint(id.toString())
                                            if (onInvalidateValidation) onInvalidateValidation();
                                        }
                                    }}>{i18n('global.validation.run')}</button>
                                </div>}

                            </div>
                            }>
                                <DetailDescriptionsItem className={hasErrors ? "invalid" : undefined}>
                                    <div>
                                        {renderValidationIcon()}
                                    </div>
                                </DetailDescriptionsItem>
                            </TooltipTrigger>

                            {scope && (
                                <DetailDescriptionsItem>
                                    <Icon glyph={'fa-globe'} className={'mr-1'} />
                                    {scope.name}
                                </DetailDescriptionsItem>
                            )}
                            <EntityBindings item={item} onInvalidateDetail={onInvalidateDetail} onPushApToExt={onPushApToExt} />
                            {item.replacedIds && <ReplacedEntities ids={item.replacedIds} />}
                            <div style={{ flex: 1 }} />
                            {item.revStateApproval && (
                                <DetailDescriptionsItemWithButton
                                    renderButton={onToggleRevision ? () =>
                                        <Button onClick={onToggleRevision}>
                                            <Icon glyph={'fa-pencil'} />
                                        </Button> : undefined
                                    }
                                    className={revisionActive ? "revision" : undefined}
                                >
                                    <DetailRevState state={item.revStateApproval} />
                                </DetailDescriptionsItemWithButton>
                            )}
                        </DetailDescriptions>
                    </div>
                )}
                <Button
                    onClick={onToggleCollapsed}
                    variant={'light'}
                    className="collapse-button"
                    title={collapsed ? 'Zobrazit podrobnosti' : 'Skrýt podrobnosti'}
                >
                    <Icon glyph={collapsed ? 'fa-angle-double-down' : 'fa-angle-double-up'} />
                </Button>
                <div>
                </div>
            </div>
            <RevisionApTypeNames className={"test"} apType={apType} apTypeNew={revisionActive ? apTypeNew : undefined} />
        </div>
    );
};

export default DetailHeader;
