/**
 * Stránka archivních pomůcek.
 */

import './FundActionPage.scss';

import ArrParentPage from "./ArrParentPage.jsx";

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {
    Ribbon,
    FundNodesSelectForm,
    FundNodesList,
    FormInput,
    ArrFundPanel
} from 'components/index.jsx';
import {
    StoreHorizontalLoader,
    Icon,
    i18n,
    AbstractReactComponent,
    ListBox,
    RibbonGroup,
    Utils
} from 'components/shared';
import {Button} from 'react-bootstrap';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {
    fundActionFetchDetailIfNeeded,
    fundActionFetchListIfNeeded,
    fundActionFetchConfigIfNeeded,
    fundActionFormChange,
    fundActionFormShow,
    fundActionFormSubmit,
    fundActionActionSelect,
    funcActionActionInterrupt,
    fundActionFormReset
} from 'actions/arr/fundAction.jsx';
import * as perms from 'actions/user/Permission.jsx';
import {getOneSettings} from 'components/arr/ArrUtils.jsx';
import {canSetFocus, setFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {ActionState} from '../../constants.tsx'
import {actionStateTranslation, fundActionActionReceive} from "../../actions/arr/fundAction";
import {PropTypes} from 'prop-types';
import defaultKeymap from './FundActionPageKeymap.jsx';
import PersistentSortForm from "../../components/arr/PersistentSortForm";
import {PERSISTENT_SORT_CODE} from "../../constants.tsx";
import {descItemTypesFetchIfNeeded} from "../../actions/refTables/descItemTypes";

class FundActionPage extends ArrParentPage {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    componentWillMount(){
        let newKeymap = Utils.mergeKeymaps(ArrParentPage.defaultKeymap,defaultKeymap);
        Utils.addShortcutManager(this,newKeymap);
    }
    static propTypes = {};

    constructor(props) {
        super(props, "arr-actions-page");

        this.bindMethods(
            'handleListBoxActionSelect',
            'handleRibbonFormClear',
            'handleRibbonCreateAction',
            'handleRibbonNewAction',
            'handleRibbonCopyAction',
            'handleRibbonInterruptAction',
            'handleFormNodesAdd',
            'handleFormNodeDelete'
        );

        this.state = {};
    }

    componentDidMount() {
        this.props.dispatch(descItemTypesFetchIfNeeded());

        const fund = this.getActiveFund(this.props);
        if (fund) {
            this.dispatch(fundActionFetchListIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchConfigIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchDetailIfNeeded(fund.versionId));
        }
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(descItemTypesFetchIfNeeded());

        const fund = this.getActiveFund(nextProps);
        if(fund) {
            this.dispatch(fundActionFetchListIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchConfigIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchDetailIfNeeded(fund.versionId));
        }
    }

    handleShortcuts(action,e) {
        console.log("#handleShortcuts FundActionPage", '[' + action + ']', this);
        switch (action) {
            case 'newAction':
                this.handleRibbonNewAction();
                break;
            default:
                super.handleShortcuts(action,e);
        }
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasFundActionPage(activeFund ? activeFund.id : null);
    }

    handleRibbonFormClear() {
        const fund = this.getActiveFund(this.props);
        const {versionId} = fund;
        this.dispatch(fundActionFormReset(versionId));
    } // Form reset

    handleRibbonCopyAction() {
        const fund = this.getActiveFund(this.props);
        const {fundAction: {detail: {data}}, versionId} = fund;
        this.dispatch(fundActionFormChange(versionId, {
            nodes: data.nodes,
            code: data.code
        }));
        this.dispatch(fundActionFormShow(versionId));
    }

    handleRibbonCreateAction() {
        const fund = this.getActiveFund(this.props);
        const {fundAction: {form}, versionId} = fund;

        if (form.code !== '' && form.nodes && form.nodes.length > 0) {
            if (form.code === PERSISTENT_SORT_CODE) {
                //zavolá metodu FundActionPage#submitPersistentSortForm
                this.refs.persistentSortForm.getWrappedInstance().submit();
            } else {
                this.dispatch(fundActionFormSubmit(versionId, form.code))
            }
        }
    }

    submitPersistentSortForm = (versionId, values) => {
        return this.dispatch(fundActionFormSubmit(versionId, PERSISTENT_SORT_CODE, values))
    };

    handleRibbonInterruptAction() {
        const fund = this.getActiveFund(this.props);
        const {fundAction: {detail: {currentDataKey}}} = fund;
        this.dispatch(funcActionActionInterrupt(currentDataKey));
    }

    handleRibbonNewAction() {
        const fund = this.getActiveFund(this.props);
        const {versionId} = fund;
        this.dispatch(fundActionFormShow(versionId))
    }

    handleListBoxActionSelect(item) {
        const fund = this.getActiveFund(this.props);
        const {versionId} = fund;
        this.dispatch(fundActionActionSelect(versionId, item.id))
    }

    handleFormNodesAdd() {
        const fund = this.getActiveFund(this.props);
        const {versionId} = fund;
        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesSelectForm
                onSubmitForm={(nodeIds, nodes) => {
                    const fund = this.getActiveFund(this.props);
                    const {fundAction:{form}} = fund;
                    const newNodes = [
                        ...form.nodes
                    ];
                    nodes.map(item => {
                        indexById(newNodes, item.id) === null && newNodes.push(item);
                    });
                    this.dispatch(fundActionFormChange(versionId, {nodes: newNodes}));
                    this.dispatch(modalDialogHide());
                }}
            />
        ))
    }

    handleFormNodeDelete(item) {
        const fund = this.getActiveFund(this.props);
        const {fundAction:{form}, versionId} = fund;
        const index = indexById(form.nodes, item.id);
        if (index !== null) {
            this.dispatch(fundActionFormChange(versionId, {
                nodes: [
                    ...form.nodes.slice(0, index),
                    ...form.nodes.slice(index + 1)
                ]
            }))
        }
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const {userDetail} = this.props

        var detail = false;
        var isFormVisible = false;
        if (fund) {
            detail = fund.fundAction.detail;
            isFormVisible = fund.fundAction.isFormVisible;
        }

        var altActions = [];
        if (fund) {
            if (!isFormVisible  && !readMode && !closed) {
                if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id})) {
                    altActions.push(
                        <Button key="new-action" onClick={this.handleRibbonNewAction}><Icon glyph="fa-plus-circle"/>
                            <div><span className="btnText">{i18n('ribbon.action.fundAction.action.new')}</span></div>
                        </Button>
                    );
                }
            }
        }

        const itemActions = [];
        if (fund) {
            if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id}) && !readMode && !closed) {
                if (isFormVisible) {
                    itemActions.push(
                        <Button key="run-action" onClick={this.handleRibbonCreateAction}><Icon glyph="fa-play"/>
                            <div><span className="btnText">{i18n('ribbon.action.fundAction.form.run')}</span></div>
                        </Button>,
                        <Button key="clear-action" onClick={this.handleRibbonFormClear}><Icon glyph="fa-trash"/>
                            <div><span className="btnText">{i18n('ribbon.action.fundAction.form.clear')}</span></div>
                        </Button>
                    );
                } else if (detail.fetched && !detail.isFetching && detail.data && detail.currentDataKey === detail.data.id) {
                    const {data} = detail;
                    itemActions.push(
                        <Button key="copy-action" onClick={this.handleRibbonCopyAction}><Icon glyph="fa-refresh"/>
                            <div><span className="btnText">{i18n('ribbon.action.fundAction.action.copy')}</span></div>
                        </Button>
                    );
                    switch (data.state) {
                        case ActionState.PLANNED:
                        case ActionState.RUNNING:
                        {
                            itemActions.push(
                                <Button key="stop-action" onClick={this.handleRibbonInterruptAction}><Icon glyph="fa-sync"/>
                                    <div><span
                                        className="btnText">{i18n('ribbon.action.fundAction.action.interrupt')}</span></div>
                                </Button>
                            );
                            break;
                        }
                        case ActionState.WAITING:
                        {
                            itemActions.push(
                                <Button key="-action" onClick={this.handleRibbonInterruptAction}><Icon glyph="fa-times"/>
                                    <div><span className="btnText">{i18n('ribbon.action.fundAction.action.cancel')}</span>
                                    </div>
                                </Button>
                            );
                            break;
                        }
                        // case ActionState.FINISHED:
                        // case ActionState.ERROR:
                        // case ActionState.INTERRUPTED:
                    }
                }
            }
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr subMenu fundId={fund.id} altSection={altSection} itemSection={itemSection}/>
        )
    }

    getConfigByCode(code) {
        const fund = this.getActiveFund(this.props);
        const configs = fund.fundAction.config.data;
        const index = indexById(configs, code, 'code');
        if (index !== null) {
            return configs[index];
        }
        return null;
    }


    static getStateIcon(state) {
        switch (state) {
            case ActionState.RUNNING:
                return <Icon glyph='fa-cog'/>;
            case ActionState.WAITING:
                return <Icon glyph='fa-clock-o'/>;
            case ActionState.OUTDATED:
            case ActionState.FINISHED:
                return <Icon glyph='fa-check'/>;
            case ActionState.ERROR:
                return <Icon glyph='fa-exclamation'/>;
            case ActionState.PLANNED:
                return <Icon glyph='fa-calendar'/>;
            case ActionState.INTERRUPTED:
                return <Icon glyph='fa-times'/>;
            default:
                return;
        }
    }

    renderRowItem(props) {
        const {item} = props;
        const icon = FundActionPage.getStateIcon(item.state);
        const config = this.getConfigByCode(item.code);
        const name = config ? <span title={item.name} className='name'>{config.name}</span> : '';

        return (
            <div className='item' key={item.id}>
                {icon}
                <div>
                    <div>{name}</div>
                    <div>
                        {item.date}
                        {actionStateTranslation(item.state)}
                    </div>
                </div>

            </div>
        )
    }

    renderLeftPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);

        return <div className='actions-list-container'>
            <StoreHorizontalLoader store={fund.fundAction.list}/>
            {fund.fundAction.list.fetched && <ListBox
                className='actions-listbox'
                key='actions-list'
                activeIndex={indexById(fund.fundAction.list.data, fund.fundAction.detail.currentDataKey)}
                items={fund.fundAction.list.data}
                renderItemContent={this.renderRowItem.bind(this)}
                onSelect={this.handleListBoxActionSelect}
                onFocus={this.handleListBoxActionSelect}
            />}
        </div>;
    }

    renderCenterPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const {fundAction: {detail, isFormVisible, config, form}, versionId} = fund;
        const fundActionCount = fund.fundAction.list.data ? fund.fundAction.list.data.length : 0;
        if (isFormVisible) {
            let description = null;
            if (config.fetched && form.code !== null) {
                const index = indexById(config.data, form.code, 'code');
                if (index !== null) {
                    const text = config.data[index].description;
                    description = <div>
                        <div>Popis</div>
                        <div>{text}</div>
                    </div>
                }
            }

            return <div className='center-container'>
                <StoreHorizontalLoader store={config}/>
                {config.fetched && <div>
                    <h2>{i18n('arr.fundAction.form.newAction')}</h2>
                    <div>
                        <FormInput componentClass="select"
                                   label={i18n('arr.fundAction.form.type')}
                                   key='code-action'
                                   ref='code-action'
                                   className='form-control'
                                   value={form.code}
                                   onChange={(e) => {this.dispatch(fundActionFormChange(versionId, {code: e.target.value}))}}
                        >
                            <option key="novalue" />
                            {config.data.map((item) => (<option key={item.code} value={item.code}>{item.name}</option>))}
                        </FormInput>
                    </div>
                    {description}
                    <h2>{i18n("arr.fundAction.title.nodes")}</h2>
                    <FundNodesList
                        nodes={form.nodes}
                        onAddNode={this.handleFormNodesAdd}
                        onDeleteNode={this.handleFormNodeDelete}
                    />
                    {form.code === PERSISTENT_SORT_CODE && [
                    <h2>{i18n("arr.functions.configuration")}</h2>,
                    <PersistentSortForm
                        initialValues={detail.data && detail.data.config && JSON.parse(detail.data.config)}
                        onSubmit={this.submitPersistentSortForm}
                        ref={"persistentSortForm"}
                        versionId={versionId}
                    />]}
                </div>}
            </div>
        }

        if (detail) {
            if (!detail.isFetching && !detail.fetched) {    // pokud načítá ale nemá načteno
                return <div className='center-container'>
                    <div className="unselected-msg">
                        <div className="title">{fundActionCount > 0 ? i18n('arr.fundAction.noSelection.title') : i18n('arr.fundAction.emptyList.title')}</div>
                        <div className="msg-text">{fundActionCount > 0 ? i18n('arr.fundAction.noSelection.message') : i18n('arr.fundAction.emptyList.message')}</div>
                    </div>
                </div>;
            }

            let config;
            let date;
            let data;
            if (detail.fetched) {
                data = detail.data;
                config = this.getConfigByCode(data.code);
                if (data.datePlanned) {
                    date = data.datePlanned;
                } else if (data.dateStarted) {
                    date = data.dateStarted;
                } else if (data.dateFinished) {
                    date = data.dateFinished;
                }
                date = Utils.dateTimeToString(new Date(date));
            }

            return <div className='center-container'>
                <StoreHorizontalLoader store={detail} />

                {detail.fetched && <div className='detail'>
                    <div>
                        <h1>{config.name}</h1>
                        <h3>{FundActionPage.getStateIcon(data.state)} {actionStateTranslation(data.state)}
                            <small>{date}</small>
                        </h3>
                    </div>
                    <div><textarea className='config' readOnly={true} value=""/></div>
                    {data.error ? <div><h3>{i18n('arr.fundAction.error')}</h3>
                        <div>{data.error}</div>
                    </div> : ''}
                    <FundNodesList
                        nodes={data.nodes}
                        readOnly={true}
                    />
                </div>}
            </div>
        }
    }
};

function mapStateToProps(state) {
    const {arrRegion, splitter, userDetail} = state;
    return {
        arrRegion,
        splitter,
        userDetail,
    }
}

export default connect(mapStateToProps)(FundActionPage);
