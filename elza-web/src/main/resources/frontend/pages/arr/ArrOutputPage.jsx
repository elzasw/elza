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
import {ListBox, Ribbon, RibbonGroup, Icon, i18n, ArrOutputDetail, AddOutputForm, AbstractReactComponent} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {canSetFocus, setFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {fundOutputFetchIfNeeded, fundOutputSelectOutput, fundOutputCreate} from 'actions/arr/fundOutput.jsx'
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
            'handleAddOutput');
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
    
    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        const fund = this.getActiveFund(this.props)

        var altActions = [];
        altActions.push(
            <Button key="add-fa" onClick={this.handleAddOutput}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.output.add')}</span></div></Button>
        )
        
        var itemActions = [];

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="large">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="large">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr altSection={altSection} itemSection={itemSection}/>
        )
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
                {item.lockChange ? <div>{Utils.dateTimeToString(new Date(item.lockChange))}</div> : <div>&nbsp;</div>}
            </div>
        )
    }

    handleSelect(item) {
        const fund = this.getActiveFund(this.props)
        this.dispatch(fundOutputSelectOutput(fund.versionId, item.id))
    }

    render() {
        const {focus, splitter, arrRegion} = this.props;

        const fund = this.getActiveFund(this.props)
        var leftPanel
        let centerPanel

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
        } else {
            centerPanel = <div className="fund-noselect">{i18n('arr.fund.noselect')}</div>
        }

        return (
            <Shortcuts name='ArrOutput' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    className='arr-output-page'
                    ribbon={this.buildRibbon()}
                    leftPanel={leftPanel}
                    centerPanel={centerPanel}
                    rightPanel={null}
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
