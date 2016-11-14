import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, NoFocusButton, Icon, PartyNameForm} from 'components/index.jsx'
import {indexById, objectById} from 'stores/app/utils.jsx'


class PartyDetailNames extends AbstractReactComponent {

    static PropTypes = {
        party: React.PropTypes.object.isRequired,
        partyType: React.PropTypes.object.isRequired,
        onPartyUpdate: React.PropTypes.func.isRequired,
    };

    getPartyName = (partyName, partyType) => {
        const nameBuilder = [];
        const nameHelper = (namePart,nameBuilder) => {
            if (namePart) {
                nameBuilder.push(namePart);
            }
        };

        nameHelper(partyName.degreeBefore, nameBuilder);
        nameHelper(partyName.mainPart, nameBuilder);
        nameHelper(partyName.otherPart, nameBuilder);
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
        const party = {
            ...this.props.party,
            partyNames: [
                ...partyNames,
                {
                    ...data,
                    nameFormType: {
                        id: data.nameFormTypeId
                    }
                }
            ]
        };
        this.props.onPartyUpdate(party);
        this.dispatch(modalDialogHide());
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

    partyNameSetPreffered = (id) => {
        const party = {
            ...this.props.party,
            partyNames: this.props.party.partyNames.map(name => ({
                ...name,
                prefferedName: name.partyNameId == id
            }))
        };
        this.props.onPartyUpdate(party);
    };

    handlePartyNameAdd = () => {
        const {partyType} = this.props;
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.new') , <PartyNameForm partyType={partyType} onSubmitForm={this.partyNameAdd} />));
    };

    handlePartyNameUpdate = (partyName) => {
        const {partyType} = this.props;
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.update') , <PartyNameForm partyType={partyType} initData={partyName} onSubmitForm={this.partyNameAdd.bind(this, partyName)} />));
    };

    handleDelete = (id) => {
        if (confirm(i18n('party.detail.name.delete'))) {
            this.partyNameDelete(id);
        }
    };

    handleSelectPreffered = (id) => {
        if (confirm(i18n('party.detail.name.prefferedName'))) {
            this.partyNameSetPreffered(id);
        }
    };

    render() {
        const {party, partyType} = this.props;

        return <div>
            <div>
                <label>{i18n("party.detail.formNames")}</label>
                <NoFocusButton bsStyle="default" onClick={this.handlePartyNameAdd}><Icon glyph="fa-plus" /></NoFocusButton>
            </div>
            {party.partyNames.map((partyName, index) => <div key={partyName.id} className="value-group">
                <FormControl.Static>{this.getPartyName(partyName, partyType)}</FormControl.Static>
                <div className="actions">
                    <NoFocusButton><Icon glyph="fa-pencil" /></NoFocusButton>
                    {partyName.prefferedName ? i18n('party.detail.formNames.prefferedName') : <span>
                        <NoFocusButton onClick={() => this.handleDelete(partyName.id)}><Icon glyph="fa-times" /></NoFocusButton>
                        <NoFocusButton onClick={() => this.handleSelectPreffered(partyName.id)}><Icon glyph="fa-check" /></NoFocusButton>
                    </span>}
                </div>
            </div>)}
        </div>
    }
}

export default connect()(PartyDetailNames);
