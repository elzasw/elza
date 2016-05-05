/**
 * Stránka archivních pomůcek.
 */

require('./FundActionPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {
    Loading,
    Icon,
    Ribbon,
    i18n,
    AbstractReactComponent,
    ListBox,
    RibbonGroup,
    FundNodesAddForm,
    FundNodesList
} from 'components/index.jsx';
import {Button, Input} from 'react-bootstrap';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {PageLayout} from 'pages/index.jsx';
import {dateTimeToString} from 'components/Utils.jsx'
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
} from 'actions/arr/fundAction.jsx'
import * as perms from 'actions/user/Permission.jsx';

const ActionState = {
    RUNNING: 'RUNNING',
    WAITING: 'WAITING',
    PLANNED: 'PLANNED',
    FINISHED: 'FINISHED',
    ERROR: 'ERROR',
    INTERRUPTED: 'INTERRUPTED'
};

var FundActionPage = class FundActionPage extends AbstractReactComponent {

    constructor(props) {
        super(props);

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

    getFund(props = this.props) {
        return props.arrRegion.activeIndex !== null ? props.arrRegion.funds[props.arrRegion.activeIndex] : false;
    }

    componentDidMount() {
        const fund = this.getFund();
        if (fund) {
            this.dispatch(fundActionFetchListIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchConfigIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchDetailIfNeeded(fund.versionId));
        }
    }

    componentWillReceiveProps(nextProps) {
        const fund = this.getFund(nextProps);
        if(fund) {
            this.dispatch(fundActionFetchListIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchConfigIfNeeded(fund.versionId));
            this.dispatch(fundActionFetchDetailIfNeeded(fund.versionId));
        }
    }

    renderCenter(fund) {
        if (!fund) {
            return <div className='text-center center-container'>{i18n('arr.fundAction.noFa')}</div>;
        }
        const {fundAction: {detail, isFormVisible, config, form}, versionId} = fund;

        if (isFormVisible) {
            if (config.isFetching && !config.fetched) {
                return <Loading />
            }

            var description = null;
            if (form.code !== null) {
                const index = indexById(config.data, form.code, 'code');
                if (index !== null) {
                    const text = config.data[index].description;
                    description = <div>
                        <div>Popis</div>
                        <div>{text}</div>
                    </div>
                }
            }
            
            return <div>
                <h2>{i18n('arr.fundAction.form.newAction')}</h2>
                <div>
                <Input type="select"
                       label={i18n('arr.fundAction.form.type')}
                       key='code-action'
                       ref='code-action'
                       className='form-control'
                       value={form.code}
                       onChange={(e) => {this.dispatch(fundActionFormChange(versionId, {code: e.target.value}))}}
                       >
                    <option key="novalue" />
                    {config.data.map((item) => (<option key={item.code} value={item.code}>{item.name}</option>))}
                </Input>
                </div>
                {description}
                <Button onClick={this.handleFormNodesAdd}><Icon glyph="fa-plus"/> {i18n('arr.fundAction.form.addNodes')}</Button>
                <FundNodesList
                    nodes={form.nodes}
                    onDeleteNode={this.handleFormNodeDelete}
                />
            </div>
        }

        if (detail) {
            if (detail.isFetching && !detail.fetched) {
                return <Loading />
            }
            if (detail.fetched) {
                const {data} = detail;
                const config = this.getConfigByCode(data.code);
                var date = null;
                if (data.datePlanned) {
                    date = dateTimeToString(new Date(data.datePlanned));
                } else if (data.dateStarted) {
                    date = dateTimeToString(new Date(data.dateStarted));
                } else if (data.dateFinished) {
                    date = dateTimeToString(new Date(data.dateFinished));
                }

                return <div className='detail'>
                    <div>
                        <h1>{config.name}</h1>
                        <h3>{this.getStateIcon(data.state)} {this.getStateTranslation(data.state)}
                            <small>{date}</small>
                        </h3>
                    </div>
                    <div><textarea className='config' readOnly={true} value="KONFIGURACE AKCE"/></div>
                    {data.error ? <div><h3>{i18n('arr.fundAction.error')}</h3>
                        <div>{data.error}</div>
                    </div> : ''}
                    <FundNodesList
                        nodes={data.nodes}
                        readOnly
                    />
                </div>
            }
        }
    }

    handleRibbonFormClear() {
        const {versionId} = this.getFund();
        this.dispatch(fundActionFormReset(versionId));
    } // Form reset

    handleRibbonCopyAction() {
        const {fundAction: {detail: {data}}, versionId} = this.getFund();
        this.dispatch(fundActionFormChange(versionId, {
            nodes: data.nodes,
            code: data.code
        }));
        this.dispatch(fundActionFormShow(versionId));
    }

    handleRibbonCreateAction() {
        const {fundAction: {form}, versionId} = this.getFund();
        if (form.code !== '' && form.nodes && form.nodes.length > 0) {
            this.dispatch(fundActionFormSubmit(versionId))
        }
    }

    handleRibbonInterruptAction() {
        const {fundAction: {detail: {currentDataKey}}} = this.getFund();
        this.dispatch(funcActionActionInterrupt(currentDataKey));
    }

    handleRibbonNewAction() {
        const {versionId} = this.getFund();
        this.dispatch(fundActionFormShow(versionId))
    }

    handleListBoxActionSelect(item) {
        const {versionId} = this.getFund();
        this.dispatch(fundActionActionSelect(versionId, item.id))
    }

    handleFormNodesAdd() {
        const {versionId} = this.getFund();
        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesAddForm
                onSubmitForm={(nodeIds, nodes) => {
                    const {fundAction:{form}} = this.getFund();
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
        const {fundAction:{form}, versionId} = this.getFund();
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
    buildRibbon() {
        const fund = this.getFund();
        const {fundAction: {detail, isFormVisible}} = fund;
        const {userDetail} = this.props

        var altActions = [];
        if (fund) {
            if (!isFormVisible) {
                if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id})) {
                    altActions.push(
                        <Button key="new-action" onClick={this.handleRibbonNewAction}><Icon glyph="fa-plus"/>
                            <div><span className="btnText">{i18n('ribbon.action.fundAction.action.new')}</span></div>
                        </Button>
                    );
                }
            }
        }

        const itemActions = [];
        if (fund) {
            if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id})) {
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
            <Ribbon arr altSection={altSection} itemSection={itemSection}/>
        )
    }

    getConfigByCode(code) {
        const configs = this.getFund().fundAction.config.data;
        const index = indexById(configs, code, 'code');
        if (index !== null) {
            return configs[index];
        }
        return null;
    }


    getStateIcon(state) {
        switch (state) {
            case ActionState.RUNNING:
                return <Icon glyph='fa-cog'/>;
            case ActionState.WAITING:
                return <Icon glyph='fa-clock-o'/>;
            case ActionState.FINISHED:
                return <Icon glyph='fa-check'/>;
            case ActionState.ERROR:
                return <Icon glyph='fa-exclamation'/>;
            case ActionState.PLANNED:
                return <Icon glyph='fa-calendar'/>;
            case ActionState.INTERRUPTED:
                return <Icon glyph='fa-times'/>;
            default:
                return <Icon glyph='fa-question'/>;
        }
    }

    getStateTranslation(state) {
        switch (state) {
            case ActionState.RUNNING:
                return i18n('arr.fundAction.state.running');
            case ActionState.WAITING:
                return i18n('arr.fundAction.state.waiting');
            case ActionState.FINISHED:
                return i18n('arr.fundAction.state.finished');
            case ActionState.ERROR:
                return i18n('arr.fundAction.state.error');
            case ActionState.PLANNED:
                return i18n('arr.fundAction.state.planned');
            case ActionState.INTERRUPTED:
                return i18n('arr.fundAction.state.interrupted');
            default:
                return i18n('arr.fundAction.state.unknown');
                break;
        }
    }


    renderRowItem(item) {
        const icon = this.getStateIcon(item.state);
        const config = this.getConfigByCode(item.code);
        const name = config ? <span title={item.name} className='name'>{config.name}</span> : '';

        return (
            <div className='item' key={item.id}>
                {icon}
                <div>
                    <div>{name}</div>
                    <div>
                        {item.date}
                        {this.getStateTranslation(item.state)}
                    </div>
                </div>

            </div>
        )
    }

    render() {
        const {arrRegion, splitter, userDetail} = this.props;
        const fund = arrRegion.activeIndex !== null ? arrRegion.funds[arrRegion.activeIndex] : false;

        var leftPanel
        var centerPanel
        if (userDetail.hasFundActionPage(fund ? fund.id : null)) { // má právo na tuto stránku
            if (fund) {
                leftPanel = <div className='actions-list-container'>{
                    fund.fundAction.list.fetched ?
                    <ListBox
                        className='actions-listbox'
                        key='actions-list'
                        activeIndex={indexById(fund.fundAction.list.data, fund.fundAction.detail.currentDataKey)}
                        items={fund.fundAction.list.data}
                        renderItemContent={this.renderRowItem.bind(this)}
                        onSelect={this.handleListBoxActionSelect}
                        onFocus={this.handleListBoxActionSelect}
                    /> : <Loading />}
                </div>;
            }
            centerPanel = <div className='center-container'>{this.renderCenter(fund)}</div>;
        } else {
            centerPanel = <div className='text-center'>{i18n('global.insufficient.right')}</div>
        }

        return (
            <PageLayout
                className="arr-actions-page"
                splitter={splitter}
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
            />
        )
    }
};

FundActionPage.propTypes = {};

function mapStateToProps(state) {
    const {arrRegion, splitter, userDetail} = state;
    return {
        arrRegion,
        splitter,
        userDetail,
    }
}

module.exports = connect(mapStateToProps)(FundActionPage);
