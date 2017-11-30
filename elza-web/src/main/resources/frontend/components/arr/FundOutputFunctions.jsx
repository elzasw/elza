import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, ListBox, StoreHorizontalLoader, HorizontalLoader, FormInput} from 'components/shared';
import {fetchFundOutputFunctionsIfNeeded, fundOutputFunctionsFilterByState, fundOutputActionRun, fundOutputActionInterrupt} from 'actions/arr/fundOutputFunctions.jsx'
import {fundActionFetchConfigIfNeeded} from 'actions/arr/fundAction.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {dateTimeToString} from 'components/Utils.jsx';
import './FundOutputFunctions.less'
import {actionStateTranslation} from "../../actions/arr/fundAction";

const ACTION_RUNNING_STATE = ['RUNNING', 'WAITING', 'PLANNED'];
const ACTION_NOT_RUNNING_STATE = ['FINISHED', 'ERROR', 'INTERRUPTED', 'OUTDATED'];

const OutputState = {
    OPEN: 'OPEN',
    COMPUTING: 'COMPUTING',
    GENERATING: 'GENERATING',
    FINISHED: 'FINISHED',
    OUTDATED: 'OUTDATED',
    ERROR: 'ERROR' /// Pomocný stav websocketu
};

/**
 * Správa souborů.
 */
class FundOutputFunctions extends AbstractReactComponent {

    state = {};

    static PropTypes = {
        actionConfig: React.PropTypes.array,
        outputId: React.PropTypes.number.isRequired,
        versionId: React.PropTypes.number.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        outputState: React.PropTypes.string.isRequired,
        fundOutputFunctions: React.PropTypes.object.isRequired,
    };

    componentDidMount() {
        const {versionId, outputId} = this.props;
        this.dispatch(fetchFundOutputFunctionsIfNeeded(versionId, outputId));
        this.dispatch(fundActionFetchConfigIfNeeded(versionId));
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, outputId} = this.props;
        this.dispatch(fetchFundOutputFunctionsIfNeeded(versionId, outputId));
        this.dispatch(fundActionFetchConfigIfNeeded(versionId));
    }

    handleStateSearch = (state) => {
        const {versionId} = this.props;
        this.dispatch(fundOutputFunctionsFilterByState(versionId, state));
    };

    getConfigByCode = (code) => {
        const {actionConfig} = this.props;
        const index = indexById(actionConfig, code, 'code');
        if (index !== null) {
            return actionConfig[index];
        }
        return null;
    };

    handleActionRun = (code) => {
        const {versionId} = this.props;
        this.dispatch(fundOutputActionRun(versionId, code));
    };

    handleActionInterrupt = (id) => {
        this.dispatch(fundOutputActionInterrupt(id));
    };

    focus = () => {
        this.refs.listBox.focus()
    };

    renderListItem = (item) => {
        const {outputState, readMode} = this.props;
        const config = this.getConfigByCode(item.code);
        const name = config ? <span title={item.name} className='name'>{config.name}</span> : '';
        const state = actionStateTranslation(item.state);
        let buttons = null;
        if (!readMode && outputState !== OutputState.FINISHED && outputState !== OutputState.OUTDATED) {
            if (state == null || ACTION_NOT_RUNNING_STATE.indexOf(item.state) !== -1) {
                buttons = <Icon glyph="fa-play" onClick={() => this.handleActionRun(item.code)}/>;
            } else if (ACTION_RUNNING_STATE.indexOf(item.state) !== -1) {
                buttons = <Icon glyph="fa-stop" onClick={() => this.handleActionInterrupt(item.id)}/>
            }
        }

        let dateToState = "";

        if (item.dateFinished != null) {
            dateToState = " (" + dateTimeToString(new Date(item.dateFinished)) + ")";
        } else if (item.dateStarted != null) {
            dateToState = " (" + dateTimeToString(new Date(item.dateStarted)) + ")";
        } else if (item.datePlanned != null) {
            dateToState = " (" + dateTimeToString(new Date(item.datePlanned)) + ")";
        }

        return <div className='item' key={item.id}>
            <div>
                <div>{name}</div>
                <div>
                    {i18n('arr.output.functions.state', state == null ? i18n('arr.output.functions.notStarted') : state + dateToState)}
                </div>
            </div>
            <div>{buttons}</div>
        </div>
    };

    render() {
       const {fundOutputFunctions, actionConfig} = this.props;
        if (!actionConfig) {
            return <HorizontalLoader />
        }

        return <div className='functions-list-container'>
            <FormInput componentClass="select" onChange={(e) => this.handleStateSearch(e.target.value)} value={fundOutputFunctions.filterRecommended} disabled={!fundOutputFunctions.fetched}>
                <option value={true} key="recommended-filter">{i18n('arr.output.functions.recommended')}</option>
                <option value={false} key="no-filter">{i18n('arr.output.functions.all')}</option>
            </FormInput>

            <StoreHorizontalLoader store={fundOutputFunctions} />
            {fundOutputFunctions.fetched && <ListBox
                ref="listBox"
                className="functions-listbox"
                items={fundOutputFunctions.data.sort((a,b) => {
                    const configA = this.getConfigByCode(a.code);
                    const configB = this.getConfigByCode(b.code);

                    const nameA = configA.name.toUpperCase();
                    const nameB = configB.name.toUpperCase();
                    if (nameA < nameB) {
                        return -1;
                    }
                    if (nameA > nameB) {
                        return 1;
                    }

                    // names must be equal
                    return 0;
                })}
                renderItemContent={this.renderListItem}
            />}
        </div>;
    }
}

function mapStateToProps(state) {
    const {arrRegion: {funds, activeIndex}} = state;

    let actionConfig = null;
    if (activeIndex !== null && funds[activeIndex].fundAction) {
        const {fundAction: {config: {fetched, data}}} = funds[activeIndex];
        if (fetched) {
            actionConfig = data;
        }
    }
    return {
        actionConfig
    }
}


export default connect(mapStateToProps, null, null, { withRef: true })(FundOutputFunctions);
