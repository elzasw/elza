/**
 * Stránka výstupů.
 */

require('./ArrOutputPage.less');

import React from 'react';
import Utils from "components/Utils.jsx";
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {ListBox, Ribbon, Loading, RibbonGroup, FundNodesAddForm, Icon, FundNodesList, i18n, ArrOutputDetail, AddOutputForm, AbstractReactComponent} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {canSetFocus, setFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {fundOutputFetchIfNeeded, fundOutputRemoveNodes, fundOutputSelectOutput, fundOutputCreate, fundOutputUsageEnd, fundOutputDelete, fundOutputAddNodes } from 'actions/arr/fundOutput.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {fundActionFormShow, fundActionFormChange} from 'actions/arr/fundAction.jsx'
import {routerNavigate} from 'actions/router.jsx'
var classNames = require('classnames');
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');

var keyModifier = Utils.getKeyModifier()

var keymap = {
    ArrOutput: {
        area1: keyModifier + '1',
        area2: keyModifier + '2',
        area3: keyModifier + '3',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

var ArrOutputPage = class ArrOutputPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getActiveFund', 'renderListItem', 'handleSelect', 'trySetFocus', 'handleShortcuts',
            'handleAddOutput', 'handleAddNodes', 'handleUsageEnd', 'handleDelete', 'handleRemoveNode',
            'handleBulkActions');
    }

    componentDidMount() {
        const fund = this.getActiveFund(this.props)
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
        }
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const fund = this.getActiveFund(nextProps)
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
        }
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'fund-output', 1)) {
                this.refs.fundOutputList && this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refs.fundOutputList).focus()
                })
                focusWasSet()
            }
        }
    }

    getActiveFund(props) {
        var arrRegion = props.arrRegion;
        var activeFund = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
        }
        return activeFund
    }

    requestValidationData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, false))
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'area1':
                this.dispatch(setFocus('fund-output', 1))
                break
            case 'area2':
                this.dispatch(setFocus('fund-output', 2))
                break
            case 'area3':
                this.dispatch(setFocus('fund-output', 3))
                break
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleAddOutput() {
        const fund = this.getActiveFund(this.props)

        this.dispatch(modalDialogShow(this, i18n('arr.output.title.add'),
            <AddOutputForm onSubmitForm={(data) => {this.dispatch(fundOutputCreate(fund.versionId, data))}}/>));
    }

    handleAddNodes() {
        const fund = this.getActiveFund(this.props)
        const fundOutputDetail = fund.fundOutput.fundOutputDetail

        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesAddForm
                onSubmitForm={(ids, nodes) => {
                    this.dispatch(fundOutputAddNodes(fund.versionId, fundOutputDetail.id, ids))
                }}
                />))
    }

    handleRemoveNode(node) {
        const fund = this.getActiveFund(this.props)
        const fundOutputDetail = fund.fundOutput.fundOutputDetail
        this.dispatch(fundOutputRemoveNodes(fund.versionId, fundOutputDetail.id, [node.id]))
    }

    handleBulkActions() {
        const fund = this.getActiveFund(this.props)
        const fundOutputDetail = fund.fundOutput.fundOutputDetail

        this.dispatch(fundActionFormShow())
        this.dispatch(fundActionFormChange({nodeList: fundOutputDetail.namedOutput.nodes}))
        this.dispatch(routerNavigate('/arr/actions'));
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        const {userDetail} = this.props

        const fund = this.getActiveFund(this.props)

        var altActions = [];
        if (fund) {
            if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_OUTPUT_WR_ALL, {type: perms.FUND_OUTPUT_WR, fundId: fund.id})) {
                altActions.push(
                    <Button key="add-output" onClick={this.handleAddOutput}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.output.add')}</span></div></Button>
                )
            }
        }

        var itemActions = [];
        if (fund) {
            const fundOutputDetail = fund.fundOutput.fundOutputDetail

            if (fundOutputDetail.id !== null && fundOutputDetail.fetched && !fundOutputDetail.isFetching) {
                if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_OUTPUT_WR_ALL, {type: perms.FUND_OUTPUT_WR, fundId: fund.id})) {
                    if (!fundOutputDetail.lockDate) {
                        itemActions.push(
                            <Button key="add-fund-nodes" onClick={this.handleAddNodes}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.output.nodes.add')}</span></div></Button>
                        )
                        itemActions.push(
                            <Button key="fund-output-usage-end" onClick={this.handleUsageEnd}><Icon glyph="fa-clock-o" /><div><span className="btnText">{i18n('ribbon.action.arr.output.usageEnd')}</span></div></Button>
                        )
                    }
                    itemActions.push(
                        <Button key="fund-output-delete" onClick={this.handleDelete}><Icon glyph="fa-trash"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.output.delete')}</span></div>
                        </Button>
                    )
                }

                if (fundOutputDetail.namedOutput.nodes.length > 0) {
                    if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id})) { // právo na hromadné akce
                        itemActions.push(
                            <Button key="fund-output-bulk-actions" onClick={this.handleBulkActions}><Icon glyph="fa-cog" /><div><span className="btnText">{i18n('ribbon.action.arr.output.bulkActions')}</span></div></Button>
                        )
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
            <Ribbon arr fundId={fund ? fund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    }

    handleUsageEnd() {
        const fund = this.getActiveFund(this.props)
        const fundOutputDetail = fund.fundOutput.fundOutputDetail
        if (confirm(i18n('arr.output.usageEnd.confirm'))) {
            this.dispatch(fundOutputUsageEnd(fund.versionId, fundOutputDetail.id))
        }
    }

    handleDelete() {
        const fund = this.getActiveFund(this.props)
        const fundOutputDetail = fund.fundOutput.fundOutputDetail
        if (confirm(i18n('arr.output.delete.confirm'))) {
            this.dispatch(fundOutputDelete(fund.versionId, fundOutputDetail.id))
        }
    }

    renderListItem(item, isActive, index) {
        const fund = this.getActiveFund(this.props)
        const fundOutput = fund.fundOutput

        var temporaryChanged = false

        const currTemporary = item.namedOutput.temporary
        var prevTemporary = index - 1 >= 0 ? fundOutput.outputs[index - 1].namedOutput.temporary : false

        var cls = {
            item: true,
            'temporary-splitter': currTemporary !== prevTemporary
        }

        return (
            <div className={classNames(cls)}>
                <div className='name'>{item.namedOutput.name}</div>
                {item.lockDate ? <div>{Utils.dateTimeToString(new Date(item.lockDate))}</div> : <div>&nbsp;</div>}
            </div>
        )
    }

    handleSelect(item) {
        const fund = this.getActiveFund(this.props)
        this.dispatch(fundOutputSelectOutput(fund.versionId, item.id))
    }

    render() {
        const {focus, splitter, userDetail} = this.props;

        const fund = this.getActiveFund(this.props)
        var leftPanel, rightPanel
        let centerPanel

        if (userDetail.hasArrOutputPage(fund ? fund.id : null)) { // má právo na tuto stránku
            if (fund) {
                const fundOutput = fund.fundOutput
    
                var activeIndex
                if (fundOutput.fundOutputDetail.id !== null) {
                    activeIndex = indexById(fundOutput.outputs, fundOutput.fundOutputDetail.id)
                }
                leftPanel = (
                    <div className="fund-output-list-container">
                        <ListBox
                            className='fund-output-listbox'
                            ref='fundOutputList'
                            items={fundOutput.outputs}
                            activeIndex={activeIndex}
                            renderItemContent={this.renderListItem}
                            onFocus={this.handleSelect}
                            onSelect={this.handleSelect}
                        />
                    </div>
                )
    
                centerPanel = <ArrOutputDetail
                    versionId={fund.versionId}
                    fundOutputDetail={fundOutput.fundOutputDetail}
                    />
    
                const fundOutputDetail = fund.fundOutput.fundOutputDetail
                if (fundOutputDetail.id !== null && fundOutputDetail.fetched) {
                    rightPanel = (
                        <div className="fund-nodes-container">
                            <FundNodesList
                                nodes={fundOutputDetail.namedOutput.nodes}
                                onDeleteNode={this.handleRemoveNode}
                                readOnly={fundOutputDetail.lockDate ? true : false}
                                />
                        </div>
                    )
                }
            } else {
                centerPanel = <div className="fund-noselect">{i18n('arr.fund.noselect')}</div>
            }
        } else {
            centerPanel = <div>{i18n('global.insufficient.right')}</div>
        }

        return (
            <Shortcuts name='ArrOutput' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    className='arr-output-page'
                    ribbon={this.buildRibbon()}
                    leftPanel={leftPanel}
                    centerPanel={centerPanel}
                    rightPanel={rightPanel}
                />
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, focus, userDetail} = state
    return {
        splitter,
        arrRegion,
        focus,
        userDetail,
    }
}

ArrOutputPage.propTypes = {
    splitter: React.PropTypes.object.isRequired,
    arrRegion: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
}

ArrOutputPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(ArrOutputPage);
