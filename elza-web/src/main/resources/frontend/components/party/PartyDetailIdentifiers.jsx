import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, NoFocusButton, Icon, PartyIdentifierForm} from 'components/index.jsx'
import {indexById} from 'stores/app/utils.jsx'

import './PartyDetailIdentifiers.less'

const removeUndefined = (obj) => {
    for (let key in obj ) {
        if (obj.hasOwnProperty(key)) {
            if (obj[key] === undefined || obj[key] === null) {
                delete obj[key];
            }
        }
    }
    return obj;
};
const isNotBlankObject = (obj) => {
    const newObj = removeUndefined(obj);
    return Object.keys(newObj).length > 0
};

/**
 * Identifikátory zadané osoby - pouze u korporací
 */
class PartyDetailIdentifiers extends AbstractReactComponent {

    static PropTypes = {
        canEdit: React.PropTypes.bool.isRequired,
        party: React.PropTypes.object.isRequired,
        onPartyUpdate: React.PropTypes.func.isRequired,
    };

    partyGroupIdentifierDelete = (id) => {
        const partyGroupIdentifiers = this.props.party.partyGroupIdentifiers;
        const index = indexById(partyGroupIdentifiers, id);
        const party = {
            ...this.props.party,
            partyGroupIdentifiers: [
                ...partyGroupIdentifiers.slice(0, index),
                ...partyGroupIdentifiers.slice(index+1)
            ]
        };
        this.props.onPartyUpdate(party);
    };

    addIdentifier = (identifier) => {
        const party = {
            ...this.props.party,
            partyGroupIdentifiers: [
                ...this.props.party.partyGroupIdentifiers,
                {
                    ...identifier,
                    from: isNotBlankObject(identifier.from) ? identifier.from : null,
                    to: isNotBlankObject(identifier.to) ? identifier.to : null,
                },
            ]
        };
        this.props.onPartyUpdate(party);
        this.dispatch(modalDialogHide())
    };

    update = (origIdentifier, newIdentifier) => {
        const index = indexById(this.props.party.partyGroupIdentifiers, origIdentifier.id);
        const party = {
            ...this.props.party,
            partyGroupIdentifiers: [
                ...this.props.party.partyGroupIdentifiers.slice(0, index),
                {
                    ...origIdentifier,
                    ...newIdentifier,
                    from: isNotBlankObject(newIdentifier.from) ? newIdentifier.from : null,
                    to: isNotBlankObject(newIdentifier.to) ? newIdentifier.to : null,
                },
                ...this.props.party.partyGroupIdentifiers.slice(index+1)
            ]
        };
        this.props.onPartyUpdate(party);
        this.dispatch(modalDialogHide());
    };

    handlePartyGroupIdentifierAdd = () => {
        this.dispatch(modalDialogShow(this, i18n('party.detail.identifier.new') , <PartyIdentifierForm onSubmitForm={this.addIdentifier} />));
    };


    handlePartyGroupIdentifierUpdate = (partyGroupIdentifier) => {
        this.dispatch(modalDialogShow(this, i18n('party.detail.identifier.update'), <PartyIdentifierForm initialValues={partyGroupIdentifier} onSubmitForm={this.update.bind(this, partyGroupIdentifier)} />));
    };


    handleDelete = (id) => {
        if (confirm(i18n('party.detail.identifier.delete'))) {
            this.partyGroupIdentifierDelete(id);
        }
    };

    render() {
        const {party, canEdit} = this.props;
        return <div className="party-detail-identifiers">
            <div>
                <label>{i18n("party.detail.partyGroupIdentifiers")}</label>
                {canEdit && <NoFocusButton bsStyle="default" onClick={this.handlePartyGroupIdentifierAdd}><Icon glyph="fa-plus" /></NoFocusButton>}
            </div>
            {party.partyGroupIdentifiers.map((partyGroupIdentifier, index) => <div key={partyGroupIdentifier.id} className="value-group">
                <div className="value">{partyGroupIdentifier.identifier}</div>
                {canEdit && <div className="actions">
                    <NoFocusButton onClick={() => this.handlePartyGroupIdentifierUpdate(partyGroupIdentifier)}><Icon glyph="fa-pencil" /></NoFocusButton>
                    <NoFocusButton onClick={() => this.partyGroupIdentifierDelete(partyGroupIdentifier.id)}><Icon glyph="fa-times" /></NoFocusButton>
                </div>}
            </div>)}
        </div>
    }
}

export default connect()(PartyDetailIdentifiers);
