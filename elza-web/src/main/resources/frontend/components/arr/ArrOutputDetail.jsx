/**
 * Formulář detailu a editace verze výstupu.
 */

require('./ArrOutputDetail.less');

import React from 'react';
import Utils from "components/Utils.jsx";
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Loading, FundNodesAddForm, AbstractReactComponent} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {fundOutputDetailFetchIfNeeded} from 'actions/arr/fundOutput.jsx'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
var keyModifier = Utils.getKeyModifier()

var keymap = {
    ArrOutputDetail: {
        xxx: keyModifier + 'e',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

var ArrOutputDetail = class ArrOutputDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('trySetFocus', 'handleShortcuts');
    }

    componentDidMount() {
        const {versionId, fundOutputDetail} = this.props
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id))
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, fundOutputDetail} = nextProps
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id))
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        // if (canSetFocus()) {
        //     if (isFocusFor(focus, 'fund-output', 1)) {
        //         this.refs.fundOutputList && this.setState({}, () => {
        //             ReactDOM.findDOMNode(this.refs.fundOutputList).focus()
        //         })
        //         focusWasSet()
        //     }
        // }
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    render() {
        const {fundOutputDetail} = this.props;

        if (fundOutputDetail.id === null) {
            return <div className='arr-output-detail-container'></div>
        }

        if (fundOutputDetail.fetching || !fundOutputDetail.fetched) {
            return <div className='arr-output-detail-container'><Loading/></div>
        }        
        
        return (
            <Shortcuts name='ArrOutputDetail' handler={this.handleShortcuts}>
                <div ref='arr-output-detail-container' className={"partyDetail"}>
                    ........
                </div>
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    // const {splitter, arrRegion, focus, userDetail} = state
    return {
    }
}

ArrOutputDetail.propTypes = {
    fundOutputDetail: React.PropTypes.object.isRequired,
}

ArrOutputDetail.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps)(ArrOutputDetail);
