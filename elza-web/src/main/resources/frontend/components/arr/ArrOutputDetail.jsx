import React from 'react';
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {addShortcutManager} from "components/Utils.jsx";
import {HorizontalLoader, i18n, AbstractReactComponent, FormInput} from 'components/shared';
import {fundOutputDetailFetchIfNeeded, fundOutputEdit} from 'actions/arr/fundOutput.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'
import {fundOutputRemoveNodes, fundOutputAddNodes } from 'actions/arr/fundOutput.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import OutputInlineForm from 'components/arr/OutputInlineForm.jsx'
import {PropTypes} from 'prop-types';
import './ArrOutputDetail.less';
import {Shortcuts} from 'react-shortcuts';
import OutputSubNodeForm from "./OutputSubNodeForm";
import FundNodesList from "./FundNodesList";
import FundNodesSelectForm from "./FundNodesSelectForm";
import defaultKeymap from './ArrOutputDetailKeymap.jsx';
import FundOutputFiles from "./FundOutputFiles";
import ToggleContent from "../shared/toggle-content/ToggleContent";
import {ApScopeVO, ArrOutputVO} from "../../typings/Outputs";
import {AppFetchingStore} from "../../typings/globals";
import ScopeList from './ScopeList';
import ScopeField from "../admin/ScopeField";
import * as scopeActions from "../../actions/scopes/scopes";
import storeFromArea from "../../shared/utils/storeFromArea";
import {WebApi} from "actions/index";

const OutputState = {
    OPEN: 'OPEN',
    COMPUTING: 'COMPUTING',
    GENERATING: 'GENERATING',
    FINISHED: 'FINISHED',
    OUTDATED: 'OUTDATED',
    ERROR: 'ERROR' /// Pomocný stav websocketu
};

type ComponentProps = {
    versionId: number;
    fund: any;
    calendarTypes: any;
    descItemTypes: any;
    templates: any;
    rulDataTypes: any;
    closed: boolean;
    readMode: boolean;
    fundOutputDetail: ArrOutputVO & AppFetchingStore & {subNodeForm: any};
};

type ConnectedProps = {
    outputTypes: any;
    focus: any;
    userDetail: any;
};

type Props = ComponentProps & ConnectedProps;

/**
 * Formulář detailu a editace verze výstupu.
 */
