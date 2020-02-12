import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl, Button} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, Icon} from 'components/shared'
import {indexById, objectById} from 'stores/app/utils.jsx'
import {normalizeNameObject} from 'actions/party/party.jsx'

import './PartyDetailNames.less'
import PartyNameForm from "./PartyNameForm";

class PartyDetailNames extends AbstractReactComponent {

    static propTypes = {
        canEdit: PropTypes.bool.isRequired,
        party: PropTypes.object.isRequired,
        partyType: PropTypes.object.isRequired,
        onPartyUpdate: PropTypes.func.isRequired,
    };

    getPartyName = (partyName, partyType) => {
        const nameBuilder = [];
        const nameHelper = (namePart,nameBuilder) => {
            if (namePart) {
                nameBuilder.push(namePart);
            }
        };

        nameHelper(partyName.degreeBefore, nameBuilder);
        nameHelper(partyName.otherPart, nameBuilder);
        nameHelper(partyName.mainPart, nameBuilder);
        let roman = null, geoAddon = null, addon = null;
        partyName.partyNameComplements.forEach((e) => {
            const type = objectById(partyType.complementTypes, e.complementTypeId);
            if (type) {
                if (type.code == "2") {
                    addon = e.complement;
                } else if (type.code == "3") {
                    roman = e.complement;
                } else if (type.code == "4") {
                    geoAddon = e.complement;
                }
            }
        });
        nameHelper(roman, nameBuilder);
        nameHelper(geoAddon, nameBuilder);
        nameHelper(addon, nameBuilder);
        let str = nameBuilder.join(' ');
        if (partyName.degreeAfter != null && partyName.degreeAfter.length > 0) {
            str += ', ' + partyName.degreeAfter;
        }
        return str;
    };

    partyNameAdd = (data) => {
        const partyNames = this.props.party.partyNames;
        data = normalizeNameObject(data);
        const party = {
            ...this.props.party,
            partyNames: [
                ...partyNames,
                data
            ]
        };
        return this.props.onPartyUpdate(party);
    };

    partyNameDelete = (id) => {
        const partyNames = this.props.party.partyNames;
        const index = indexById(partyNames, id);
        const party = {
            ...this.props.party,
            partyNames: [
                ...partyNames.slice(0, index),
                ...partyNames.slice(index+1)
            ]
        };
        this.props.onPartyUpdate(party);
    };

    partyNameUpdate = (originalName, newName) => {
        const partyNames = this.props.party.partyNames;
        const index = indexById(partyNames, originalName.id);
        newName = normalizeNameObject({
            ...originalName,
            ...newName
        });
        const party = {
            ...this.props.party,
            partyNames: [
                ...partyNames.slice(0, index),
                newName,
                ...partyNames.slice(index+1)
            ]
        };
        return this.props.onPartyUpdate(party);
    };

    partyNameSetPreffered = (id) => {
        const party = {
            ...this.props.party,
            partyNames: this.props.party.partyNames.map(name => ({
                ...name,
                prefferedName: name.id == id
            }))
        };
        this.props.onPartyUpdate(party);
    };

    handlePartyNameAdd = () => {
        const {partyType} = this.props;
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.new'), <PartyNameForm partyType={partyType} onSubmitForm={this.partyNameAdd} />, "dialog-lg"));
    };

    handlePartyNameUpdate = (partyName) => {
        const {partyType} = this.props;
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.update'), <PartyNameForm partyType={partyType} initData={partyName} onSubmitForm={this.partyNameUpdate.bind(this, partyName)} />, "dialog-lg"));
    };

    handleDelete = (id) => {
        if (window.confirm(i18n('party.detail.name.delete'))) {
            this.partyNameDelete(id);
        }
    };

    handleSelectPreffered = (id) => {
        if (window.confirm(i18n('party.detail.name.setPrefferedNameAlert'))) {
            this.partyNameSetPreffered(id);
        }
    };

    render() {
        const {party, partyType, canEdit} = this.props;

        return <div className="party-detail-names">
            <div >
                <label className="group-label">{i18n("party.detail.formNames")}</label>
                {canEdit && <Button bsStyle="action" onClick={this.handlePartyNameAdd}><Icon glyph="fa-plus" /></Button>}
            </div>
            <div className="name-group">
                {party.partyNames.map((partyName, index) =>
                    <div key={partyName.id} className={partyName.prefferedName ? "preffered value-group" : "value-group"}>
                        <div className="value">{this.getPartyName(partyName, partyType)}</div>
                        <div className="actions">
                            {canEdit && <Button  bsStyle="action" onClick={() => this.handlePartyNameUpdate(partyName)}><Icon glyph="fa-pencil" /></Button>}
                            {canEdit
                            && !partyName.prefferedName
                            && <span>
                                <Button className="delete" bsStyle="action" onClick={() => this.handleDelete(partyName.id)}><Icon glyph="fa-trash" /></Button>
                                <Button bsStyle="action" onClick={() => this.handleSelectPreffered(partyName.id)}><Icon glyph="fa-star" /></Button>
                            </span>}
                        </div>
                    </div>)}
            </div>
        </div>
    }
}

export default connect()(PartyDetailNames);
