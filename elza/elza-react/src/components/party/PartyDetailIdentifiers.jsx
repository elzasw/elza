import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {Button} from '../ui';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx';
import {MODAL_DIALOG_VARIANT} from '../../constants.tsx';

import './PartyDetailIdentifiers.scss';
import PartyIdentifierForm from './PartyIdentifierForm';

const removeUndefined = (obj) => {
    for (let key in obj) {
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
    return Object.keys(newObj).length > 0;
};

/**
 * Identifikátory zadané osoby - pouze u korporací
 */
class PartyDetailIdentifiers extends AbstractReactComponent {

    static propTypes = {
        canEdit: PropTypes.bool.isRequired,
        party: PropTypes.object.isRequired,
        onPartyUpdate: PropTypes.func.isRequired,
    };

    partyGroupIdentifierDelete = (id) => {
        const partyGroupIdentifiers = this.props.party.partyGroupIdentifiers;
        const index = indexById(partyGroupIdentifiers, id);
        const party = {
            ...this.props.party,
            partyGroupIdentifiers: [
                ...partyGroupIdentifiers.slice(0, index),
                ...partyGroupIdentifiers.slice(index + 1),
            ],
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
            ],
        };
        return this.props.onPartyUpdate(party);
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
                ...this.props.party.partyGroupIdentifiers.slice(index + 1),
            ],
        };
        return this.props.onPartyUpdate(party);
    };

    handlePartyGroupIdentifierAdd = () => {
        this.props.dispatch(modalDialogShow(this, i18n('party.detail.identifier.new'), <PartyIdentifierForm
            onSubmitForm={this.addIdentifier}/>, MODAL_DIALOG_VARIANT.LARGE));
    };


    handlePartyGroupIdentifierUpdate = (partyGroupIdentifier) => {
        this.props.dispatch(modalDialogShow(this, i18n('party.detail.identifier.update'), <PartyIdentifierForm
            initialValues={partyGroupIdentifier}
            onSubmitForm={this.update.bind(this, partyGroupIdentifier)}/>, MODAL_DIALOG_VARIANT.LARGE));
    };


    handleDelete = (id) => {
        if (window.confirm(i18n('party.detail.identifier.delete'))) {
            this.partyGroupIdentifierDelete(id);
        }
    };

    render() {
        const {party, canEdit} = this.props;
        const hasValues = party.partyGroupIdentifiers.length > 0;
        return <div className="party-detail-names">
            <div>
                <label className="group-label">{i18n('party.detail.partyGroupIdentifiers')}</label>
                {canEdit &&
                <Button variant="action" onClick={this.handlePartyGroupIdentifierAdd}><Icon glyph="fa-plus"/></Button>}
            </div>
            {hasValues && <div className="name-group">
                {party.partyGroupIdentifiers.map((partyGroupIdentifier, index) => <div key={partyGroupIdentifier.id}
                                                                                       className="value-group">
                    <div className="value">{partyGroupIdentifier.identifier}</div>
                    {canEdit && <div className="actions">
                        <Button variant="action"
                                onClick={() => this.handlePartyGroupIdentifierUpdate(partyGroupIdentifier)}><Icon
                            glyph="fa-pencil"/></Button>
                        <Button className="delete" variant="action"
                                onClick={() => this.partyGroupIdentifierDelete(partyGroupIdentifier.id)}><Icon
                            glyph="fa-trash"/></Button>
                    </div>}
                </div>)}
            </div>}
        </div>;
    }
}

export default connect()(PartyDetailIdentifiers);
