/**
 * Formulář detailu a editace verze výstupu.
 */

require('./ArrOutputDetail.less');

import React from 'react';
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import Utils from "components/Utils.jsx";
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {Loading, i18n, AbstractReactComponent} from 'components/index.jsx';
import {Input} from 'react-bootstrap';
import {fundOutputDetailFetchIfNeeded} from 'actions/arr/fundOutput.jsx'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
var keyModifier = Utils.getKeyModifier()

var keymap = {
    ArrOutputDetail: {
        xxx: keyModifier + 'e',
    },
};
var shortcutManager = new ShortcutsManager(keymap);

var ArrOutputDetail = class ArrOutputDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('trySetFocus', 'handleShortcuts');
    }

    componentDidMount() {
        const {versionId, fundOutputDetail} = this.props;
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.dispatch(outputTypesFetchIfNeeded());
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, fundOutputDetail} = nextProps;
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.dispatch(outputTypesFetchIfNeeded());
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props;

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
        const {fundOutputDetail, outputTypes} = this.props;
        
        if (fundOutputDetail.id === null) {
            return <div className='arr-output-detail-container'></div>
        }

        if (!fundOutputDetail.fetched) {
            return <div className='arr-output-detail-container'><Loading/></div>
        }

        var outputType = false;
        if (outputTypes) {
            const index = indexById(outputTypes,fundOutputDetail.outputDefinition.outputTypeId);
            outputType = index !== null ? outputTypes[index].name : false;
        }

        return (
            <Shortcuts name='ArrOutputDetail' handler={this.handleShortcuts}>
                <div ref='arr-output-detail-container' className={"partyDetail"}>
                    <Input type="text" label={i18n('arr.output.name')} disabled value={fundOutputDetail.outputDefinition.name}/>
                    <Input type="text" label={i18n('arr.output.internalCode')} disabled value={fundOutputDetail.outputDefinition.internalCode}/>
                    {outputType && <Input type="text" label={i18n('arr.output.outputType')} disabled value={outputType}/>}
                </div>
            </Shortcuts>
        )
    }
};

function mapStateToProps(state) {
    // const {splitter, arrRegion, focus, userDetail} = state
    return {
        outputTypes: state.refTables.outputTypes.items
    }
}

ArrOutputDetail.propTypes = {
    fundOutputDetail: React.PropTypes.object.isRequired,
};

ArrOutputDetail.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps)(ArrOutputDetail);
