import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, NoFocusButton, Icon} from 'components/index.jsx'
import {indexById} from 'stores/app/utils.jsx'


/**
 * Identifikátory zadané osoby - pouze u korporací
 */
class PartyDetailIdentifiers extends AbstractReactComponent {

    static PropTypes = {
        party: React.PropTypes.object.isRequired,
        onPartyUpdate: React.PropTypes.func.isRequired,
    };

    partyGroupIdentifierDelete = (id) => {
        const partyGroupIdentifiers = this.props.party.partyGroupIdentifiers;
        const index = indexById(partyGroupIdentifiers, id, 'partyGroupIdentifierId');
        const party = {
            ...this.props.party,
            partyGroupIdentifiers: [
                ...partyGroupIdentifiers.slice(0, index),
                ...partyGroupIdentifiers.slice(index+1)
            ]
        };
        this.props.onPartyUpdate(party);
    };

    addIdentifier = (data) => {
        const party = {
            ...this.props.party,
            partyGroupIdentifiers: [
                ...this.props.party.partyGroupIdentifiers,
                {
                    note: data.note,
                    identifier: data.identifier,
                    source: data.source,
                    from: {
                        textDate: data.fromText,
                        calendarTypeId: data.fromCalendar
                    },
                    to: {
                        textDate: data.toText,
                        calendarTypeId: data.toCalendar
                    }
                }
            ]
        };
        this.props.onPartyUpdate(party);
        this.dispatch(modalDialogHide())
    };

    handlePartyGroupIdentifierAdd = () => {
        this.dispatch(modalDialogShow(this, i18n('party.detail.identifier.new') , <PartyIdentifierForm onSubmitForm={this.addIdentifier} />));
    };

    handleDelete = (id) => {
        if (confirm(i18n('party.detail.identifier.delete'))) {
            this.partyGroupIdentifierDelete(id);
        }
    };

    render() {
        const {party} = this.props;
        return <div>
            <div>
                <label>{i18n("party.detail.partyGroupIdentifiers")}</label>
                <NoFocusButton bsStyle="default" onClick={this.handlePartyGroupIdentifierAdd}><Icon glyph="fa-plus" /></NoFocusButton>
            </div>
            {party.partyGroupIdentifiers.map((partyGroupIdentifier, index) => <div key={partyGroupIdentifier.partyGroupIdentifierId} className="value-group">
                <FormControl.Static>{partyGroupIdentifier.identifier}</FormControl.Static>
                <div className="actions">
                    <NoFocusButton><Icon glyph="fa-pencil" /></NoFocusButton>
                    <NoFocusButton onClick={() => this.partyGroupIdentifierDelete(partyGroupIdentifier.partyGroupIdentifierId)}><Icon glyph="fa-times" /></NoFocusButton>
                </div>
            </div>)}
        </div>
    }
}

export default connect()(PartyDetailIdentifiers);
