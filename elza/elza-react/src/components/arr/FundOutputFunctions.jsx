import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, ListBox, StoreHorizontalLoader, HorizontalLoader, FormInput} from 'components/shared';
import {fetchFundOutputFunctionsIfNeeded, fundOutputFunctionsFilterByState, fundOutputActionRun, fundOutputActionInterrupt} from 'actions/arr/fundOutputFunctions.jsx'
import {fundActionFetchConfigIfNeeded} from 'actions/arr/fundAction.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {dateTimeToString} from 'components/Utils.jsx';
import './FundOutputFunctions.scss'
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

    static propTypes = {
        actionConfig: PropTypes.array,
        outputId: PropTypes.number.isRequired,
        versionId: PropTypes.number.isRequired,
        readMode: PropTypes.bool.isRequired,
        outputState: PropTypes.string.isRequired,
        fundOutputFunctions: PropTypes.object.isRequired,
    };

    componentDidMount() {
        const {versionId, outputId} = this.props;
        this.props.dispatch(fetchFundOutputFunctionsIfNeeded(versionId, outputId));
        this.props.dispatch(fundActionFetchConfigIfNeeded(versionId));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {versionId, outputId} = this.props;
        this.props.dispatch(fetchFundOutputFunctionsIfNeeded(versionId, outputId));
        this.props.dispatch(fundActionFetchConfigIfNeeded(versionId));
    }

    handleStateSearch = (state) => {
        const {versionId} = this.props;
        this.props.dispatch(fundOutputFunctionsFilterByState(versionId, state));
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
        this.props.dispatch(fundOutputActionRun(versionId, code));
    };

    handleActionInterrupt = (id) => {
        this.props.dispatch(fundOutputActionInterrupt(id));
    };

    focus = () => {
        this.refs.listBox.focus()
    };

    getActionState = (item) => {
        const state = actionStateTranslation(item.state);
        let stateString = i18n('arr.output.functions.notStarted');
        const actionDate = item.dateFinished || item.dateStarted || item.datePlanned;
        const formattedDate = dateTimeToString(new Date(actionDate));

        if(state !== null){
            stateString = state + " (" + formattedDate + ")";
        }

        return stateString;
    }

    getItemActions = (item) => {
        const {outputState, readMode} = this.props;
        let actions = [];

        if (!readMode && outputState !== OutputState.FINISHED && outputState !== OutputState.OUTDATED) {
            if (!item.state || ACTION_NOT_RUNNING_STATE.indexOf(item.state) !== -1) {
                actions.push(<Icon glyph="fa-play" onClick={() => this.handleActionRun(item.code)}/>);
            } else if (ACTION_RUNNING_STATE.indexOf(item.state) !== -1) {
                actions.push(<Icon glyph="fa-stop" onClick={() => this.handleActionInterrupt(item.id)}/>);
            }
        }
        return actions;
    }

    renderListItem = (props, active, index) => {
        const {item} = props;
        const config = this.getConfigByCode(item.code);
        const name = config ? config.name : '';
        const actionState = this.getActionState(item);

        return <div className='item' key={index}>
            <div className="details">
                <div className="name" title={name}>{name}</div>
                <div className="info" title={actionState}>{actionState}</div>
            </div>
            <div className="actions">{this.getItemActions(item)}</div>
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


export default connect(mapStateToProps)(FundOutputFunctions);