class ArrOutputDetail extends AbstractReactComponent<Props> {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    props: Props;
    componentWillMount(){
        addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    static propTypes = {
        versionId: React.PropTypes.number.isRequired,
        fund: React.PropTypes.object.isRequired,
        calendarTypes: React.PropTypes.object.isRequired,
        descItemTypes: React.PropTypes.object.isRequired,
        templates: React.PropTypes.object.isRequired,
        rulDataTypes: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired,
        closed: React.PropTypes.bool.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        fundOutputDetail: React.PropTypes.object.isRequired,
    };

    componentDidMount() {
        const {versionId, fundOutputDetail} = this.props;
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.dispatch(outputTypesFetchIfNeeded());
        this.props.dispatch(scopeActions.scopesListFetchIfNeeded());
        this.requestData(this.props.versionId, this.props.fundOutputDetail);

        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, fundOutputDetail} = nextProps;
        fundOutputDetail.id !== null && this.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.dispatch(outputTypesFetchIfNeeded());
        nextProps.dispatch(scopeActions.scopesListFetchIfNeeded());

        this.requestData(nextProps.versionId, nextProps.fundOutputDetail);

        this.trySetFocus(nextProps)
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     * @param fundOutputDetail {Object} store
     */
    requestData(versionId, fundOutputDetail) {
        this.dispatch(descItemTypesFetchIfNeeded());
        if (fundOutputDetail.fetched && !fundOutputDetail.isFetching) {
            this.dispatch(outputFormActions.fundSubNodeFormFetchIfNeeded(versionId, null));
        }
        this.dispatch(refRulDataTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
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

    handleSaveOutput = (data: ApScopeVO) => {
        const {fund, fundOutputDetail} = this.props;
        this.dispatch(fundOutputEdit(fund.versionId, fundOutputDetail.id, data));
    };

    handleRemoveNode = (node) => {
        const {fund, fundOutputDetail} = this.props;

        if (confirm(i18n("arr.fund.nodes.deleteNode"))) {
            this.dispatch(fundOutputRemoveNodes(fund.versionId, fundOutputDetail.id, [node.id]))
        }
    };

    handleRemoveScope = (scope: ApScopeVO) => {
        const {fundOutputDetail} = this.props;

        if (confirm(i18n("arr.fund.nodes.deleteNode"))) {
            WebApi.deleteRestrictedScope(fundOutputDetail.id, scope.id);
        }
    };


    handleAddScope = (scope: ApScopeVO) => {
        const {fundOutputDetail} = this.props;

        WebApi.addRestrictedScope(fundOutputDetail.id, scope.id);
        // Zbytek zařídí websocket
    };

    handleRenderNodeItem = (node) => {
        const {readOnly} = this.props;

        const refMark = <div className="reference-mark">{createReferenceMarkString(node)}</div>;

        let name = node.name ? node.name : <i>{i18n('fundTree.node.name.undefined', node.id)}</i>;
        if (name.length > NODE_NAME_MAX_CHARS) {
            name = name.substring(0, NODE_NAME_MAX_CHARS - 3) + '...'
        }
        name = <div title={name} className="name">{name}</div>;

        return <div className="item">
            <div className="item-container">
                {refMark}
                {name}
            </div>
            <div className="actions-container">
                {!readOnly && <Button onClick={this.handleDeleteItem.bind(this, node)}><Icon glyph="fa-trash"/></Button>}
            </div>
        </div>;
    };

    handleAddNodes = () => {
        const {fund, fundOutputDetail} = this.props;

        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesSelectForm
                onSubmitForm={(ids, nodes) => {
                    this.dispatch(fundOutputAddNodes(fund.versionId, fundOutputDetail.id, ids))
                }}
            />))
    };

    isEditable = (item = this.props.fundOutputDetail) => {
        return !item.lockDate && item.state === OutputState.OPEN
    };

    renderOutputFiles() {
        const {fundOutputDetail, versionId, fund} = this.props;
        const {fundOutput : {fundOutputFiles}} = fund;

        if (fundOutputDetail.outputResultId === null) {
            return null;
        }

        return <FundOutputFiles
            ref="fundOutputFiles"
            versionId={versionId}
            outputResultId={fundOutputDetail.outputResultId}
            fundOutputFiles={fundOutputFiles}
        />
    }

    render() {
        const {
            fundOutputDetail,
            focus,
            fund,
            versionId,
            descItemTypes,
            calendarTypes,
            rulDataTypes,
            closed,
            readMode,
            scopeList
        } = this.props;

        if (fundOutputDetail.id === null) {
            return <div className='arr-output-detail-container'>
                        <div className="unselected-msg">
                            <div className="title">{i18n('arr.output.noSelection.title')}</div>
                            <div className="msg-text">{i18n('arr.output.noSelection.message')}</div>
                        </div>
                    </div>
        }

        const fetched = fundOutputDetail.fetched && fundOutputDetail.subNodeForm.fetched && calendarTypes.fetched && descItemTypes.fetched;
        if (!fetched) {
            return <HorizontalLoader/>
        }

        let form= <OutputSubNodeForm
            versionId={versionId}
            fundId={fund.id}
            selectedSubNodeId={fundOutputDetail.id}
            rulDataTypes={rulDataTypes}
            calendarTypes={calendarTypes}
            descItemTypes={descItemTypes}
            subNodeForm={fundOutputDetail.subNodeForm}
            closed={!this.isEditable()}
            focus={focus}
            readMode={closed || readMode}
        />;

        let readonly = closed || readMode || !this.isEditable();

        const existingScopes = (fundOutputDetail.scopes || []).map(i => i.id);
        const connectableScopes = scopeList.rows && scopeList.rows.filter(s => existingScopes.indexOf(s.id) === -1);

        return <Shortcuts name='ArrOutputDetail' className={"arr-output-detail-container"} style={{height: "100%"}} handler={this.handleShortcuts}>
            <div className="output-definition-commons">
                <OutputInlineForm
                    disabled={readonly}
                    initData={fundOutputDetail}
                    onSave={this.handleSaveOutput}
                />
                {fundOutputDetail.error && <div>
                    <FormInput componentClass="textarea" value={fundOutputDetail.error} disabled label={i18n('arr.output.title.error')}/>
                </div>}
            </div>
            <div>
                <label className="control-label">{i18n("arr.output.title.nodes")}</label>
                <FundNodesList
                    nodes={fundOutputDetail.nodes}
                    onDeleteNode={this.handleRemoveNode}
                    onAddNode={this.handleAddNodes}
                    readOnly={readonly}
                />
            </div>
            <div>
                <label className="control-label">{i18n("arr.output.title.scopes")}</label>
                {!readonly && <ScopeField scopes={connectableScopes} onChange={this.handleAddScope} value={null} />}
                <ScopeList
                    scopes={fundOutputDetail.scopes || []}
                    onRemove={this.handleRemoveScope}
                    readOnly={readonly}
                />
            </div>
            <hr className="small"/>
            {this.renderOutputFiles()}
            <h4 className={"desc-items-title"}>{i18n("developer.title.descItems")}</h4>
            <ToggleContent opened={!readonly} withText>
                {form}
            </ToggleContent>
        </Shortcuts>;
    }
}

function mapStateToProps(state) {
    const {focus, userDetail} = state;
    return {
        outputTypes: state.refTables.outputTypes.items,
        focus,
        userDetail,
        scopeList: storeFromArea(state, scopeActions.AREA_SCOPE_LIST),
    }
}

export default connect(mapStateToProps)(ArrOutputDetail);
