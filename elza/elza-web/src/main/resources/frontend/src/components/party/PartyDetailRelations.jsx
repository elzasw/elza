import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl, Button} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, Icon} from 'components/shared'
import {isNotBlankObject} from 'components/Utils.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {relationCreate, relationUpdate, relationDelete, RELATION_CLASS_TYPE_REPEATABILITY, USE_UNITDATE_ENUM, RELATION_CLASS_CODES, normalizeDatation} from 'actions/party/party.jsx'

import './PartyDetailRelations.less'
import RelationForm from "./RelationForm";

class PartyDetailRelations extends AbstractReactComponent {

    static PropTypes = {
        canEdit: PropTypes.bool.isRequired,
        label: PropTypes.element.isRequired,
        party: PropTypes.object.isRequired,
        relationType: PropTypes.object.isRequired,
        apTypesMap: PropTypes.object
    };

    addIdentifier = (relation) => {
        const {relationType, party} = this.props;
        return this.dispatch(relationCreate({
            ...relation,
            relationTypeId: relationType.id,
            partyId: party.id,
            from: isNotBlankObject(relation.from) ? normalizeDatation(relation.from) : null,
            to: isNotBlankObject(relation.to) ? normalizeDatation(relation.to) : null,
        }));
    };

    update = (origRelation, newRelation) => {
        const {relationType, party} = this.props;
        return this.dispatch(relationUpdate({
            ...origRelation,
            ...newRelation,
            relationTypeId: relationType.id,
            partyId: party.id,
            from: isNotBlankObject(newRelation.from) ? normalizeDatation(newRelation.from) : null,
            to: isNotBlankObject(newRelation.to) ? normalizeDatation(newRelation.to) : null,
        }));
        console.warn("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    };

    handleRelationAdd = () => {
        const {label, party, relationType, apTypesMap} = this.props;
        this.dispatch(modalDialogShow(this, label, <RelationForm partyId={party.id} apTypesMap={apTypesMap} relationType={relationType} onSubmitForm={this.addIdentifier} />, "dialog-lg"));
    };

    handleRelationUpdate = (relation) => {
        const {label, party, relationType, apTypesMap} = this.props;
        this.dispatch(modalDialogShow(this, label, <RelationForm partyId={party.id} apTypesMap={apTypesMap} relationType={relationType} initialValues={relation} onSubmitForm={this.update.bind(this, relation)} />, "dialog-lg"));
    };

    handleRelationDelete = (id) => {
        if (confirm(i18n("party.relation.delete.confirm"))) {
            this.dispatch(relationDelete(id));
        }
    };

    render() {
        const {party, label, relationType, canEdit} = this.props;
        const relations = party.relations ? party.relations.filter(i => i.relationTypeId == relationType.id) : [];

        let addButton = null;
        if (relationType.relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.MULTIPLE ||
            (relationType.relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.UNIQUE &&
                (!relations || relations.length < 1))) {
            addButton = <Button bsStyle="action" onClick={this.handleRelationAdd}><Icon glyph="fa-plus" /></Button>;
        }

        return (
            <div className="party-detail-relations">
                <div>
                    <label className="group-label">{label}</label>
                    {canEdit && addButton}
                </div>
                {relations.map((relation, index) =>
                <div key={relation.id} className="value-group relation-group flex">
                    <div className="flex-1">
                        {(relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL
                        || relationType.useUnitdate == USE_UNITDATE_ENUM.ONE)
                        && relation.from
                        && (relation.from.value || relation.from.textDate || relation.from.note)
                        && <div className="flex flex-1 no-wrap-group">
                            <label>{relationType.useUnitdate == USE_UNITDATE_ENUM.ONE ? i18n('party.relation.date')+":" : i18n('party.relation.from')+":"}</label>
                            {relation.from.value && <div className="item">{relation.from.value}</div>}
                            {relation.from.textDate && <div className="item">"{relation.from.textDate}"</div>}
                            {relation.from.note && <div className="item note">{relation.from.note}</div>}
                        </div>}
                        {relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL
                        && relation.to
                        && (relation.to.value || relation.to.textDate || relation.to.note)
                        && <div className="flex flex-1 no-wrap-group">
                            <label>{i18n('party.relation.to')+":"}</label>
                            {relation.to.value && <div className="item">{relation.to.value}</div>}
                            {relation.to.textDate && <div className="item">"{relation.to.textDate}"</div>}
                            {relation.to.note && <div className="item note">{relation.to.note}</div>}
                        </div>}
                        {relation.relationEntities && relation.relationEntities.map(entity =>
                        <div className="flex flex-1 no-wrap-group" key={entity.id}>
                            <label>{entity.roleType.name}:</label>
                            <div className="item">{entity.record.record}</div>
                            <div className="item note">{entity.record.note}</div>
                        </div>)}
                        {relation.note && <div className="note">{relation.note}</div>}
                    </div>
                    <div className="actions">
                        <Button bsStyle="action" onClick={() => this.handleRelationUpdate(relation)}><Icon glyph="fa-pencil" /></Button>
                        <Button className="delete" bsStyle="action" onClick={() => this.handleRelationDelete(relation.id)}><Icon glyph="fa-trash" /></Button>
                    </div>
                </div>)}
            </div>);
    }
}

export default connect()(PartyDetailRelations);
