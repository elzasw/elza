import React, { FC } from 'react';
import { Dropdown, DropdownButton } from 'react-bootstrap';
import { connect } from 'react-redux';
import { showAsyncWaiting } from '../../../../actions/global/modalDialog';
import * as perms from '../../../../actions/user/Permission';
import { WebApi } from '../../../../actions/WebApi';
import { ApAccessPointVO } from '../../../../api/ApAccessPointVO';
import { ApBindingVO } from '../../../../api/ApBindingVO';
import { SyncState } from '../../../../api/SyncState';
import { indexById, objectById } from '../../../../shared/utils';
import { FundScope } from '../../../../types';
import i18n from '../../../i18n';
import { Icon } from '../../../index';
import { Button } from '../../../ui';
import ValidationResultIcon from '../../../ValidationResultIcon';
import DetailDescriptions from './DetailDescriptions';
import DetailDescriptionsItem from './DetailDescriptionsItem';
import './DetailHeader.scss';
import DetailState from './DetailState';
import { SyncIcon } from "../sync-icon";

interface Props extends ReturnType<typeof mapStateToProps> {
    item: ApAccessPointVO;
    onToggleCollapsed?: () => void;
    onInvalidateDetail?: () => void;
    collapsed: boolean;
    id?: number;
    validationErrors?: string[];
    apTypesMap: object;
    scopes: any;
    externalSystems: any[];
    dispatch: any;
}

const formatDate = (a: any, ...other) => a;
const formatDateTime = (a: any, ...other) => a;

const getProcessingMessage = (key: string) => {
    return <h4 className="processing">{i18n(key)}</h4>;
};

