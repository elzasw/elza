import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl, Button} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, Icon} from 'components/shared'
import {indexById} from 'stores/app/utils.jsx'
import {relationCreate, relationUpdate, relationDelete, RELATION_CLASS_TYPE_REPEATABILITY, USE_UNITDATE_ENUM, normalizeDatation} from 'actions/party/party.jsx'
import {RELATION_CLASS_CODES} from '../../constants.tsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import {isNotBlankObject} from 'components/Utils.jsx'

import './PartyDetailRelations.scss'
import RelationClassForm from "./RelationClassForm";
import RelationForm from "./RelationForm";

class PartyDetailRelations extends AbstractReactComponent {

    state = {};

    static propTypes = {
        canEdit: PropTypes.bool.isRequired,
        label: PropTypes.element.isRequired,
        party: PropTypes.object.isRequired,
        partyType: PropTypes.object.isRequired,
        relationClassTypes: PropTypes.object.isRequired,
        apTypesMap: PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.loadState()
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.loadState(nextProps)
    }

    loadState = (nextProps = this.props) => {
        const {partyType, party, relationClassType} = nextProps;
        if (!partyType || !party || !relationClassType) {
            return;
        }
        const allowedRelationTypes = partyType.relationTypes ? partyType.relationTypes.filter(i => i.relationClassType.id === relationClassType.id) : [];

        const allowedRelationTypesMap = getMapFromList(allowedRelationTypes);
        const allowedRelationTypesIds = allowedRelationTypes.map(i => i.id);

        const relations = party.relations.filter(i => allowedRelationTypesIds.indexOf(i.relationTypeId) !== -1);

        this.setState({
            allowedRelationTypes,
            allowedRelationTypesMap,
            allowedRelationTypesIds,
            relations
        })
    };

    addIdentifier = (relation) => {
        const {party} = this.props;
        return this.props.dispatch(relationCreate({
            ...relation,
            partyId: party.id,
            from: isNotBlankObject(relation.from) ? normalizeDatation(relation.from) : null,
            to: isNotBlankObject(relation.to) ? normalizeDatation(relation.to) : null,
        }));
    };

    update = (origRelation, newRelation) => {
        const {party} = this.props;
        return this.props.dispatch(relationUpdate({
            ...origRelation,
            ...newRelation,
            partyId: party.id,
            from: isNotBlankObject(newRelation.from) ? normalizeDatation(newRelation.from) : null,
            to: isNotBlankObject(newRelation.to) ? normalizeDatation(newRelation.to) : null,
        }));
    };

    handleRelationAdd = () => {
        const {label, party, apTypesMap} = this.props;
        const {allowedRelationTypes} = this.state;
        this.props.dispatch(modalDialogShow(this, label, <RelationClassForm partyId={party.id} apTypesMap={apTypesMap} relationTypes={allowedRelationTypes} onSubmitForm={this.addIdentifier} />, "dialog-lg"));
    };

    handleRelationUpdate = (relation) => {
        const {label, party, apTypesMap} = this.props;
        const {allowedRelationTypesMap} = this.state;
        const relationType = allowedRelationTypesMap[relation.relationTypeId];
        this.props.dispatch(modalDialogShow(this, label, <RelationForm apTypesMap={apTypesMap} partyId={party.id} relationType={relationType} initialValues={relation} onSubmitForm={this.update.bind(this, relation)} />, "dialog-lg"));
    };

    handleRelationDelete = (id) => {
        if (confirm(i18n("party.relation.delete.confirm"))) {
            this.props.dispatch(relationDelete(id));
        }
    };

    render() {
        const {label, relationClassType, canEdit} = this.props;
        const {relations, allowedRelationTypesMap} = this.state;

        let addButton = null;
        if (relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.MULTIPLE ||
            (relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.UNIQUE &&
                (!relations || relations.length < 1))) {
            addButton = <Button bsStyle="action" onClick={this.handleRelationAdd}><Icon glyph="fa-plus" /></Button>;
        }

        const relationsArray = relations ? relations : [];

        return (
            <div className="party-detail-relations">
                <div>
                    <label className="group-label">{label}</label>
                    {canEdit && addButton}
                </div>
                {relationsArray.map((relation, index) =>
                <div key={relation.id} className="value-group relation-group flex">
                    <div className="flex-1">
                        <label className="item">{relationClassType.code !== RELATION_CLASS_CODES.RELATION && allowedRelationTypesMap[relation.relationTypeId].name}</label><br />
                        {(allowedRelationTypesMap[relation.relationTypeId].useUnitdate == USE_UNITDATE_ENUM.INTERVAL
                        || allowedRelationTypesMap[relation.relationTypeId].useUnitdate == USE_UNITDATE_ENUM.ONE)
                        && relation.from
                        &&  (relation.from.value || relation.from.textDate || relation.from.note)
                        && <div className="flex flex-1 no-wrap-group">
                            <label>{allowedRelationTypesMap[relation.relationTypeId].useUnitdate ? i18n('party.relation.date')+":" : i18n('party.relation.from')+":"}</label>
                            {relation.from.value && <div className="item">{relation.from.value}</div>}
                            {relation.from.textDate && <div className="item">"{relation.from.textDate}"</div>}
                            {relation.from.note && <div className="item note">{relation.from.note}</div>}
                        </div>}
                        {allowedRelationTypesMap[relation.relationTypeId].useUnitdate == USE_UNITDATE_ENUM.INTERVAL
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
            </div>)
    }
}

export default connect()(PartyDetailRelations);
