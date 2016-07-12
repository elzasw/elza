/**
 * Správa souborů.
 */

require('./FundOutputFunctions.less')

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, ListBox, Loading} from 'components/index.jsx';
import FundActionPage from 'pages/arr/FundActionPage.jsx'
import {Input} from 'react-bootstrap'
import {fetchFundOutputFunctionsIfNeeded, fundOutputFunctionsFilterByState, fundOutputActionRun, fundOutputActionInterrupt} from 'actions/arr/fundOutputFunctions.jsx'
import {fundActionFetchConfigIfNeeded} from 'actions/arr/fundAction.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {dateTimeToString} from 'components/Utils.jsx';

const ACTION_RUNNING_STATE = ['RUNNING', 'WAITING', 'PLANNED',];

const FundOutputFunctions = class FundOutputFunctions extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'handleStateSearch',
            'renderListItem',
            'handleActionRun',
            'handleActionInterrupt',
            'getConfigByCode',
            'focus'
        );

        this.state = {}
    }

    componentDidMount() {
        const {versionId, outputId} = this.props;
        this.dispatch(fetchFundOutputFunctionsIfNeeded(versionId, outputId))
        this.dispatch(fundActionFetchConfigIfNeeded(versionId));
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, outputId} = this.props;
        this.dispatch(fetchFundOutputFunctionsIfNeeded(versionId, outputId));
        this.dispatch(fundActionFetchConfigIfNeeded(versionId));
    }

    handleStateSearch(state) {
        const {versionId} = this.props;
        this.dispatch(fundOutputFunctionsFilterByState(versionId, state));
    }


    getConfigByCode(code) {
        const {actionConfig} = this.props;
        const index = indexById(actionConfig, code, 'code');
        if (index !== null) {
            return actionConfig[index];
        }
        return null;
    }
    
    handleActionRun(code) {
        const {versionId} = this.props;
        this.dispatch(fundOutputActionRun(versionId, code));
    }

    handleActionInterrupt(id) {
        this.dispatch(fundOutputActionInterrupt(id));
    }

    focus() {
        this.refs.listBox.focus()
    }

    renderListItem(item) {
        const config = this.getConfigByCode(item.code);
        const name = config ? <span title={item.name} className='name'>{config.name}</span> : '';
        const state = FundActionPage.getStateTranslation(item.state);
        return (
            <div className='item' key={item.id}>
                <div>
                    <div>{name}</div>
                    <div>
                        {i18n('arr.output.functions.state', state == null ? i18n('arr.output.functions.notStarted') : state + (item.dateFinished != null ? " (" + dateTimeToString(new Date(item.dateFinished)) + ")" : ''))}
                    </div>
                </div>
                <div>
                    {state == null && <Icon glyph="fa-play" onClick={() => this.handleActionRun(item.code)}/>}
                    {ACTION_RUNNING_STATE.indexOf(item.state) !== -1 && <Icon glyph="fa-stop" onClick={() => this.handleActionInterrupt(item.id)}/>}
                </div>
            </div>
        )
    }

    render() {
        const {fetched, data, filterRecommended, actionConfig} = this.props;

        if (!fetched || !actionConfig) {
            return <Loading/>
        }

        return (
            <div className='functions-list-container'>
                <Input type="select" onChange={(e) => this.handleStateSearch(e.target.value)} value={filterRecommended}>
                    <option value={true} key="recommended-filter">{i18n('arr.output.functions.recommended')}</option>
                    <option value={false} key="no-filter">{i18n('arr.output.functions.all')}</option>
                </Input>

                <ListBox
                    ref="listBox"
                    className="functions-listbox"
                    items={data}
                    renderItemContent={this.renderListItem}
                />
            </div>
        )
    }
};

FundOutputFunctions.propTypes = {
    actionConfig: React.PropTypes.array,
    outputId: React.PropTypes.number.isRequired,
    versionId: React.PropTypes.number.isRequired,
    data: React.PropTypes.array,
    filterRecommended: React.PropTypes.bool.isRequired,
    fetched: React.PropTypes.bool.isRequired
};

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


module.exports = connect(mapStateToProps, null, null, { withRef: true })(FundOutputFunctions);
