import React, {FC} from 'react';
import DetailState from './DetailState';
import {Col, Dropdown, DropdownButton, Row} from 'react-bootstrap';
import DetailDescriptions from './DetailDescriptions';
import DetailDescriptionsItem from './DetailDescriptionsItem';
import {connect} from 'react-redux';
import ArchiveEntityName from './ArchiveEntityName';
import {ApValidationErrorsVO} from '../../../api/ApValidationErrorsVO';
import {ApAccessPointVO} from '../../../api/ApAccessPointVO';
import './DetailHeader.scss';
import {Icon} from '../../index';
import {Button} from '../../ui';
import {indexById, objectById} from '../../../shared/utils';
import i18n from '../../i18n';
import {SyncState} from '../../../api/SyncState';
import {ApBindingVO} from '../../../api/ApBindingVO';
import {showAsyncWaiting} from '../../../actions/global/modalDialog';
import {WebApi} from '../../../actions/WebApi';
import ValidationResultIcon from '../../ValidationResultIcon';
import * as perms from '../../../actions/user/Permission';

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

    const renderApTypeNames = (delimiter = '>') => {
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
        if (item.externalIds.length > 0 && externalSystems.length > 0) {
            const apExternalWr = userDetail.hasOne(perms.AP_EXTERNAL_WR);
            return (
                <div className="bindings" key="bindings">
                    {item.externalIds.map(binding => {
                        const externalSystem = objectById(externalSystems, binding.externalSystemCode, 'code');
                        return (
                            <div className="binding" key={'binding-' + binding.id}>
                                <div className="info">
                                    <span className="system">{externalSystem.name}</span>-{' '}
                                    {i18n('ap.binding.external-id')}:{' '}
                                    <span title={binding.extRevision} className="link">
                                        <a href={binding.detailUrl} target="_blank" rel="noopener noreferrer">
                                            {binding.value}
                                        </a>
                                    </span>
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
                                    , {i18n('ap.binding.user')}: <span className="user">{binding.extUser}</span>
                                </div>
                                <div className="action pl-3" key={'binding-action-' + binding.id}>
                                    <Icon
                                        glyph="fa-refresh"
                                        title={i18n('ap.binding.syncState.' + binding.syncState)}
                                        className={binding.syncState == SyncState.NOT_SYNCED ? 'disabled' : ''}
                                    />
                                    <DropdownButton
                                        className="d-inline-block ml-3 fixheight"
                                        variant="action"
                                        id={'binding-action-' + binding.id}
                                        title={((<Icon glyph="fa-ellipsis-v" />) as any) as string}
                                    >
                                        <Dropdown.Item key="synchronize" onClick={() => handleSynchronize(binding)}>
                                            {i18n('ap.binding.action.synchronize')}
                                        </Dropdown.Item>
                                        {apExternalWr && (
                                            <Dropdown.Item key="update" onClick={() => handleUpdate(binding)}>
                                                {i18n('ap.binding.action.update')}
                                            </Dropdown.Item>
                                        )}
                                        <Dropdown.Item key="disconnect" onClick={() => handleDisconnect(binding)}>
                                            {i18n('ap.binding.action.disconnect')}
                                        </Dropdown.Item>
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

    return (
        <div className={'detail-header-wrapper'}>
            <Row className={collapsed ? 'ml-3 mt-1 mr-3 pb-1' : 'ml-3 mt-3 mr-3 pb-3 space-between middle'}>
                {collapsed && (
                    <Col className={'p-0'} style={{flex: 1}}>
                        <h4 className={'m-0'}>
                            <ArchiveEntityName name={item.name} description={item.description} />{' '}
                            {showValidationError()}
                        </h4>
                    </Col>
                )}

                {!collapsed && (
                    <Col className={'p-0'} style={{flex: 1}}>
                        <div className={'d-inline-block mr-3 mt-3 pull-left'}>
                            <Icon glyph={'fa-file-o'} className={'fa-4x'} />
                        </div>
                        <div className={'d-inline-block'}>
                            <h1 className={'m-0'}>
                                <ArchiveEntityName name={item.name} description={item.description} />{' '}
                                {showValidationError()}
                            </h1>
                            <h4>{apType.name}</h4>
                            <DetailDescriptions>
                                {id && <DetailDescriptionsItem label="ID:">{id}</DetailDescriptionsItem>}
                                {item.state && (
                                    <DetailDescriptionsItem>
                                        <DetailState state={item.stateApproval} />
                                    </DetailDescriptionsItem>
                                )}
                                {item.scopeId && scopes[item.scopeId] && (
                                    <DetailDescriptionsItem>
                                        <Icon glyph={'fa-globe'} className={'mr-1'} />
                                        {scopes[item.scopeId].name}
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
                    </Col>
                )}
                <Col>
                    <Button
                        onClick={onToggleCollapsed}
                        variant={'light'}
                        style={{position: 'absolute', right: 0, bottom: 0}}
                        title={collapsed ? 'Zobrazit podrobnosti' : 'Skrýt podrobnosti'}
                    >
                        <Icon className={''} glyph={collapsed ? 'fa-angle-double-down' : 'fa-angle-double-up'} />
                    </Button>
                </Col>
            </Row>
            <Row>
                <Col className={'ap-type'}>
                    <div className={'p-1 pl-3'}>{renderApTypeNames()}</div>
                </Col>
            </Row>
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
