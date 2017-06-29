/**
 * Tabulka s hromadnými akcemi, komponenta obsahuje i logiku
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Loading, Icon} from 'components/shared';
import {Button, Table} from 'react-bootstrap';
import {dateTimeToString} from 'components/Utils.jsx';
import {indexById} from 'stores/app/utils.jsx';
import {bulkActionsRun, bulkActionsLoadData, bulkActionsValidateVersion} from 'actions/arr/bulkActions.jsx';

const BulkActionsTable = class BulkActionsTable extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleRun');
    }

    componentDidMount() {
        this.requestData(this.props.store.isDirty, this.props.store.isFetching, this.props.versionId, this.props.mandatory, false)
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.store.isDirty, nextProps.store.isFetching, nextProps.versionId, nextProps.mandatory, true);
    }

    requestData(isDirty, isFetching, versionId, mandatory, silent) {
        (isDirty || this.props.store.mandatory !== mandatory) && !isFetching &&
        (this.props.versionValidate ?
                this.dispatch(bulkActionsValidateVersion(versionId, silent)) :
                this.dispatch(bulkActionsLoadData(versionId, silent))
        )
    }

    handleRun(code, name) {
        window.confirm(i18n('arr.fund.bulkActions.runAction', name)) && this.dispatch(bulkActionsRun(this.props.versionId, code));
    }

    render() {
        const {onClose} = this.props;
        var table;
        if (this.props.store.actions.length !== 0 && this.props.store.states !== false) {
            var key = 0;
            table = this.props.store.actions.map((item) => {
                var index = indexById(this.props.store.states, item.code, 'code'), state, innerState, canRun = false, lastRun = "-";
                let indexExist = (index != null);
                switch (indexExist ? this.props.store.states[index].state : null) {
                    case "RUNNING":
                        innerState = <div><Icon glyph="fa-play-circle-o"/>{i18n('arr.fund.bulkActions.running')}</div>;
                        break;
                    case "WAITING":
                    case "PLANNED":
                        innerState = <div><Icon glyph="fa-pause-circle-o"/>{i18n('arr.fund.bulkActions.planned')}</div>;
                        break;
                    case "ERROR":
                        innerState = <div><Icon glyph="fa-exclamation-triangle"/>{i18n('arr.fund.bulkActions.err')}</div>;
                        break;
                    case null:
                        lastRun = 'Nikdy';
                    //case "FINISH":
                    default:
                        canRun = true;
                        innerState = <div><Icon glyph="fa-times"/>{i18n('arr.fund.bulkActions.idle')}</div>;
                }
                state = <td className="no-wrap">{innerState}</td>
                if (indexExist && this.props.store.states[index].runChange) {
                    lastRun = dateTimeToString(new Date(this.props.store.states[index].runChange.changeDate));
                }
                return <tr key={key++}>
                    {state}
                    <td title={item.description}>{item.name}</td>
                    <td>{lastRun}</td>
                    <td>{canRun && <Button
                        onClick={() => (this.handleRun(item.code, item.name))}>{i18n('arr.fund.bulkActions.run')}</Button>}</td>
                </tr>
            });
        } else {
            table = null;
        }

        let okText = this.props.okText ? this.props.okText : <div>i18n('arr.fund.bulkActions.noActions')</div>;

        return (
            table === null ? okText :
            <Table striped bordered condensed >
                <thead>
                <tr>
                    <th>{i18n('arr.fund.bulkActions.state')}</th>
                    <th>{i18n('arr.fund.bulkActions.name')}</th>
                    <th>{i18n('arr.fund.bulkActions.runChange')}</th>
                    <th>{i18n('arr.fund.bulkActions.tools')}</th>
                </tr>
                </thead>
                <tbody>
                {this.props.store.isFetching &&
                <tr key="x">
                    <td colSpan="4"><Loading /></td>
                </tr>
                }
                {table}
                </tbody>
            </Table>
        )
    }
};


BulkActionsTable.propTypes = {
    mandatory: React.PropTypes.bool.isRequired,
    versionValidate: React.PropTypes.bool.isRequired
};

export default connect((state) => ({
    store: state.arrRegion.funds[state.arrRegion.activeIndex].bulkActions,
    versionId: state.arrRegion.funds[state.arrRegion.activeIndex].versionId
}))(BulkActionsTable);



