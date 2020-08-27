import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon, RibbonGroup, Utils} from '../../components/shared';
import {DetailActions} from '../../shared/detail';
import Ribbon from '../../components/page/Ribbon';
import ImportForm from '../../components/form/ImportForm';
import RegistryList from '../../components/registry/RegistryList';
import {Button} from '../../components/ui';
import {registryDelete, registryDetailFetchIfNeeded, registryListInvalidate} from '../../actions/registry/registry.jsx';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog.jsx';
import {refRecordTypesFetchIfNeeded} from '../../actions/refTables/recordTypes.jsx';
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor, setFocus} from '../../actions/global/focus.jsx';
import * as perms from '../../actions/user/Permission.jsx';
import {apExtSystemListFetchIfNeeded} from '../../actions/registry/apExtSystemList';
import {PropTypes} from 'prop-types';
import './RegistryPage.scss';
import PageLayout from '../shared/layout/PageLayout';
import defaultKeymap from './RegistryPageKeymap.jsx';
import {AP_VIEW_SETTINGS, FOCUS_KEYS} from '../../constants.tsx';
import * as eidTypes from '../../actions/refTables/eidTypes';
import ScopeLists from '../../components/arr/ScopeLists';
import ApStateHistoryForm from '../../components/registry/ApStateHistoryForm';
import ApStateChangeForm from '../../components/registry/ApStateChangeForm';
import {WebApi} from '../../actions';
import ApDetailPageWrapper from '../../components/registry/ApDetailPageWrapper';
import {refApTypesFetchIfNeeded} from '../../actions/refTables/apTypes';
import {refPartTypesFetchIfNeeded} from '../../actions/refTables/partTypes';
import {descItemTypesFetchIfNeeded} from '../../actions/refTables/descItemTypes';
import {refRulDataTypesFetchIfNeeded} from '../../actions/refTables/rulDataTypes';
import CreateAccessPointModal from '../../components/registry/modal/CreateAccessPointModal';
import ApExtSearchModal, {TypeModal} from '../../components/registry/modal/ApExtSearchModal';
import {Area} from '../../api/Area';
import ApPushToExt from '../../components/registry/modal/ApPushToExt';
import ExtSyncsModal from '../../components/registry/modal/ExtSyncsModal';
import {objectById} from '../../shared/utils';

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistryPage extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    static propTypes = {
        splitter: PropTypes.object.isRequired,
        refTables: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
    };

    state = {items: []};

    componentDidMount() {
        this.initData();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.initData(nextProps);
    }

    canDeleteRegistry = () => {
        // We can delete item if has id and data
        const {id, data} = this.props.registryDetail;

        return id && data;
    };

    initData = (props = this.props) => {
        const {dispatch} = this.props;

        //todo: prevest na apTypes
        dispatch(refRecordTypesFetchIfNeeded());

        dispatch(refApTypesFetchIfNeeded());
        dispatch(eidTypes.fetchIfNeeded());
        dispatch(refPartTypesFetchIfNeeded());
        dispatch(descItemTypesFetchIfNeeded());
        dispatch(refRulDataTypesFetchIfNeeded());
        dispatch(DetailActions.fetchIfNeeded(AP_VIEW_SETTINGS, '', WebApi.getApTypeViewSettings));

        if (props.userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            dispatch(apExtSystemListFetchIfNeeded());
        }

        this.trySetFocus(props);
    };

    trySetFocus = props => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {
                // focus po ztrátě
                if (this.refs.registryList) {
                    // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.registryList.focus();
                        focusWasSet();
                    });
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1) || isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1, 'list')) {
                this.setState({}, () => {
                    this.refs.registryList.focus();
                    focusWasSet();
                });
            }
        }
    };

    handleShortcuts = action => {
        console.log('#handleShortcuts', '[' + action + ']', this);
        switch (action) {
            case 'addRegistry':
                this.handleAddRegistry();
                break;
            case 'area1':
                this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 1));
                break;
            case 'area2':
                this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 2));
                break;
            default:
                break;
        }
    };

    handleAddRegistry = () => {
        const {
            registryList: {
                filter: {versionId},
                parents,
            },
            dispatch,
        } = this.props;

        dispatch(
            modalDialogShow(
                this,
                i18n('registry.addRegistry'),
                <CreateAccessPointModal
                    initialValues={{}}
                    onSubmit={formData => {
                        if (!formData.partForm) {
                            return Promise.reject("");
                        }
                        const data = {
                            ...formData,
                            partForm: {
                                ...formData.partForm,
                                items: formData.partForm.items.filter(i => i.value != null)
                            }
                        }
                        const submitData = {
                            partForm: data.partForm,
                            accessPointId: null,
                            languageCode: null,
                            scopeId: data.scopeId,
                            typeId: data.apType.id,
                        };
                        return WebApi.createAccessPoint(submitData);
                    }}
                    onSubmitSuccess={data => {
                        dispatch(modalDialogHide());
                        this.props.dispatch(registryDetailFetchIfNeeded(data.id));
                        this.props.dispatch(registryListInvalidate());
                    }}
                />,
                'dialog-lg',
                () => {},
            ),
        );
    };

    handleDeleteRegistry = () => {
        if (window.confirm(i18n('registry.deleteRegistryQuestion'))) {
            const {
                registryDetail: {
                    data: {id},
                },
            } = this.props;
            this.props.dispatch(registryDelete(id));
        }
    };

    handleRegistryImport = () => {
        this.props.dispatch(modalDialogShow(this, i18n('import.title.registry'), <ImportForm record />));
    };

    handleApExtSearch = () => {
        const {extSystems} = this.props;
        const initialValues = {
            area: Area.ALLNAMES,
            onlyMainPart: 'false', // musí být jako string, autocomplete má problém s true/false hodnotou
        };
        if (extSystems.length === 1) {
            initialValues.extSystem = extSystems[0].code;
        }
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('ap.ext-search.title'),
                <ApExtSearchModal itemType={TypeModal.SEARCH} initialValues={initialValues} extSystems={extSystems} />,
                'dialog-xl',
            ),
        );
    };

    handleExtSyncs = () => {
        const {extSystems, dispatch} = this.props;
        const initialValues = {};
        if (extSystems.length === 1) {
            initialValues.extSystem = extSystems[0].code;
        }
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('ap.ext-syncs.title'),
                <ExtSyncsModal
                    onNavigateAp={accessPointId => {
                        dispatch(modalDialogHide());
                        dispatch(registryDetailFetchIfNeeded(accessPointId, true));
                    }}
                    initialValues={initialValues}
                    extSystems={extSystems}
                />,
                'dialog-xl',
            ),
        );
    };

    handleConnectAp = () => {
        const {
            extSystems,
            registryDetail: {data},
            dispatch,
        } = this.props;
        const id = data.id;
        const initialValues = {
            area: Area.ALLNAMES,
            onlyMainPart: 'false', // musí být jako string, autocomplete má problém s true/false hodnotou
        };

        const filteredExtSystems = extSystems.filter(extSystem => {
            const found = objectById(data.externalIds, extSystem.code, 'externalSystemCode');
            return found === null;
        });

        if (filteredExtSystems.length === 1) {
            initialValues.extSystem = filteredExtSystems[0].code;
        }
        dispatch(
            modalDialogShow(
                this,
                i18n('ap.ext-search.title-connect'),
                <ApExtSearchModal
                    onConnected={() => {
                        dispatch(registryDetailFetchIfNeeded(id, true));
                    }}
                    itemType={TypeModal.CONNECT}
                    accessPointId={id}
                    initialValues={initialValues}
                    extSystems={filteredExtSystems}
                />,
                'dialog-xl',
            ),
        );
    };

    handlePushApToExt = () => {
        const {
            extSystems,
            registryDetail: {data},
            dispatch,
        } = this.props;
        const id = data.id;
        const initialValues = {};
        if (extSystems.length === 1) {
            initialValues.extSystem = extSystems[0].code;
        }

        const filteredExtSystems = extSystems.filter(extSystem => {
            const found = objectById(data.externalIds, extSystem.code, 'externalSystemCode');
            return found === null;
        });

        dispatch(
            modalDialogShow(
                this,
                i18n('ap.push-to-ext.title'),
                <ApPushToExt
                    onSubmit={data => {
                        return WebApi.saveAccessPoint(id, data.extSystem);
                    }}
                    onSubmitSuccess={() => {
                        dispatch(modalDialogHide());
                        dispatch(registryDetailFetchIfNeeded(id, true));
                    }}
                    initialValues={initialValues}
                    extSystems={filteredExtSystems}
                />,
                'dialog-sm',
            ),
        );
    };

    handleScopeManagement = () => {
        this.props.dispatch(modalDialogShow(this, i18n('accesspoint.scope.management.title'), <ScopeLists />));
    };

    handleShowApHistory = () => {
        const {
            registryDetail: {
                data: {id},
            },
        } = this.props;
        const form = <ApStateHistoryForm accessPointId={id} />;
        this.props.dispatch(modalDialogShow(this, i18n('ap.history.title'), form, 'dialog-lg'));
    };

    handleChangeApState = () => {
        const {
            registryDetail: {
                data: {id, typeId, scopeId},
            },
        } = this.props;
        const form = (
            <ApStateChangeForm
                initialValues={{
                    typeId: typeId,
                    scopeId: scopeId,
                }}
                onSubmit={data => {
                    const finalData = {
                        comment: data.comment,
                        state: data.state,
                        typeId: data.typeId,
                        scopeId: data.scopeId !== '' ? parseInt(data.scopeId) : null,
                    };
                    return WebApi.changeState(id, finalData);
                }}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                    this.props.dispatch(registryDetailFetchIfNeeded(id, true));
                    this.props.dispatch(registryListInvalidate());
                }}
                accessPointId={id}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('ap.state.change'), form));
    };

    buildRibbon = () => {
        const {
            registryDetail: {data, id},
            userDetail,
            extSystems,
            module,
            customRibbon,
            registryDetail,
        } = this.props;

        const parts = module && customRibbon ? customRibbon : {altActions: [], itemActions: [], primarySection: null};

        const altActions = [...parts.altActions];

        if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            altActions.push(
                <Button key="addRegistry" onClick={this.handleAddRegistry}>
                    <Icon glyph="fa-plus-circle" />
                    <div>
                        <span className="btnText">{i18n('registry.addNewRegistry')}</span>
                    </div>
                </Button>,
            );
            altActions.push(
                <Button key="registryImport" onClick={this.handleRegistryImport}>
                    <Icon glyph="fa-download" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.registry.import')}</span>
                    </div>
                </Button>,
            );

            if (extSystems && extSystems.length > 0) {
                altActions.push(
                    <Button key="ap-ext-search" onClick={this.handleApExtSearch}>
                        <Icon glyph="fa-download" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.ap.ext-search')}</span>
                        </div>
                    </Button>,
                );
                altActions.push(
                    <Button key="ext-syncs" onClick={this.handleExtSyncs}>
                        <Icon glyph="fa-gg" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.ap.ext-syncs')}</span>
                        </div>
                    </Button>,
                );
            }
        }
        if (userDetail.hasOne(perms.FUND_ADMIN, perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR)) {
            altActions.push(
                <Button key="scopeManagement" onClick={this.handleScopeManagement}>
                    <Icon glyph="fa-wrench" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.registry.scope.manage')}</span>
                    </div>
                </Button>,
            );
        }

        const itemActions = [...parts.itemActions];
        if (this.canDeleteRegistry()) {
            if (
                userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
                    type: perms.AP_SCOPE_WR,
                    scopeId: data ? data.scopeId : null,
                })
            ) {
                itemActions.push(
                    <Button disabled={data.invalid} key="registryRemove" onClick={this.handleDeleteRegistry}>
                        <Icon glyph="fa-trash" />
                        <div>
                            <span className="btnText">{i18n('registry.deleteRegistry')}</span>
                        </div>
                    </Button>,
                );
            }

            this.props.onShowUsage &&
                itemActions.push(
                    <Button key="registryShow" onClick={() => this.props.onShowUsage(registryDetail)}>
                        <Icon glyph="fa-search" />
                        <div>
                            <span className="btnText">{i18n('registry.registryUsage')}</span>
                        </div>
                    </Button>,
                );
        }

        if (id && data) {
            itemActions.push(
                <Button key="show-state-history" onClick={this.handleShowApHistory}>
                    <Icon glyph="fa-clock-o" />
                    <div>
                        <span className="btnText">{i18n('ap.stateHistory')}</span>
                    </div>
                </Button>,
            );

            // TODO: oprávnění
            itemActions.push(
                <Button key="change-state" onClick={this.handleChangeApState}>
                    <Icon glyph="fa-pencil" />
                    <div>
                        <span className="btnText">{i18n('ap.changeState')}</span>
                    </div>
                </Button>,
            );

            itemActions.push(
                <Button key="connect-ap" onClick={this.handleConnectAp}>
                    <Icon glyph="fa-link" />
                    <div>
                        <span className="btnText">{i18n('ap.connect')}</span>
                    </div>
                </Button>,
            );

            if (userDetail.hasOne(perms.AP_EXTERNAL_WR)) {
                itemActions.push(
                    <Button key="push-ap-to-ext" onClick={this.handlePushApToExt}>
                        <Icon glyph="fa-upload" />
                        <div>
                            <span className="btnText">{i18n('ap.push-to-ext')}</span>
                        </div>
                    </Button>,
                );
            }
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="ribbon-alt-actions" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="ribbon-item-actions" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return <Ribbon primarySection={parts.primarySection} altSection={altSection} itemSection={itemSection} />;
    };

    render() {
        const {splitter, status, registryDetail} = this.props;

        const centerPanel = (
            <div className="registry-page">{registryDetail.id && <ApDetailPageWrapper id={registryDetail.id} />}</div>
        );

        // const rightPanel = registryDetail.fetched ? (
        //     <div className={'registry-history'}>
        //         <DetailHistory apId={registryDetail.id} commentCount={registryDetail.data.comments}/>
        //     </div>
        // ) : false;

        return (
            <Shortcuts
                name="Registry"
                handler={this.handleShortcuts}
                global
                stopPropagation={false}
                className="main-shortcuts2"
            >
                <PageLayout
                    splitter={splitter}
                    key="registryPage"
                    ribbon={this.buildRibbon()}
                    leftPanel={<RegistryList />}
                    centerPanel={centerPanel}
                    // rightPanel={rightPanel}
                    status={status}
                />
            </Shortcuts>
        );
    }
}

export default connect(state => {
    const {
        app: {apExtSystemList, registryDetail, registryList},
        splitter,
        refTables,
        focus,
        userDetail,
    } = state;
    return {
        extSystems: apExtSystemList.fetched ? apExtSystemList.rows : null,
        splitter,
        registryDetail,
        registryList,
        refTables,
        focus,
        userDetail,
    };
})(RegistryPage);
