import React from 'react';
import { connect } from 'react-redux';
import { AbstractReactComponent, i18n, Icon, RibbonGroup, RibbonSplit, Utils } from '../../components/shared';
import { DetailActions } from '../../shared/detail';
import Ribbon from '../../components/page/Ribbon';
import ImportForm from '../../components/form/ImportForm';
import RegistryList from '../../components/registry/RegistryList';
import { Button } from '../../components/ui';
import {
    registryDelete, registryDetailFetchIfNeeded, registryListInvalidate, registryCreateRevision,
    registryDeleteRevision, registryChangeStateRevision, registryDetailInvalidate, registryDetailClear, goToAe, getArchiveEntityUrl, AREA_REGISTRY_DETAIL
} from '../../actions/registry/registry.jsx';
import { modalDialogHide, modalDialogShow } from '../../actions/global/modalDialog.jsx';
import { refRecordTypesFetchIfNeeded } from '../../actions/refTables/recordTypes.jsx';
import { Shortcuts } from 'react-shortcuts';
import { canSetFocus, focusWasSet, isFocusFor, setFocus } from '../../actions/global/focus.jsx';
import * as perms from '../../actions/user/Permission.jsx';
import { apExtSystemListFetchIfNeeded } from '../../actions/registry/apExtSystemList';
import { PropTypes } from 'prop-types';
import './RegistryPage.scss';
import PageLayout from '../shared/layout/PageLayout';
import defaultKeymap from './RegistryPageKeymap.jsx';
import { AP_VIEW_SETTINGS, FOCUS_KEYS, MODAL_DIALOG_SIZE, URL_ENTITY } from '../../constants.tsx';
import * as eidTypes from '../../actions/refTables/eidTypes';
import ScopeLists from '../../components/arr/ScopeLists';
import ApStateHistoryForm from '../../components/registry/ApStateHistoryForm';
import ApStateChangeForm from '../../components/registry/ApStateChangeForm';
import RevStateChangeForm from '../../components/registry/RevStateChangeForm';
import RevMergeForm from '../../components/registry/RevMergeForm';
import { WebApi } from '../../actions';
import ApDetailPageWrapper from '../../components/registry/ApDetailPageWrapper';
import { refApTypesFetchIfNeeded } from '../../actions/refTables/apTypes';
import { refPartTypesFetchIfNeeded } from '../../actions/refTables/partTypes';
import { descItemTypesFetchIfNeeded } from '../../actions/refTables/descItemTypes';
import { refRulDataTypesFetchIfNeeded } from '../../actions/refTables/rulDataTypes';
import CreateAccessPointModal from '../../components/registry/modal/CreateAccessPointModal';
import ApExtSearchModal, { TypeModal } from '../../components/registry/modal/ApExtSearchModal';
import { Area } from 'api/Area';
import { ApPushToExt } from '../../components/registry/modal/ApPushToExt';
import ExtSyncsModal from '../../components/registry/modal/ExtSyncsModal';
import { objectById, storeFromArea } from '../../shared/utils';
import RegistryUsageForm from '../../components/form/RegistryUsageForm';
import { AccessPointDeleteForm } from '../../components/form/AccesspointDeleteForm';
import { StateApproval } from 'api/StateApproval';
import { withRouter } from "react-router";
import { RevStateApproval } from 'api/RevStateApproval';
import { showConfirmDialog } from 'components/shared/dialog';
import { Api } from 'api';
import { routerNavigate } from 'actions/router';
import { isInteger, isUuid } from 'utils/regex';
import { ApCopyModal } from 'components/registry/modal/ap-copy';
import { AP_EXT_SYSTEM_TYPE } from '../../constants';

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistryPage extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    shouldComponentUpdate() {
        return true;
    }

    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }

    static propTypes = {
        splitter: PropTypes.object.isRequired,
        refTables: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        fund: PropTypes.object,
    };

    state = { items: [] };

    componentDidMount() {
        this.initData();
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        this.initData(this.props);
    }

    canDeleteRegistry = () => {
        // We can delete item if has id and data
        const { id, data } = this.props.registryDetail;

        return id && data;
    };

    initData = (props = this.props) => {
        const { dispatch, registryDetail, history, select = false } = this.props;

        //todo: prevest na apTypes
        dispatch(refRecordTypesFetchIfNeeded());

        dispatch(refApTypesFetchIfNeeded());
        dispatch(eidTypes.fetchIfNeeded());
        dispatch(refPartTypesFetchIfNeeded());
        dispatch(descItemTypesFetchIfNeeded());
        dispatch(refRulDataTypesFetchIfNeeded());
        dispatch(DetailActions.fetchIfNeeded(AP_VIEW_SETTINGS, '', WebApi.getApTypeViewSettings));

        if (props.userDetail.hasOne(perms.AP_SCOPE_WR_ALL) || props.userDetail.hasOne(perms.AP_SCOPE_WR) || props.userDetail.hasOne(perms.AP_EXTERNAL_WR)) {
            dispatch(apExtSystemListFetchIfNeeded());
        }

        if (!select) {
            const matchId = this.props.match.params.id;

            // pokud si pamatujeme spolední navštívenou při prvním vstupu - provedeme přesměrování
            if (registryDetail.id !== null && matchId == null) {
                history.replace(`${URL_ENTITY}/${registryDetail.id}`);
            }

            if (isUuid(matchId)) {
                dispatch(registryDetailFetchIfNeeded(matchId)).then((data) => {
                    if (data) {
                        dispatch(routerNavigate(getArchiveEntityUrl(data.id), "REPLACE"));
                    }
                });
            } else if (isInteger(matchId)) {
                dispatch(registryDetailFetchIfNeeded(parseInt(matchId)));
            }
        }

        this.trySetFocus(props);
    };

    trySetFocus = props => {
        const { focus } = props;

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
        // console.log('#handleShortcuts', '[' + action + ']', this);
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

    isFormItemValid = (item) => item['@class'] === '.ApItemEnumVO' ? item.specId !== undefined : item.value !== undefined;

    handleAddRegistry = () => {
        const { dispatch, history, select = false } = this.props;

        dispatch(
            modalDialogShow(
                this,
                i18n('registry.addRegistry'),
                <CreateAccessPointModal
                    initialValues={{}}
                    onSubmit={formData => {
                        if (!formData.partForm) {
                            return Promise.reject('');
                        }
                        const data = {
                            ...formData,
                            partForm: {
                                ...formData.partForm,
                                items: formData.partForm.items.map(({ updatedItem }) => updatedItem).filter(this.isFormItemValid),
                            },
                        };
                        const submitData = {
                            partForm: data.partForm,
                            accessPointId: null,
                            languageCode: null,
                            scopeId: data.scopeId,
                            typeId: data.apType.id,
                        };
                        return WebApi.createAccessPoint(submitData).then((data) => {
                            dispatch(modalDialogHide());
                            this.props.dispatch(goToAe(history, data.id, false, !select));
                            this.props.dispatch(registryListInvalidate());
                        });
                    }}
                />,
                MODAL_DIALOG_SIZE.LG,
                () => { },
            ),
        );
    };

    handleDeleteRegistry = async () => {
        const { dispatch, registryDetail: { data: { id } } } = this.props;
        const result = await dispatch(showConfirmDialog(i18n('registry.deleteRegistryQuestion')));
        if (result) {
            dispatch(registryDelete(id));
        }
    };

    handleRegistryImport = () => {
        this.props.dispatch(modalDialogShow(this, i18n('import.title.registry'), <ImportForm record />));
    };

    handleApExtSearch = () => {
        const { extSystems } = this.props;
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
                MODAL_DIALOG_SIZE.XL,
            ),
        );
    };

    handleExtSyncs = () => {
        const { extSystems, dispatch, history, select = false } = this.props;
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
                        dispatch(goToAe(history, accessPointId, true, !select));
                    }}
                    initialValues={initialValues}
                    extSystems={extSystems}
                />,
                MODAL_DIALOG_SIZE.XL,
            ),
        );
    };

    handleConnectAp = () => {
        const {
            extSystems,
            registryDetail: { data },
            dispatch,
            history,
            select = false,
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
                        dispatch(goToAe(history, id, true, !select));
                    }}
                    itemType={TypeModal.CONNECT}
                    accessPointId={id}
                    initialValues={initialValues}
                    extSystems={filteredExtSystems}
                />,
                MODAL_DIALOG_SIZE.XL,
            ),
        );
    };

    handlePushApToExt = async (item, extSystems) => {
        const {
            dispatch,
            history,
            select = false,
        } = this.props;
        const result = item.revStateApproval != null
            ? await dispatch(showConfirmDialog(i18n('ap.push-to-ext.confirmation')))
            : true;

        if (result) {
            dispatch(
                modalDialogShow(
                    this,
                    i18n('ap.push-to-ext.title'),
                    <ApPushToExt
                        detail={item}
                        onSubmit={async (data) => {
                            try {
                                await WebApi.saveAccessPoint(item.id, data.extSystemCode);
                            } catch (e) {
                                throw Error(e);
                            }
                            dispatch(modalDialogHide());
                            dispatch(goToAe(history, item.id, true, !select));
                            return;
                        }}
                        extSystems={extSystems}
                    />,
                ),
            );
        }
    };

    handleApCopy = () => {
        const { dispatch, detail, history } = this.props;
        if (!detail) { throw Error("No accesspoint detail.") }
        dispatch(
            modalDialogShow(
                this,
                i18n('ap.copy.title'),
                <ApCopyModal
                    onSubmit={async (data) => {
                        const id = detail.id;
                        const result = await Api.accesspoints.copyAccessPoint(id, data);
                        dispatch(modalDialogHide())
                        dispatch(goToAe(history, result.data.id, true, true));
                        return;
                    }}
                    detail={detail.data}
                />,
            ),
        );
    }

    handleScopeManagement = () => {
        this.props.dispatch(modalDialogShow(this, i18n('accesspoint.scope.management.title'), <ScopeLists />));
    };

    handleShowApHistory = () => {
        const {
            registryDetail: {
                data: { id },
            },
        } = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('ap.history.title'),
                <ApStateHistoryForm accessPointId={id} />,
                MODAL_DIALOG_SIZE.LG,
            ),
        );
    };

    handleRegistryShowUsage = data => {
        this.props.dispatch(modalDialogShow(this, i18n('registry.registryUsage'), <RegistryUsageForm detail={data} />));
    };

    handleDeleteAccessPoint = async (accessPointDetail) => {
        const { dispatch } = this.props;
        const result = accessPointDetail.data.revStateApproval != null
            ? await dispatch(showConfirmDialog(i18n('accesspoint.removeDuplicity.confirmation')))
            : true;
        if (result) {
            dispatch(modalDialogShow(
                this,
                i18n('accesspoint.removeDuplicity.title'),
                <AccessPointDeleteForm
                    detail={accessPointDetail}
                    onSubmitSuccess={() => {
                        dispatch(registryDetailInvalidate());
                    }}
                />
            ));
        }
    };

    handleChangeApState = () => {
        const {
            dispatch,
            history,
            registryDetail: {
                data: { id, typeId, scopeId, stateApproval },
            },
            select = false,
        } = this.props;
        const form = (
            <ApStateChangeForm
                accessPointId={id}
                initialValues={{
                    state: stateApproval,
                    typeId: typeId,
                    scopeId: scopeId,
                }}
                onSubmit={async (data) => {
                    const finalData = {
                        comment: data.comment,
                        stateApproval: data.state,
                        typeId: data.typeId,
                        scopeId: data.scopeId !== '' ? parseInt(data.scopeId) : null,
                    };
                    await Api.accesspoints.accessPointChangeState(id, finalData); // TODO - apVersion zatim neni dostupna

                    dispatch(modalDialogHide());
                    dispatch(goToAe(history, id, true, !select));
                }}
            />
        );
        dispatch(modalDialogShow(this, i18n('ap.changeState'), form));
    };

    handleCreateRevision = async () => {
        const { dispatch, history, select = false, registryDetail: { data: { id } } } = this.props;
        const result = await dispatch(showConfirmDialog(i18n('registry.createRevisionQuestion')));
        if (result) {
            dispatch(registryCreateRevision(id, history, select));
        }
    };

    handleDeleteRevision = async () => {
        const { dispatch, history, select = false, registryDetail: { data: { id } } } = this.props;
        const result = await dispatch(showConfirmDialog(i18n('registry.deleteRevisionQuestion')));
        if (result) {
            dispatch(registryDeleteRevision(id, history, select));
        }
    };

    handleChangeStateRevision = () => {
        const {
            dispatch,
            history,
            registryDetail: {
                data: { id, newTypeId, revStateApproval },
            },
            select = false,
        } = this.props;
        const form = (
            <RevStateChangeForm
                initialValues={{
                    state: revStateApproval,
                    typeId: newTypeId,
                }}
                onSubmit={async (data) => {
                    await dispatch(registryChangeStateRevision(id, undefined, data, history, select)) // TODO - apVersion zatim neni dostupna

                    dispatch(modalDialogHide());
                    dispatch(goToAe(history, id, true, !select));
                    dispatch(registryListInvalidate());
                }}
            />
        );
        dispatch(modalDialogShow(this, i18n('registry.changeStateRevision'), form));
    };

    handleMergeRevision = () => {
        const {
            history,
            registryDetail: {
                data: { id, stateApproval },
            },
            select = false,
        } = this.props;
        const form = (
            <RevMergeForm
                initialValues={{
                    state: stateApproval,
                }}
                onSubmit={data => {
                    return WebApi.mergeRevision(id, data.state);
                }}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                    this.props.dispatch(registryDetailInvalidate());
                    this.props.dispatch(goToAe(history, id, true, !select));
                    this.props.dispatch(registryListInvalidate());
                }}
                accessPointId={id}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('registry.mergeRevision'), form));
    };

    handleRestoreEntity = async () => {
        const { registryDetail: { id } } = this.props;
        await Api.accesspoints.restoreAccessPoint(id);
        this.props.dispatch(registryDetailInvalidate());
    }

    buildRibbon = () => {
        const {
            registryDetail: { data, id },
            userDetail,
            extSystems,
            module,
            customRibbon,
            registryDetail,
            select,
        } = this.props;

        const parts = module && customRibbon ? customRibbon : { altActions: [], itemActions: [], primarySection: null };
        const hasRevision = data?.revStateApproval != null;

        const completeExternalSystems = extSystems?.filter((extSystem) => extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE);
        const hasOnlyCompleteExternalSystems = completeExternalSystems?.length === extSystems?.length;

        const altActions = [...parts.altActions];

        if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR)) {
            altActions.push(
                <Button key="addRegistry" onClick={this.handleAddRegistry}>
                    <Icon glyph="fa-plus-circle" />
                    <div>
                        <span className="btnText">{i18n('registry.addNewRegistry')}</span>
                    </div>
                </Button>,
            );
            if (!select) {
                altActions.push(
                    <Button key="registryImport" onClick={this.handleRegistryImport}>
                        <Icon glyph="fa-file" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.registry.import')}</span>
                        </div>
                    </Button>,
                );

                if (extSystems && extSystems.length > 0 && !hasOnlyCompleteExternalSystems) {
                    altActions.push(
                        <Button key="ap-ext-search" onClick={this.handleApExtSearch}>
                            <Icon glyph="fa-cloud-download" />
                            <div>
                                <span className="btnText">{i18n('ribbon.action.ap.ext-search')}</span>
                            </div>
                        </Button>,
                    );
                }
                if (extSystems && extSystems.length > 0 && (!hasOnlyCompleteExternalSystems || userDetail.hasOne(perms.ADMIN))) {
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
        }

        const itemActions = [...parts.itemActions];
        const revisionActions = [];
        const invalidItemActions = [];

        if (!select) {
            if (userDetail.hasOne(perms.ADMIN)) {
                altActions.push(
                    <Button key="scopeManagement" onClick={this.handleScopeManagement}>
                        <Icon glyph="fa-wrench" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.registry.scope.manage')}</span>
                        </div>
                    </Button>,
                );
            }

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

                if (
                    userDetail.hasOne(
                        perms.AP_SCOPE_WR_ALL,
                        {
                            type: perms.AP_SCOPE_WR,
                            scopeId: data ? data.scopeId : null,
                        }
                    ) && userDetail.hasOne(
                        perms.FUND_RD,
                        perms.FUND_RD_ALL,
                    )
                ) {
                    itemActions.push(
                        <Button key="registryShow" onClick={() => this.handleRegistryShowUsage(registryDetail)}>
                            <Icon glyph="fa-search" />
                            <div>
                                <span className="btnText">{i18n('registry.registryUsage')}</span>
                            </div>
                        </Button>,
                    );
                }

                if (
                    userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
                        type: perms.AP_SCOPE_WR,
                        scopeId: data ? data.scopeId : null,
                    })
                ) {
                    itemActions.push(
                        <Button key="deleteReplaceAccessPoint" onClick={() => this.handleDeleteAccessPoint(registryDetail)}>
                            <Icon glyph="fa-ban" />
                            <div>
                                <span className="btnText">{i18n('accesspoint.removeDuplicity')}</span>
                            </div>
                        </Button>,
                    );
                }
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

                if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR,
                    perms.AP_CONFIRM_ALL, perms.AP_CONFIRM,
                    perms.AP_EDIT_CONFIRMED_ALL, perms.AP_EDIT_CONFIRMED
                )) {
                    itemActions.push(
                        <Button key="change-state" onClick={this.handleChangeApState} disabled={hasRevision}>
                            <Icon glyph="fa-pencil" />
                            <div>
                                <span className="btnText">{i18n('ap.changeState')}</span>
                            </div>
                        </Button>,
                    );
                }

                // Vypnuti moznosti propojeni AP s AP v CAMu
                // TODO: remove related code
                /*
                if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR)) {
                itemActions.push(
                <Button key="connect-ap" onClick={this.handleConnectAp}>
                <Icon glyph="fa-link" />
                <div>
                <span className="btnText">{i18n('ap.connect')}</span>
                </div>
                </Button>,
                );
                }
                */

                if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR)) {
                    itemActions.push(
                        <Button key="push-ap-to-ext" onClick={this.handleApCopy}>
                            <Icon glyph="fa-copy" />
                            <div>
                                <span className="btnText">{i18n("ap.copy.title")}</span>
                            </div>
                        </Button>,
                    );
                }

                if (hasRevision) {
                    revisionActions.push(
                        <Button disabled={data.invalid} key="revisionDelete" onClick={this.handleDeleteRevision}>
                            <Icon glyph="fa-undo" />
                            <div>
                                <span className="btnText">{i18n('registry.deleteRevision')}</span>
                            </div>
                        </Button>,
                    );
                    revisionActions.push(
                        <Button disabled={data.invalid} key="revisionChangeState" onClick={this.handleChangeStateRevision}>
                            <Icon glyph="fa-pencil" />
                            <div>
                                <span className="btnText">{i18n('registry.changeStateRevision')}</span>
                            </div>
                        </Button>,
                    );
                    revisionActions.push(
                        <Button disabled={data.invalid} key="revisionMerge" onClick={this.handleMergeRevision}>
                            <Icon glyph="fa-check" />
                            <div>
                                <span className="btnText">{i18n('registry.mergeRevision')}</span>
                            </div>
                        </Button>,
                    );
                } else if (!data.invalid) {
                    revisionActions.push(
                        <Button disabled={data.invalid} key="revisionCreate" onClick={this.handleCreateRevision}>
                            <Icon glyph="fa-plus" />
                            <div>
                                <span className="btnText">{i18n('registry.createRevision')}</span>
                            </div>
                        </Button>,
                    );
                }
                if (data.invalid) {
                    invalidItemActions.push(
                        <Button key="restoreEntity" onClick={this.handleRestoreEntity}>
                            <Icon glyph="fa-undo" />
                            <div>
                                <span className="btnText">{i18n('registry.restoreEntity')}</span>
                            </div>
                        </Button>,
                    );
                }
            }
        }

        const altSection = altActions.length > 0 ? (
            <>
                {altActions.length > 0 &&
                    <RibbonGroup className="small" >
                        {altActions}
                    </RibbonGroup>}
                {itemActions.length > 0 &&
                    <RibbonGroup className="small" >
                        {itemActions}
                    </RibbonGroup>}
                {revisionActions.length > 0 &&
                    <RibbonGroup className="small" >
                        {revisionActions}
                    </RibbonGroup>}
                {invalidItemActions.length > 0 &&
                    <RibbonGroup className="small" >
                        {invalidItemActions}
                    </RibbonGroup>}
            </>
        ) : undefined;

        return <Ribbon primarySection={parts.primarySection} altSection={altSection} showUser={!select} />;
    };

    getEditMode = () => {
        const { registryDetail, userDetail } = this.props;
        let editMode = false;
        if (registryDetail.id && registryDetail.data) {
            const apState = registryDetail.data.stateApproval;
            const revisionState = registryDetail.data.revStateApproval;
            if (apState !== StateApproval.TO_APPROVE && apState !== StateApproval.APPROVED) {
                editMode = userDetail.hasOne(perms.ADMIN, perms.AP_SCOPE_WR_ALL, {
                    type: perms.AP_SCOPE_WR,
                    scopeId: registryDetail.data.scopeId,
                });
            }
            if (
                revisionState === RevStateApproval.ACTIVE
                || revisionState === RevStateApproval.TO_AMEND
            ) {
                if (apState === StateApproval.APPROVED) {
                    editMode = userDetail.hasOne(perms.ADMIN, perms.AP_EDIT_CONFIRMED_ALL, {
                        type: perms.AP_EDIT_CONFIRMED,
                        scopeId: registryDetail.data.scopeId,
                    });
                } else {
                    editMode = userDetail.hasOne(perms.ADMIN, perms.AP_SCOPE_WR_ALL, {
                        type: perms.AP_SCOPE_WR,
                        scopeId: registryDetail.data.scopeId,
                    });
                }
            }
        }
        return editMode;
    }

    render() {
        const { splitter, status, registryDetail, select = false } = this.props;

        const centerPanel = (
            <div className="registry-page">
                {(registryDetail.fetched || (select && registryDetail.id && registryDetail.fetched)) &&
                    <ApDetailPageWrapper
                        select={select}
                        id={registryDetail?.data?.id}
                        editMode={this.getEditMode()}
                        onPushApToExt={this.handlePushApToExt}
                    />}
            </div>
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
                    leftPanel={<RegistryList select={select} fund={this.props.fund} />}
                    centerPanel={centerPanel}
                    // rightPanel={rightPanel}
                    status={status}
                />
            </Shortcuts>
        );
    }
}

export default withRouter(connect(state => {
    const {
        app: { apExtSystemList, registryDetail, registryList },
        splitter,
        refTables,
        focus,
        userDetail,
    } = state;
    return {
        detail: storeFromArea(state, AREA_REGISTRY_DETAIL),
        extSystems: apExtSystemList.fetched ? apExtSystemList.rows : null,
        splitter,
        registryDetail,
        registryList,
        refTables,
        focus,
        userDetail,
    };
})(RegistryPage));
