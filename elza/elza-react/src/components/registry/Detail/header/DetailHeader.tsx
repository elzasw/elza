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

interface Props {
    item: ApAccessPointVO;
    onToggleCollapsed?: () => void;
    onInvalidateDetail?: () => void;
    onToggleRevision?: () => void;
    collapsed: boolean;
    id?: number;
    validationErrors?: string[];
    revisionActive?: boolean;
}

const formatDateTime = (dateString: string) => new Date(dateString).toLocaleString();
const getItemState = (item: ApAccessPointVO) => {
    if(item.replacedById != undefined) {return StateApprovalEx.REPLACED}
    if(item.invalid) {return StateApprovalEx.INVALID}
    return item.stateApproval
}

const DetailHeader: FC<Props> = ({
    onInvalidateDetail,
    item,
    id,
    collapsed,
    onToggleCollapsed,
    onToggleRevision,
    validationErrors,
    revisionActive,
}) => {
    const scopes = useSelector(({ refTables: { scopesData } }:AppState) =>
        scopesData.scopes.find((scope) => scope.versionId === -1)?.scopes || []) // všechny scope
    const apTypesMap = useSelector(({refTables}:AppState) => refTables.recordTypes.itemsMap);
    const apType = apTypesMap[item.typeId] as any;
    const apTypeNew = apTypesMap[item.newTypeId] as any;


    const showValidationError = () => {
        if (validationErrors && validationErrors.length > 0) {
            return <ValidationResultIcon message={validationErrors} />;
        }
    };


    let scope: FundScope | null = null;
    if (item.scopeId) {
        scope = objectById(scopes, item.scopeId);
    }

    const itemState = getItemState(item);

    return (
        <div className={'detail-header-wrapper'}>
            <div className='header-container'>
                {collapsed && (
                    <div className="header collapsed">
                        <h4 className="name">
                            <Icon glyph={'fa-file-o'}/>
                            <span className="text">{item.name}</span>
                            {showValidationError()}
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
                                <h1 style={{margin: 0}}>
                                    <Icon glyph={'fa-file-o'}/>
                                    <span className="text">{item.name}</span>
                                    {showValidationError()}
                                </h1>
                                {item.replacedById != undefined && 
                                    <div style={{fontSize: "1rem"}}>
                                        <DescriptionEntityRef entityId={item.replacedById}/>
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
                                    style={{width: "auto"}}
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
                                    style={{width: "auto"}}
                                    content={ item.lastChange ?
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
                            {scope && (
                                <DetailDescriptionsItem>
                                    <Icon glyph={'fa-globe'} className={'mr-1'} />
                                    {scope.name}
                                </DetailDescriptionsItem>
                            )}
                            <EntityBindings item={item} onInvalidateDetail={onInvalidateDetail}/>
                            {item.replacedIds && <ReplacedEntities ids={item.replacedIds}/>}
                            <div style={{flex: 1}}/>
                            {item.revStateApproval && (
                                <DetailDescriptionsItemWithButton
                                    renderButton={onToggleRevision ? () => 
                                        <Button onClick={onToggleRevision}>
                                            <Icon glyph={'fa-pencil'}/>
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
            <RevisionApTypeNames className={"test"} apType={apType} apTypeNew={revisionActive ? apTypeNew : undefined}/>
        </div>
    );
};

export default DetailHeader;
