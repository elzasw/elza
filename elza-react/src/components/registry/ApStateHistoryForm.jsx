/**
 * Formulář zobrazení hostorie.
 */
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent} from 'components/shared';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { messageFor } from 'components/shared/lang/dynamicMessage';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    date: { id: 'ap.history.title.change.date', defaultMessage: 'Datum' },
    time: { id: 'ap.history.title.change.time', defaultMessage: 'Čas' },
    state: { id: 'ap.history.title.change.state', defaultMessage: 'Stav' },
    operation: { id: 'ap.history.title.change.operace', defaultMessage: 'Operace' },
    scope: { id: 'ap.history.title.change.scope', defaultMessage: 'Oblast' },
    type: { id: 'ap.history.title.change.type', defaultMessage: 'Podtřída' },
    comment: { id: 'ap.history.title.change.comment', defaultMessage: 'Komentář' },
    user: { id: 'ap.history.title.change.user', defaultMessage: 'Uživatel' },
});

/** Stav entity v historii; klíč se dřív skládal z hodnoty. */
const stateMessages = defineMessages({
    NEW: { id: 'ap.history.title.state.NEW', defaultMessage: 'nová' },
    TO_APPROVE: { id: 'ap.history.title.state.TO_APPROVE', defaultMessage: 'ke schválení' },
    APPROVED: { id: 'ap.history.title.state.APPROVED', defaultMessage: 'schválená' },
    TO_AMEND: { id: 'ap.history.title.state.TO_AMEND', defaultMessage: 'k doplnění' },
    REV_ACTIVE: { id: 'ap.history.title.state.REV_ACTIVE', defaultMessage: 'revize v přípravě' },
    REV_TO_APPROVE: { id: 'ap.history.title.state.REV_TO_APPROVE', defaultMessage: 'revize ke schválení' },
    REV_TO_AMEND: { id: 'ap.history.title.state.REV_TO_AMEND', defaultMessage: 'revize k doplnění' },
});

/** Provedená operace; klíč se dřív skládal z hodnoty. */
const operationMessages = defineMessages({
    AP_CREATE: { id: 'ap.history.title.operation.AP_CREATE', defaultMessage: 'vytváření' },
    AP_UPDATE: { id: 'ap.history.title.operation.AP_UPDATE', defaultMessage: 'aktualizace' },
    AP_SYNCH: { id: 'ap.history.title.operation.AP_SYNCH', defaultMessage: 'synchronizace' },
});
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {dateToString, timeToString} from 'components/Utils.jsx';
import ListBox from '../shared/listbox/ListBox';
import {WebApi} from '../../actions';
import {HorizontalLoader} from '../shared';
import './ApStateHistoryForm.scss';

class ApStateHistoryForm extends AbstractReactComponent {
    static propTypes = {
        accessPointId: PropTypes.number.isRequired,
    };

    constructor(props) {
        super(props);
        this.state = {
            fetched: false,
            data: [],
        };
    }

    componentDidMount() {
        WebApi.findStateHistories(this.props.accessPointId).then(data => {
            this.setState({
                fetched: true,
                data: data,
            });
        });
    }

    renderItemContent = item => {
        if (item === null) {
            return null;
        }

        return (
            <div className="row-container">
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                {item.description && (
                    <div className="col col3" title={item.description}>
                        {item.description}
                    </div>
                )}
                {item.typeText && (
                    <div className="col col4" title={item.typeText}>
                        {item.typeText}
                    </div>
                )}
                <div className="col col5">{item.username ? item.username : <i>System</i>}</div>
            </div>
        );
    };

    getState = state => {
        return this.props.intl.formatMessage(messageFor(stateMessages, state, stateMessages.NEW));
    };

	getOperation = op => {
		return this.props.intl.formatMessage(messageFor(operationMessages, op, operationMessages.AP_UPDATE));
	};

    renderItem = data => {
        const item = data.item;
        return (
            <div className="row-container">
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                <div className="col col3">{item.state ? this.getState(item.state) : ""}</div>
				<div className="col col4">{item.operation ? this.getOperation(item.operation) : ""}</div>
                <div className="col col5" title={item.scope}>
                    {item.scope}
                </div>
                <div className="col col6" title={item.type}>
                    {item.type}
                </div>
                <div className="col col7" title={item.comment}>
                    {item.comment}
                </div>
                <div className="col col8">{item.username ? item.username : <i>System</i>}</div>
            </div>
        );
    };

    render() {
        const {onClose} = this.props;
        const {data, fetched} = this.state;

        let content = <ListBox items={data} renderItemContent={this.renderItem} />;

        return (
            <div className="ap-state-history-form-container">
                <Modal.Body>
                    <div className="changes-listbox-container">
                        <div className="header-container">
                            <div className="col col1">{<FormattedMessage {...messages.date} />}</div>
                            <div className="col col2">{<FormattedMessage {...messages.time} />}</div>
                            <div className="col col3">{<FormattedMessage {...messages.state} />}</div>
							<div className="col col4">{<FormattedMessage {...messages.operation} />}</div>
                            <div className="col col5">{<FormattedMessage {...messages.scope} />}</div>
                            <div className="col col6">{<FormattedMessage {...messages.type} />}</div>
                            <div className="col col7">{<FormattedMessage {...messages.comment} />}</div>
                            <div className="col col8">{<FormattedMessage {...messages.user} />}</div>
                            {/*<div className="colScrollbar" style={{width: getScrollbarWidth()}}></div>*/}
                        </div>
                        {fetched ? content : <HorizontalLoader />}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.close} />}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

export default injectIntl(connect()(ApStateHistoryForm));
