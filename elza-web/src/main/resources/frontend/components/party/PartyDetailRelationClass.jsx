import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, NoFocusButton, Icon, RelationClassForm, RelationForm} from 'components/index.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {relationCreate, relationUpdate, relationDelete, RELATION_CLASS_TYPE_REPEATABILITY, USE_UNITDATE_ENUM, RELATION_CLASS_RELATION_CODE, normalizeDatation} from 'actions/party/party.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import {isNotBlankObject} from 'components/Utils.jsx'

import './PartyDetailRelations.less'

class PartyDetailRelations extends AbstractReactComponent {

    state = {};

    static PropTypes = {
        canEdit: React.PropTypes.bool.isRequired,
        label: React.PropTypes.element.isRequired,
        party: React.PropTypes.object.isRequired,
        partyType: React.PropTypes.object.isRequired,
        relationClassTypes: React.PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.loadState()
    }

    componentWillReceiveProps(nextProps) {
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
        this.dispatch(relationCreate({
            ...relation,
            partyId: party.id,
            from: isNotBlankObject(relation.from) ? normalizeDatation(relation.from) : null,
            to: isNotBlankObject(relation.to) ? normalizeDatation(relation.to) : null,
        }));
        this.dispatch(modalDialogHide());
    };

    update = (origRelation, newRelation) => {
        const {party} = this.props;
        this.dispatch(relationUpdate({
            ...origRelation,
            ...newRelation,
            partyId: party.id,
            from: isNotBlankObject(newRelation.from) ? normalizeDatation(newRelation.from) : null,
            to: isNotBlankObject(newRelation.to) ? normalizeDatation(newRelation.to) : null,
        }));
        this.dispatch(modalDialogHide());
    };

    handleRelationAdd = () => {
        const {label, party} = this.props;
        const {allowedRelationTypes} = this.state;
        this.dispatch(modalDialogShow(this, label, <RelationClassForm partyId={party.id} relationTypes={allowedRelationTypes} onSubmitForm={this.addIdentifier} />, "dialog-lg"));
    };

    handleRelationUpdate = (relation) => {
        const {label, party} = this.props;
        const {allowedRelationTypesMap} = this.state;
        const relationType = allowedRelationTypesMap[relation.relationTypeId];
        this.dispatch(modalDialogShow(this, label, <RelationForm partyId={party.id} relationType={relationType} initialValues={relation} onSubmitForm={this.update.bind(this, relation)} />, "dialog-lg"));
    };

    handleRelationDelete = (id) => {
        if (confirm(i18n("party.relation.delete.confirm"))) {
            this.dispatch(relationDelete(id));
        }
    };

    render() {
        const {label, relationClassType} = this.props;
        const {relations, allowedRelationTypesMap} = this.state;

        let addButton = null;
        if (relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.MULTIPLE ||
            (relationClassType.repeatability == RELATION_CLASS_TYPE_REPEATABILITY.UNIQUE &&
                (!relations || relations.length < 1))) {
            addButton = <NoFocusButton bsStyle="default" onClick={this.handleRelationAdd}><Icon glyph="fa-plus" /></NoFocusButton>;
        }

        const relationsArray = relations ? relations : [];

        return <div className="party-detail-relations">
            <div>
                <label>{label}</label>
                {addButton}
            </div>
            {relationsArray.map((relation, index) => <div key={relation.id} className="value-group relation-group flex">
                <div className="flex-1">
                    {(allowedRelationTypesMap[relation.relationTypeId].useUnitdate == USE_UNITDATE_ENUM.INTERVAL || allowedRelationTypesMap[relation.relationTypeId].useUnitdate == USE_UNITDATE_ENUM.ONE) && relation.from &&  relation.from.value && <div className="flex flex-1 no-wrap-group">
                        <div className="item">{relationClassType.code !== RELATION_CLASS_RELATION_CODE && allowedRelationTypesMap[relation.relationTypeId].name + ": "}</div>
                        {relation.from.value && <div className="item">{relation.from.value}</div>}
                        {relation.from.textDate && <div className="item">{relation.from.textDate}</div>}
                            {relation.from.note && <div className="note">{relation.from.note}</div>}
                    </div>}
                    {allowedRelationTypesMap[relation.relationTypeId].useUnitdate == USE_UNITDATE_ENUM.INTERVAL && relation.to && relation.to.value && <div className="flex flex-1 no-wrap-group">
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
                    <NoFocusButton bsStyle="action" onClick={() => this.handleRelationUpdate(relation)}><Icon glyph="fa-pencil" /></NoFocusButton>
                    <NoFocusButton bsStyle="action" onClick={() => this.handleRelationDelete(relation.id)}><Icon glyph="fa-times" /></NoFocusButton>
                </div>
            </div>)}
        </div>
    }
}

export default connect()(PartyDetailRelations);
