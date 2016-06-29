/**
 * Formulář detailu a editace verze výstupu.
 */

require('./ArrOutputDetail.less');

import React from 'react';
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import Utils from "components/Utils.jsx";
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {Loading, i18n, OutputSubNodeForm, FundNodesAddForm, FundNodesList, AbstractReactComponent} from 'components/index.jsx';
import {Input} from 'react-bootstrap';
import {fundOutputDetailFetchIfNeeded} from 'actions/arr/fundOutput.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'
import {fundOutputRemoveNodes, fundOutputAddNodes } from 'actions/arr/fundOutput.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
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

        this.bindMethods('trySetFocus', 'handleShortcuts', "handleRemoveNode",
            "handleRenderNodeItem", "handleAddNodes");
    }

    componentDidMount() {
        const {versionId, fundOutputDetail} = this.props;
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.dispatch(outputTypesFetchIfNeeded());

        this.requestData(this.props.versionId, this.props.fundOutputDetail);
        
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, fundOutputDetail} = nextProps;
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.dispatch(outputTypesFetchIfNeeded());
        
        this.requestData(nextProps.versionId, nextProps.fundOutputDetail);
        
        this.trySetFocus(nextProps)
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     */
    requestData(versionId, fundOutputDetail) {
        this.dispatch(descItemTypesFetchIfNeeded());
        if (fundOutputDetail.fetched && !fundOutputDetail.isFetching) {
            this.dispatch(outputFormActions.fundSubNodeFormFetchIfNeeded(versionId, null));
        }
        this.dispatch(refRulDataTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
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

    handleRemoveNode(node) {
        const {fund, fundOutputDetail} = this.props;

        if (confirm(i18n("arr.fund.nodes.deleteNode"))) {
            this.dispatch(fundOutputRemoveNodes(fund.versionId, fundOutputDetail.id, [node.id]))
        }
    }

    handleRenderNodeItem(node) {
        const {readOnly} = this.props

        const refMark = <div className="reference-mark">{createReferenceMarkString(node)}</div>

        var name = node.name ? node.name : <i>{i18n('fundTree.node.name.undefined', node.id)}</i>;
        if (name.length > NODE_NAME_MAX_CHARS) {
            name = name.substring(0, NODE_NAME_MAX_CHARS - 3) + '...'
        }
        name = <div title={name} className="name">{name}</div>

        return (
            <div className="item">
                <div className="item-container">
                    {refMark}
                    {name}
                </div>
                <div className="actions-container">
                    {!readOnly && <Button onClick={this.handleDeleteItem.bind(this, node)}><Icon glyph="fa-trash"/></Button>}
                </div>
            </div>
        )
    }

    handleAddNodes() {
        const {fund, fundOutputDetail} = this.props;

        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesAddForm
                onSubmitForm={(ids, nodes) => {
                    this.dispatch(fundOutputAddNodes(fund.versionId, fundOutputDetail.id, ids))
                }}
            />))
    }

    render() {
        const {fundOutputDetail, outputTypes, templates, fund, versionId, packets, packetTypes, descItemTypes, calendarTypes, rulDataTypes} = this.props;
        
        if (fundOutputDetail.id === null) {
            return <div className='arr-output-detail-container'></div>
        }

        if (!fundOutputDetail.fetched) {
            return <div className='arr-output-detail-container'><Loading/></div>
        }

        var outputType = false;
        if (outputTypes) {
            const index = indexById(outputTypes, fundOutputDetail.outputDefinition.outputTypeId);
            outputType = index !== null ? outputTypes[index].name : false;
        }
        var template = false;
        if (outputTypes) {
            const index = indexById(templates.items, fundOutputDetail.outputDefinition.templateId);
            template = index !== null ? templates.items[index].name : false;
        }

        var form
        if (fundOutputDetail.subNodeForm.fetched && calendarTypes.fetched && descItemTypes.fetched) {
            form = (
                <OutputSubNodeForm
                    versionId={versionId}
                    fundId={fund.id}
                    selectedSubNodeId={fundOutputDetail.outputDefinition.id}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
                    packetTypes={packetTypes}
                    packets={packets}
                    subNodeForm={fundOutputDetail.subNodeForm}
                    closed={fundOutputDetail.lockDate ? true : false}
                    focus={focus}
                />
            )
        } else {
            form = <Loading value={i18n('global.data.loading.form')}/>
        }

        return (
            <Shortcuts name='ArrOutputDetail' handler={this.handleShortcuts}>
                <div className={"arr-output-detail-container"}>
                    <div className="output-definition-commons">
                        <Input type="text" label={i18n('arr.output.name')} disabled value={fundOutputDetail.outputDefinition.name}/>
                        <Input type="text" label={i18n('arr.output.internalCode')} disabled value={fundOutputDetail.outputDefinition.internalCode}/>
                        {template && <Input type="text" label={i18n('arr.output.template')} disabled value={template}/>}
                        {outputType && <Input type="text" label={i18n('arr.output.outputType')} disabled value={outputType}/>}
                    </div>

                    <div className="fund-nodes-container">
                        <h2>{i18n("arr.output.title.nodes")}</h2>
                        <FundNodesList
                            nodes={fundOutputDetail.outputDefinition.nodes}
                            onDeleteNode={this.handleRemoveNode}
                            onAddNode={this.handleAddNodes}
                            readOnly={fundOutputDetail.lockDate ? true : false}
                        />
                    </div>

                    {form}
                </div>
            </Shortcuts>
        )
    }
};

function mapStateToProps(state) {
    const {focus, userDetail} = state
    return {
        outputTypes: state.refTables.outputTypes.items,
        focus,
        userDetail,
    }    
}

ArrOutputDetail.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fund: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    templates: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
    fundOutputDetail: React.PropTypes.object.isRequired,
};

ArrOutputDetail.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps)(ArrOutputDetail);
