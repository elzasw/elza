import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, NoFocusButton, Icon, RelationForm} from 'components/index.jsx'
import {isNotBlankObject} from 'components/Utils.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {relationCreate, relationUpdate, relationDelete, RELATION_CLASS_TYPE_REPEATABILITY, USE_UNITDATE_ENUM, RELATION_CLASS_RELATION_CODE, normalizeDatation} from 'actions/party/party.jsx'

import './PartyDetailRelations.less'

class PartyDetailRelations extends AbstractReactComponent {

    static PropTypes = {
        canEdit: React.PropTypes.bool.isRequired,
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
            from: isNotBlankObject(relation.from) ? normalizeDatation(relation.from) : null,
            to: isNotBlankObject(relation.to) ? normalizeDatation(relation.to) : null,
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
            from: isNotBlankObject(newRelation.from) ? normalizeDatation(newRelation.from) : null,
            to: isNotBlankObject(newRelation.to) ? normalizeDatation(newRelation.to) : null,
        }));
        this.dispatch(modalDialogHide());
    };

    handleRelationAdd = () => {
        const {label, party, relationType} = this.props;
        this.dispatch(modalDialogShow(this, label, <RelationForm partyId={party.id} relationType={relationType} onSubmitForm={this.addIdentifier} />, "dialog-lg"));
    };

    handleRelationUpdate = (relation) => {
        const {label, party, relationType} = this.props;
        this.dispatch(modalDialogShow(this, label, <RelationForm partyId={party.id} relationType={relationType} initialValues={relation} onSubmitForm={this.update.bind(this, relation)} />, "dialog-lg"));
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

        return <div className="party-detail-relations">
            <div>
                <label>{label}</label>
                {addButton}
            </div>
            {relations.map((relation, index) => <div key={relation.id} className="value-group relation-group flex">
                <div className="flex-1">
                    {(relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL || relationType.useUnitdate == USE_UNITDATE_ENUM.ONE) && relation.from && relation.from.value && <div className="flex flex-1 no-wrap-group">
                        {relationType.relationClassType.code !== RELATION_CLASS_RELATION_CODE && <div className="item">{relationType.relationClassType.name}: </div>}
                            {relation.from.value && <div className="item">{relation.from.value}</div>}
                            {relation.from.textDate && <div className="item">{relation.from.textDate}</div>}
                            {relation.from.note && <div className="note">{relation.from.note}</div>}
                    </div>}
                    {relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL && relation.to && relation.to.value && <div className="flex flex-1 no-wrap-group">
                        {relation.to.value && <div className="item">{relation.to.value}</div>}
                        {relation.to.textDate && <div className="item">{relation.to.textDate}</div>}
                        {relation.to.note && <div className="note">{relation.to.note}</div>}
                    </div>}
                    {relation.relationEntities && relation.relationEntities.map(entity => <div key={entity.id}>
                        <label>{entity.roleType.name}:</label> {entity.record.record}<small>{entity.record.note}</small>
                    </div>)}
                    {relation.note && <div>{relation.note}</div>}
                </div>
                <div className="actions">
                    <NoFocusButton onClick={() => this.handleRelationUpdate(relation)}><Icon glyph="fa-pencil" /></NoFocusButton>
                    <NoFocusButton onClick={() => this.handleRelationDelete(relation.id)}><Icon glyph="fa-times" /></NoFocusButton>
                </div>
            </div>)}
        </div>
    }
}

export default connect()(PartyDetailRelations);
