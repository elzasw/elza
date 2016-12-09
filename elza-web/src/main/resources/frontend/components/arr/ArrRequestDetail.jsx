/**
 * Detail požadavku na externí systém.
 */

import React from 'react';
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import Utils, {dateTimeToString} from "components/Utils.jsx";
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {
    Loading,
    i18n,
    OutputSubNodeForm,
    FundNodesSelectForm,
    FundNodesList,
    AbstractReactComponent,
    FormInput
} from 'components/index.jsx';
import {fundOutputDetailFetchIfNeeded, fundOutputEdit} from 'actions/arr/fundOutput.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'
import {fundOutputRemoveNodes, fundOutputAddNodes} from 'actions/arr/fundOutput.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import * as digitizationActions from 'actions/arr/digitizationActions';
import RequestInlineForm from "./RequestInlineForm";
import {getRequestType} from './ArrUtils.jsx'

const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
const keyModifier = Utils.getKeyModifier();


const keymap = {
    ArrRequestDetail: {
        xxx: keyModifier + 'e',
    },
};
const shortcutManager = new ShortcutsManager(keymap);

/**
 * Formulář detailu požadavku na digitalizaci.
 */
class ArrRequestDetail extends AbstractReactComponent {

    static PropTypes = {
        versionId: React.PropTypes.number.isRequired,
        fund: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired,
        ArrRequestDetail: React.PropTypes.object.isRequired,
    };

    static childContextTypes = {
        shortcuts: React.PropTypes.object.isRequired
    };

    componentDidMount() {
        const {versionId, requestDetail} = this.props;

        requestDetail.id !== null && this.dispatch(digitizationActions.fetchDetailIfNeeded(versionId, requestDetail.id));

        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, requestDetail} = nextProps;

        requestDetail.id !== null && this.dispatch(digitizationActions.fetchDetailIfNeeded(versionId, requestDetail.id));

        this.trySetFocus(nextProps)
    }

    trySetFocus = (props) => {
        //let {focus} = props;

        // if (canSetFocus()) {
        //     if (isFocusFor(focus, 'fund-output', 1)) {
        //         this.refs.fundOutputList && this.setState({}, () => {
        //             ReactDOM.findDOMNode(this.refs.fundOutputList).focus()
        //         })
        //         focusWasSet()
        //     }
        // }
    };

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
    };

    getChildContext() {
        return {shortcuts: shortcutManager};
    }

    handleSaveRequest = (data) => {
        const {versionId, requestDetail} = this.props;
        this.dispatch(digitizationActions.requestEdit(versionId, requestDetail.id, data));
    }

    handleAddNodes = () => {
        const {versionId, requestDetail} = this.props;

        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesSelectForm
                onSubmitForm={(ids, nodes) => {
                    this.dispatch(digitizationActions.addNodes(versionId, requestDetail, ids))
                }}
            />))
    };

    handleRemoveNode = (node) => {
        const {versionId, requestDetail} = this.props;

        if (confirm(i18n("arr.fund.nodes.deleteNode"))) {
            this.dispatch(digitizationActions.removeNode(versionId, requestDetail, node.id))
        }
    };

    render() {
        const {requestDetail} = this.props;

        let form;
        if (requestDetail.id === null) {
            form = <div className="unselected-msg">
                <div className="title">{i18n('arr.request.noSelection.title')}</div>
                <div className="msg-text">{i18n('arr.request.noSelection.message')}</div>
            </div>;
        } else if (requestDetail.fetched && !requestDetail.isFetching) {
            const digReq = requestDetail.data;
            const reqType = getRequestType(digReq);
            form = (
                <div>
                    <h2>{i18n("arr.request.title.digitizationRequest")}</h2>
                    <div>
                        <label>{i18n("arr.request.title.created")}</label> {dateTimeToString(new Date(digReq.create))}
                    </div>
                    <div>
                        <label>{i18n("arr.request.title.type")}</label> {i18n("arr.request.title.type." + reqType)}
                    </div>

                    <RequestInlineForm
                        disabled={false}
                        initData={digReq}
                        onSave={this.handleSaveRequest}
                    />

                    {reqType === REQ_DIGITIZATION_REQUEST && <div>
                        <label className="control-label">{i18n("arr.request.title.nodes")}</label>
                        <FundNodesList
                            nodes={digReq.nodes}
                            onDeleteNode={this.handleRemoveNode}
                            onAddNode={this.handleAddNodes}
                            readOnly={false}
                        />
                    </div>}
                </div>
            )
        } else {
            form = <Loading value={i18n('global.data.loading')}/>;
        }

        return <Shortcuts name='ArrRequestDetail' handler={this.handleShortcuts}>
            <div className='arr-request-detail-container'>
                {form}
            </div>
        </Shortcuts>;
    }
}

function mapStateToProps(state) {
    const {focus, userDetail} = state;
    return {
        focus,
        userDetail,
    }
}

export default connect(mapStateToProps)(ArrRequestDetail);
