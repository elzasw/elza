import React from 'react';
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx';
import {connect} from 'react-redux';
import {addShortcutManager} from 'components/Utils.jsx';
import {AbstractReactComponent, FormInput, HorizontalLoader, i18n} from 'components/shared';
import {
    fundOutputAddNodes,
    fundOutputDetailFetchIfNeeded,
    fundOutputEdit,
    fundOutputRemoveNodes,
} from '../../actions/arr/fundOutput.jsx';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx';
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx';
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx';
import {outputFormActions} from 'actions/arr/subNodeForm.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import OutputInlineForm from 'components/arr/OutputInlineForm.jsx';
import {PropTypes} from 'prop-types';
import './ArrOutputDetail.scss';
import {Shortcuts} from 'react-shortcuts';
import OutputSubNodeForm from './OutputSubNodeForm';
import FundNodesList from './FundNodesList';
import FundNodesSelectForm from './FundNodesSelectForm';
import defaultKeymap from './ArrOutputDetailKeymap.jsx';
import FundOutputFiles from './FundOutputFiles';
import ToggleContent from '../shared/toggle-content/ToggleContent';

const OutputState = {
    OPEN: 'OPEN',
    COMPUTING: 'COMPUTING',
    GENERATING: 'GENERATING',
    FINISHED: 'FINISHED',
    OUTDATED: 'OUTDATED',
    ERROR: 'ERROR', /// Pomocný stav websocketu
};

/**
 * Formulář detailu a editace verze výstupu.
 */
class ArrOutputDetail extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        fund: PropTypes.object.isRequired,
        calendarTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        templates: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        closed: PropTypes.bool.isRequired,
        readMode: PropTypes.bool.isRequired,
        fundOutputDetail: PropTypes.object.isRequired,
    };

    componentDidMount() {
        const {versionId, fundOutputDetail} = this.props;
        fundOutputDetail.id !== null &&
            this.props.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.props.dispatch(outputTypesFetchIfNeeded());

        this.requestData(this.props.versionId, this.props.fundOutputDetail);

        this.trySetFocus(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {versionId, fundOutputDetail} = nextProps;
        fundOutputDetail.id !== null &&
            this.props.dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        this.props.dispatch(outputTypesFetchIfNeeded());

        this.requestData(nextProps.versionId, nextProps.fundOutputDetail);

        this.trySetFocus(nextProps);
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     * @param fundOutputDetail {Object} store
     */
    requestData(versionId, fundOutputDetail) {
        this.props.dispatch(descItemTypesFetchIfNeeded());
        if (fundOutputDetail.fetched && !fundOutputDetail.isFetching) {
            this.props.dispatch(outputFormActions.fundSubNodeFormFetchIfNeeded(versionId, null));
        }
        this.props.dispatch(refRulDataTypesFetchIfNeeded());
        this.props.dispatch(calendarTypesFetchIfNeeded());
    }

    trySetFocus = props => {
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

    handleShortcuts = action => {
        console.log('#handleShortcuts', '[' + action + ']', this);
    };

    handleSaveOutput = data => {
        const {fund, fundOutputDetail} = this.props;
        return this.props.dispatch(fundOutputEdit(fund.versionId, fundOutputDetail.id, data));
    };

    handleRemoveNode = node => {
        const {fund, fundOutputDetail} = this.props;

        if (window.confirm(i18n('arr.fund.nodes.deleteNode'))) {
            this.props.dispatch(fundOutputRemoveNodes(fund.versionId, fundOutputDetail.id, [node.id]));
        }
    };

    handleAddNodes = () => {
        const {fund, fundOutputDetail} = this.props;

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.nodes.title.select'),
                <FundNodesSelectForm
                    onSubmitForm={(ids, nodes) => {
                        this.props.dispatch(fundOutputAddNodes(fund.versionId, fundOutputDetail.id, ids));
                    }}
                />,
            ),
        );
    };

    isEditable = (item = this.props.fundOutputDetail) => {
        return !item.lockDate && item.state === OutputState.OPEN;
    };

    renderOutputFiles() {
        const {fundOutputDetail, versionId, fund} = this.props;
        const {
            fundOutput: {fundOutputFiles},
        } = fund;

        if (fundOutputDetail.outputResultId === null) {
            return null;
        }

        return (
            <FundOutputFiles
                ref="fundOutputFiles"
                versionId={versionId}
                outputResultId={fundOutputDetail.outputResultId}
                fundOutputFiles={fundOutputFiles}
            />
        );
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
        } = this.props;

        if (fundOutputDetail.id === null) {
            return (
                <div className="arr-output-detail-container">
                    <div className="unselected-msg">
                        <div className="title">{i18n('arr.output.noSelection.title')}</div>
                        <div className="msg-text">{i18n('arr.output.noSelection.message')}</div>
                    </div>
                </div>
            );
        }

        const fetched =
            fundOutputDetail.fetched &&
            fundOutputDetail.subNodeForm.fetched &&
            calendarTypes.fetched &&
            descItemTypes.fetched;
        if (!fetched) {
            return <HorizontalLoader />;
        }

        let form = (
            <OutputSubNodeForm
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
            />
        );

        return (
            <Shortcuts
                name="ArrOutputDetail"
                className={'arr-output-detail-container'}
                style={{height: '100%'}}
                handler={this.handleShortcuts}
            >
                <div className="output-definition-commons">
                    <OutputInlineForm
                        disabled={closed || readMode || !this.isEditable()}
                        initialValues={fundOutputDetail}
                        onSave={this.handleSaveOutput}
                    />
                    {fundOutputDetail.error && (
                        <div>
                            <FormInput
                                as="textarea"
                                value={fundOutputDetail.error}
                                disabled
                                label={i18n('arr.output.title.error')}
                            />
                        </div>
                    )}
                </div>
                <div>
                    <label className="control-label">{i18n('arr.output.title.nodes')}</label>
                    <FundNodesList
                        nodes={fundOutputDetail.nodes}
                        onDeleteNode={this.handleRemoveNode}
                        onAddNode={this.handleAddNodes}
                        readOnly={closed || readMode || !this.isEditable()}
                    />
                </div>
                <hr className="small" />
                {this.renderOutputFiles()}
                <h4 className={'desc-items-title'}>{i18n('developer.title.descItems')}</h4>
                <ToggleContent opened={false} withText>
                    {form}
                </ToggleContent>
            </Shortcuts>
        );
    }
}

function mapStateToProps(state) {
    const {focus, userDetail} = state;
    return {
        outputTypes: state.refTables.outputTypes.items,
        focus,
        userDetail,
    };
}

export default connect(mapStateToProps)(ArrOutputDetail);
