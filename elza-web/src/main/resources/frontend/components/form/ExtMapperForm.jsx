import React from 'react';
import ReactDOM from 'react-dom';

import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput, Icon, Loading} from 'components'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {Modal, Form, Table, Checkbox, FormControl, Button} from 'react-bootstrap'
import objectById from '../../shared/utils/objectById'
import * as perms from 'actions/user/Permission.jsx';


const INTERPI_CLASS = {
    POCATEK_EXISTENCE: 'POCATEK_EXISTENCE',
    KONEC_EXISTENCE: 'KONEC_EXISTENCE',
    ZMENA: 'ZMENA',
    UDALOST: 'UDALOST',
    SOUVISEJICI_ENTITA: 'SOUVISEJICI_ENTITA',
};

class ExtMapperForm extends AbstractReactComponent {
    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    componentWillReceiveProps() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    static validate = (data) => {
        const error = {};
        error.mappings = data.mappings.map(i => {
            const entities = i.entities ? i.entities.map(e => e.relationRoleTypeId === null ? {relationRoleTypeId: i18n('global.validation.required')} : null) : [];
            const relationTypeId = i.relationTypeId === null ? i18n('global.validation.required') : null;
            const isEntitiesOk = entities.length === 0;
            const isRelationTypeIdOk = relationTypeId === null;
            if (isEntitiesOk && isRelationTypeIdOk) {
                return null;
            }

            const res = {};
            if (!isEntitiesOk) res.entities = entities;
            if (!isRelationTypeIdOk) res.relationTypeId = relationTypeId;
            return res;
        });
        if (error.mappings.filter(i => i !== null).length === 0) {
            return {};
        }
        return error;
    };

    render() {
        const {handleSubmit, submitting, onClose, fields: {mappings, partyTypeId}, partyTypes, record, userDetail, isUpdate} = this.props;

        if (partyTypes === false) {
            return <Loading />;
        }
        const hasPermission = userDetail.hasOne(perms.INTERPI_MAPPING_WR);

        if (!hasPermission && mappings.filter(i => i.relationTypeId.value === null || i.entities.filter(e => e.relationRoleTypeId.value === null).length > 0).length > 0) {
            return <div>
                <Modal.Body>
                    <div className="text-center">
                        <h3>{i18n('extMapperForm.insufficientPrivileges')}</h3>
                        <p>{i18n('extMapperForm.idForImport', record.recordId)}</p>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" type="button" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        }

        const partyType = objectById(partyTypes, partyTypeId.value);

        return <Form name="extMapperForm" onSubmit={handleSubmit}>
            <Modal.Body>
                <label>{i18n('extMapperForm.relationMapping')}</label>
                <div style={{border: "1px solid #ddd", maxHeight: '500px', overflowY:'auto'}}>
                    <Table>
                        {mappings.map(i => {
                            const relationType = i.relationTypeId.value ? objectById(partyType.relationTypes, i.relationTypeId.value) : null;
                            const relationRoleTypes = relationType && relationType.relationRoleTypes ? relationType.relationRoleTypes : [];
                            return <tbody>
                                <tr>
                                    <th colSpan={5}>{i18n('extMapperForm.import')}</th>
                                </tr>
                                <tr>
                                    <td><Checkbox {...i.importRelation}/></td>
                                    <td>{i.interpiRelationType.value}</td>
                                    <td><Icon glyph="fa-arrow-right" /></td>
                                    <td/>
                                    <td>
                                        <FormInput componentClass="select" disabled={!hasPermission || i.importRelation.value === false} {...i.relationTypeId} onChange={(ev) => {
                                            const save = confirm(i18n('extMapperForm.saveAsDefaultMapping'));
                                            i.save.onChange(save);
                                            i.relationTypeId.onChange(ev);
                                        }}>
                                            <option key="null"/>
                                            {partyType.relationTypes.map(r => <option value={r.id} key={r.id}>{r.name}</option>)}
                                        </FormInput>
                                    </td>
                                </tr>

                                {i.entities && i.entities.map(e => <tr>
                                    <td><Checkbox {...e.importEntity}/></td>
                                    <td>{e.interpiEntityName.value}</td>
                                    <td>{e.interpiRoleType.value}</td>
                                    <td><Icon glyph="fa-arrow-right" /></td>
                                    <td>
                                        <FormInput componentClass="select" disabled={!hasPermission || i.importRelation.value === false || !i.relationTypeId.value || e.importEntity.value === false} {...e.relationRoleTypeId} onChange={(ev) => {
                                            const save = confirm(i18n('extMapperForm.saveAsDefaultMapping'));
                                            e.save.onChange(save);
                                            e.relationRoleTypeId.onChange(ev);
                                        }} >
                                            <option key="null"/>
                                            {i.relationTypeId.value && relationRoleTypes.map(r => <option value={r.id} key={r.id}>{r.name}</option>)}
                                        </FormInput>
                                    </td>
                                </tr>)}
                            </tbody>
                        })}
                    </Table>
                </div>
                <div>
                    <label>{i18n('extMapperForm.recordExtSystemDescription')}</label>
                    <FormControl componentClass="textarea" rows="10" value={record ? record.detail : ''} style={{height: '272px'}} />
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{isUpdate ? i18n('extMapperForm.update') : i18n('extMapperForm.import')}</Button>
                <Button bsStyle="link" type="button" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

export default reduxForm({
    fields: [
        'partyTypeId',
        'mappings[].id',
        'mappings[].relationTypeId',
        'mappings[].interpiClass',
        'mappings[].interpiRelationType',
        'mappings[].importRelation',
        'mappings[].save',
        'mappings[].relationEntities[].id',
        'mappings[].relationEntities[].interpiClass',
        'mappings[].relationEntities[].interpiEntityName',
        'mappings[].relationEntities[].interpiRoleType',
        'mappings[].relationEntities[].relationRoleTypeId',
        'mappings[].relationEntities[].importEntity',
        'mappings[].relationEntities[].save',
    ],
    form: 'extMapperForm',
    validate: ExtMapperForm.validate
}, (state) => ({
    partyTypes: state.refTables.partyTypes.fetched ? state.refTables.partyTypes.items : false,
    userDetail: state.userDetail
}))(ExtMapperForm);