const DetailHeader: FC<Props> = ({
    dispatch,
    onInvalidateDetail,
    item,
    id,
    collapsed,
    onToggleCollapsed,
    validationErrors,
    apTypesMap,
    scopes,
    externalSystems,
    userDetail,
}) => {
    const apType = apTypesMap[item.typeId];

    const showValidationError = () => {
        if (validationErrors && validationErrors.length > 0) {
            return <ValidationResultIcon message={validationErrors} />;
        }
    };

    const renderApTypeNames = (delimiter: React.ReactNode = '>') => {
        let elements: JSX.Element[] = [];

        if (apType.parents) {
            apType.parents.reverse().forEach((name, i) => {
                elements.push(
                    <span key={'name-' + i} className="hierarchy-level">
                        {name.toUpperCase()}
                    </span>,
                );
                elements.push(
                    <span key={'delimiter-' + i} className="hierarchy-delimiter">
                        {delimiter}
                    </span>,
                );
            });
        }
        elements.push(
            <span key="name-main" className="hierarchy-level main">
                {apType.name.toUpperCase()}
            </span>,
        );

        return elements;
    };

    const handleSynchronize = (binding: ApBindingVO) => {
        dispatch(
            showAsyncWaiting(
                null,
                getProcessingMessage('ap.binding.processing.synchronize'),
                WebApi.synchronizeAccessPoint(id!, binding.externalSystemCode),
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
                WebApi.updateArchiveEntity(id!, binding.externalSystemCode),
                () => {
                    onInvalidateDetail && onInvalidateDetail();
                },
            ),
        );
    };

    const handleDisconnect = (binding: ApBindingVO) => {
        dispatch(
            showAsyncWaiting(
                null,
                getProcessingMessage('ap.binding.processing.disconnect'),
                WebApi.disconnectAccessPoint(id!, binding.externalSystemCode),
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
                WebApi.takeRelArchiveEntities(id!, binding.externalSystemCode),
                () => {
                    onInvalidateDetail && onInvalidateDetail();
                },
            ),
        );
    };

    const renderBindings = () => {
        if (item.bindings.length > 0 && externalSystems.length > 0) {
            const apExternalWr = userDetail.hasOne(perms.AP_EXTERNAL_WR);
            return (
                <div className="bindings" key="bindings">
                    {item.bindings.map(binding => {
                        const externalSystem = objectById(externalSystems, binding.externalSystemCode, 'code');
                        const tooltip = ('id: '+binding.value)+(binding.extRevision?(', uuid: '+binding.extRevision):'')
                                      + (binding.extUser?(', '+i18n('ap.binding.user')+': '+binding.extUser):'');
                        return (
                            <div className="binding" key={'binding-' + binding.id}>
                                <div className="info" title={tooltip}>
                                    {i18n('ap.binding.source')}{': '}
                                    <span className="system">{externalSystem.name}</span>
                                    {/*
                                    <span className="link">
                                        <a href={binding.detailUrl} target="_blank" rel="noopener noreferrer">
                                            {binding.value}
                                        </a>
                                    </span>*/}
                                    , {i18n('ap.binding.extState.' + binding.extState)}
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
                                <div className="action">
                                    <SyncIcon syncState={binding.syncState || undefined}/>
                                </div>
                                <div className="action">
                                    <DropdownButton
                                        variant="action"
                                        id={'binding-action-' + binding.id}
                                        title={((<Icon glyph="fa-ellipsis-h" />) as any) as string}
                                    >
                                        <Dropdown.Item key="synchronize" onClick={() => handleSynchronize(binding)}>
                                            {i18n('ap.binding.action.synchronize')}
                                        </Dropdown.Item>
                                        {apExternalWr && (
                                            <Dropdown.Item key="update" onClick={() => handleUpdate(binding)}>
                                                {i18n('ap.binding.action.update')}
                                            </Dropdown.Item>
                                        )}
                                        {
                                            // Vypnutí možnosti odpojení entity
                                            // TODO: Odstranit relevantní kód
                                        /*
                                        <Dropdown.Item key="disconnect" onClick={() => handleDisconnect(binding)}>
                                            {i18n('ap.binding.action.disconnect')}
                                        </Dropdown.Item>
                                        */
                                        }
                                        <Dropdown.Item
                                            key="take-rel-entities"
                                            onClick={() => handleTakeRelEntities(binding)}
                                        >
                                            {i18n('ap.binding.action.take-rel-entities')}
                                        </Dropdown.Item>
                                    </DropdownButton>
                                </div>
                            </div>
                        );
                    })}
                </div>
            );
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
                            </div>
                            {item.description &&
                                <div className="description">
                                    {item.description}
                                </div>
                            }
                        </div>
                        <div style={{padding: "5px"}}>
                            <DetailDescriptions>
                                {id && <DetailDescriptionsItem label="ID:">{id}</DetailDescriptionsItem>}
                                {item.stateApproval && (
                                    <DetailDescriptionsItem>
                                        <DetailState state={item.stateApproval} />
                                    </DetailDescriptionsItem>
                                )}
                                {scope && (
                                    <DetailDescriptionsItem>
                                        <Icon glyph={'fa-globe'} className={'mr-1'} />
                                        {scope.name}
                                    </DetailDescriptionsItem>
                                )}
                                {item.lastChange && item.lastChange.user && (
                                    <DetailDescriptionsItem label="Upravil:">
                                        {item.lastChange.user.displayName}
                                        <span title={'Upraveno ' + formatDateTime(item.lastChange.change, {})}>
                                            ({formatDate(item.lastChange.change)})
                                        </span>
                                    </DetailDescriptionsItem>
                                )}
                            </DetailDescriptions>
                            {renderBindings()}
                        </div>
                    </div>
                )}
                <div>
                    <Button
                        onClick={onToggleCollapsed}
                        variant={'light'}
                        className="collapse-button"
                        title={collapsed ? 'Zobrazit podrobnosti' : 'Skrýt podrobnosti'}
                    >
                        <Icon glyph={collapsed ? 'fa-angle-double-down' : 'fa-angle-double-up'} />
                    </Button>
                </div>
            </div>
            <div className="ap-type">
                    {renderApTypeNames(<Icon glyph="fa-angle-right"/>)}
            </div>
        </div>
    );
};

const mapStateToProps = state => {
    const scopesData = state.refTables.scopesData;
    const id = scopesData && indexById(scopesData.scopes, -1, 'versionId'); // všechny scope
    let scopes = [];
    if (id !== null) {
        scopes = scopesData.scopes[id].scopes;
    }

    return {
        externalSystems: state.app.apExtSystemList.rows,
        apTypesMap: state.refTables.recordTypes.typeIdMap,
        scopes: scopes,
        userDetail: state.userDetail,
    };
};

export default connect(mapStateToProps)(DetailHeader);
