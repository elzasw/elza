import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, NoFocusButton, Icon, RelationForm} from 'components/index.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {relationCreate, relationUpdate, relationDelete} from 'actions/party/party.jsx'

const RELATION_CLASS_TYPE_REPEATABILITY = {
    UNIQUE: "UNIQUE",
    MULTIPLE: "MULTIPLE",
};

const USE_UNITDATE_ENUM = {
    NONE: 'NONE',
    ONE: 'ONE',
    INTERVAL: 'INTERVAL',
};

const RELATION_CLASS_RELATION_CODE = "R";


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

class PartyDetailRelations extends AbstractReactComponent {

    static PropTypes = {
        label: React.PropTypes.element.isRequired,
        party: React.PropTypes.object.isRequired,
        relationType: React.PropTypes.object.isRequired,
    };

    addIdentifier = (relation) => {
        const {relationType, party} = this.props;
        this.dispatch(relationCreate({
            ...relation,
            relationTypeId: relationType.id,
            partyId: party.id,
            from: isNotBlankObject(relation.from) ? relation.from : null,
            to: isNotBlankObject(relation.to) ? relation.to : null,
        }));
        this.dispatch(modalDialogHide());
    };

    update = (origRelation, newRelation) => {
        const {relationType, party} = this.props;
        this.dispatch(relationUpdate({
            ...origRelation,
            ...newRelation,
            relationTypeId: relationType.id,
            partyId: party.id,
            from: isNotBlankObject(newRelation.from) ? newRelation.from : null,
            to: isNotBlankObject(newRelation.to) ? newRelation.to : null,
        }));
        this.dispatch(modalDialogHide());
    };

    handleRelationAdd = () => {
        const {label, party, relationType} = this.props;
        this.dispatch(modalDialogShow(this, label, <RelationForm partyId={party.id} relationType={relationType} onSubmitForm={this.addIdentifier} />));
    };

    handleRelationUpdate = (relation) => {
        const {label, party, relationType} = this.props;
        this.dispatch(modalDialogShow(this, label, <RelationForm partyId={party.id} relationType={relationType} initialValues={relation} onSubmitForm={this.update.bind(this, relation)} />));
    };

    handleRelationDelete = (id) => {
        if (confirm(i18n("party.relation.delete.confirm"))) {
            this.dispatch(relationDelete(id));
        }
    };

    render() {
        const {party, label, relationType} = this.props;
        const relations = party.relations ? party.relations.filter(i => i.relationTypeId == relationType.id) : [];
        let addButton = null;
        if (relationType.relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.MULTIPLE ||
            (relationType.relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.UNIQUE &&
                (!relations || relations.length < 1))) {
            addButton = <NoFocusButton bsStyle="default" onClick={this.handleRelationAdd}><Icon glyph="fa-plus" /></NoFocusButton>;
        }

        return <div>
            <div>
                <label>{label}</label>
                {addButton}
            </div>
            {relations.map((relation, index) => <div key={relation.id} className="value-group relation-group">
                <FormControl.Static componentClass="div">
                    {(relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL || relationType.useUnitdate == USE_UNITDATE_ENUM.ONE) && relation.from && <div>
                        <FormControl.Static>{relationType.relationClassType.code !== RELATION_CLASS_RELATION_CODE && relationType.relationClassType.name + ": "}{relation.from.textDate}</FormControl.Static>
                        <div>{relation.dateNote}</div>
                    </div>}
                    {relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL && relation.to && <FormControl.Static>{relation.to.textDate}</FormControl.Static>}
                    {relation.relationEntities && relation.relationEntities.map(entity => <div>
                        <label>{entity.roleType.name}:</label> {entity.record.record}<small>{entity.record.note}</small>
                    </div>)}
                    <FormControl.Static>{relation.note}</FormControl.Static>
                </FormControl.Static>
                <div className="actions">
                    <NoFocusButton onClick={() => this.handleRelationUpdate(relation)}><Icon glyph="fa-pencil" /></NoFocusButton>
                    <NoFocusButton onClick={() => this.handleRelationDelete(relation.id)}><Icon glyph="fa-times" /></NoFocusButton>
                </div>
            </div>)}
        </div>
    }
}

export default connect()(PartyDetailRelations);