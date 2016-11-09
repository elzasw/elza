import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, NoFocusButton, Icon, RelationForm} from 'components/index.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {insertRelation} from 'actions/party/party.jsx'

class PartyDetailRelations extends AbstractReactComponent {

    static PropTypes = {
        label: React.PropTypes.element.isRequired,
        party: React.PropTypes.object.isRequired,
        relationType: React.PropTypes.object.isRequired,
        onPartyUpdate: React.PropTypes.func.isRequired, // možná
    };

    addIdentifier = (relation) => {
        insertRelation(relation, this.props.party.partyId);
        this.dispatch(modalDialogHide());
    };

    handleRelationAdd = () => {
        this.dispatch(modalDialogShow(this, i18n('party.detail.identifier.new') , <RelationForm relationType={this.props.relationType} onSubmitForm={this.addIdentifier} />));
    };

    render() {
        const {party, label, relationType} = this.props;
        return <div>
            <div>
                <label>{label}</label>
                <NoFocusButton bsStyle="default" onClick={this.handleRelationAdd}><Icon glyph="fa-plus" /></NoFocusButton>
            </div>
            {party.relations && party.relations.filter(i => i.relationTypeId == relationType.relationTypeId).map((relation, index) => <div key={relation.relationId} className="value-group">
                <FormControl.Static>a</FormControl.Static>
                <div className="actions">
                    <NoFocusButton><Icon glyph="fa-pencil" /></NoFocusButton>
                    <NoFocusButton onClick={() => {}}><Icon glyph="fa-times" /></NoFocusButton>
                </div>
            </div>)}
        </div>
    }
}

export default connect()(PartyDetailRelations);
