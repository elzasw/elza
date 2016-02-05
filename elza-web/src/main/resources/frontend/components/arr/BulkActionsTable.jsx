/**
 * Tabulka s hromadnými akcemi, komponenta obsahuje i logiku
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading, Icon} from 'components';
import {Button, Input, Table} from 'react-bootstrap';
import {dateTimeToString} from 'components/Utils'
import {indexById} from 'stores/app/utils.jsx'
import {WebApi} from 'actions'
import {bulkActionsRun, bulkActionsLoadData, bulkActionsValidateVersion} from 'actions/arr/bulkActions'

var BulkActionsTable = class BulkActionsTable extends AbstractReactComponent {
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
                this.dispatch(bulkActionsLoadData(versionId, mandatory, silent))
        )
    }

    handleRun(code, name) {
        window.confirm(i18n('arr.fa.bulkActions.runAction', name)) && this.dispatch(bulkActionsRun(this.props.versionId, code));
    }

    render() {
        const {onClose} = this.props;
        var table;
        if (this.props.store.actions.length !== 0 && this.props.store.states !== false) {
            var key = 0;
            table = this.props.store.actions.map((item) => {
                var index = indexById(this.props.store.states, item.code, 'code'), state, canRun = false, lastRun = "-";
                let indexExist = (index != null);
                switch (indexExist ? this.props.store.states[index].state : null) {
                    case "RUNNING":
                        state = <td><Icon glyph="fa-play-circle-o"/> {i18n('arr.fa.bulkActions.running')}</td>;
                        break;
                    case "WAITING":
                    case "PLANNED":
                        state = <td><Icon glyph="fa-pause-circle-o"/> {i18n('arr.fa.bulkActions.planned')}</td>;
                        break;
                    case "ERROR":
                        state = <td><Icon glyph="fa-exclamation-triangle"/> {i18n('arr.fa.bulkActions.err')}</td>;
                        break;
                    case null:
                        lastRun = 'Nikdy';
                    case "FINISH":
                    default:
                        canRun = true;
                        state = <td><Icon glyph="fa-times"/> {i18n('arr.fa.bulkActions.noActions')}</td>;
                        break;
                }
                if (indexExist && this.props.store.states[index].runChange) {
                    lastRun = dateTimeToString(new Date(this.props.store.states[index].runChange.changeDate));
                }
                return <tr key={key++}>
                    {state}
                    <td title={item.description}>{item.name}</td>
                    <td>{lastRun}</td>
                    <td>{canRun && <Button
                        onClick={() => (this.handleRun(item.code, item.name))}>{i18n('arr.fa.bulkActions.run')}</Button>}</td>
                </tr>
            });
        } else {
            table = <tr key="nothing">
                <td className="text-center" colSpan="4">{i18n('arr.fa.bulkActions.noActions')}</td>
            </tr>;
        }
        return (
            <Table striped bordered condensed>
                <thead>
                <tr>
                    <th>{i18n('arr.fa.bulkActions.state')}</th>
                    <th>{i18n('arr.fa.bulkActions.name')}</th>
                    <th>{i18n('arr.fa.bulkActions.runChange')}</th>
                    <th>{i18n('arr.fa.bulkActions.tools')}</th>
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

module.exports = connect((state) => ({
    store: state.arrRegion.fas[state.arrRegion.activeIndex].bulkActions,
    versionId: state.arrRegion.fas[state.arrRegion.activeIndex].versionId
}))(BulkActionsTable);



