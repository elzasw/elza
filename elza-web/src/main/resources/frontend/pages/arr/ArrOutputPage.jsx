/**
 * Stránka archivních pomůcek.
 */

require('./ArrOutputPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Tabs, Icon, Ribbon, i18n} from 'components';
import {FundExtendedView, FundForm, BulkActionsDialog, RibbonMenu, RibbonGroup, RibbonSplit,
    ToggleContent, AbstractReactComponent, ModalDialog, NodeTabs, FundTreeTabs, ListBox2, LazyListBox,
    VisiblePolicyForm, Loading, FundPackets} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {showRegisterJp} from 'actions/arr/fund'
import {scopesDirty} from 'actions/refTables/scopesData'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation'
import {packetsFetchIfNeeded} from 'actions/arr/packets'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes'
import {developerNodeScenariosRequest} from 'actions/global/developer'
import {Utils} from 'components'
import {barrier} from 'components/Utils';
import {isFundRootId} from 'components/arr/ArrUtils';
import {setFocus} from 'actions/global/focus'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy'
import {propsEquals} from 'components/Utils'
import {fundSelectSubNode} from 'actions/arr/nodes'
import {createFundRoot} from 'components/arr/ArrUtils.jsx'
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

import {fundOutputFetchIfNeeded} from 'actions/arr/fundOutput.jsx'

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

        this.bindMethods('getActiveFund');
    }

    componentDidMount() {
        const fund = this.getActiveFund(this.props)
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
        }
    }

    componentWillReceiveProps(nextProps) {
        const fund = this.getActiveFund(nextProps)
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
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
                this.dispatch(setFocus('arr', 1))
                break
            case 'area2':
                this.dispatch(setFocus('arr', 2))
                break
            case 'area3':
                this.dispatch(setFocus('arr', 3))
                break
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        const {arrRegion} = this.props;

        var altActions = [];

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

    render() {
        const {focus, splitter, arrRegion} = this.props;

        return (
            <Shortcuts name='ArrOutput' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    className='arr-output-page'
                    ribbon={this.buildRibbon()}
                    leftPanel={null}
                    centerPanel={null}
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
