import React from 'react';

import {reduxForm} from 'redux-form';
import {FormInput, HorizontalLoader, Icon} from 'components/shared';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx';
import {Form, FormCheck, FormControl, Modal, Table} from 'react-bootstrap';
import {Button} from '../ui';
import objectById from '../../shared/utils/objectById';
import * as perms from 'actions/user/Permission.jsx';
import AbstractReactComponent from '../AbstractReactComponent';
import i18n from '../i18n';

import './ExtMapperForm.scss';

const INTERPI_CLASS = {
    POCATEK_EXISTENCE: 'POCATEK_EXISTENCE',
    KONEC_EXISTENCE: 'KONEC_EXISTENCE',
    ZMENA: 'ZMENA',
    UDALOST: 'UDALOST',
    SOUVISEJICI_ENTITA: 'SOUVISEJICI_ENTITA',
};

class ExtMapperForm extends AbstractReactComponent {
    componentDidMount() {
        this.props.dispatch(refPartyTypesFetchIfNeeded());
    }

    UNSAFE_componentWillReceiveProps() {
        this.props.dispatch(refPartyTypesFetchIfNeeded());
    }

    static validate = (data) => {
        const error = {};
        error.mappings = data.mappings.map(i => {

            if (!i.importRelation) {
                return null;
            }

            const entities = i.entities ? i.entities.map(e => e.importEntity && !e.relationRoleTypeId ? {relationRoleTypeId: i18n('global.validation.required')} : null) : [];
            const relationTypeId = !i.relationTypeId ? i18n('global.validation.required') : null;
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
            return <HorizontalLoader/>;
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
                    <Button variant="link" type="button" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>;
        }

        const partyType = objectById(partyTypes, partyTypeId.value);

        return <Form name="extMapperForm" onSubmit={handleSubmit}>
            <Modal.Body>
                <label>{i18n('extMapperForm.relationMapping')}</label>
                <div style={{border: '1px solid #ddd', maxHeight: '500px', overflowY: 'auto'}}>
                    <Table>
                        {mappings.map(i => {
                            const relationType = i.relationTypeId.value ? objectById(partyType.relationTypes, i.relationTypeId.value) : null;
                            const relationRoleTypes = relationType && relationType.relationRoleTypes ? relationType.relationRoleTypes : [];
                            return <tbody>
                            <tr>
                                <th colSpan={5}>{i18n('extMapperForm.import')}</th>
                            </tr>
                            <tr className="import-relation">
                                <td><FormCheck {...i.importRelation}/></td>
                                <td>{i.interpiRelationType.value}</td>
                                <td><Icon glyph="fa-arrow-right"/></td>
                                <td/>
                                <td>
                                    <FormInput as="select" disabled={!hasPermission} {...i.relationTypeId}
                                               onChange={(ev) => {
                                                   const save = window.confirm(i18n('extMapperForm.saveAsDefaultMapping'));
                                                   i.save.onChange(save);
                                                   i.relationTypeId.onChange(ev);
                                               }}>
                                        <option key="null"/>
                                        {partyType.relationTypes.map(r => <option value={r.id}
                                                                                  key={r.id}>{r.name}</option>)}
                                    </FormInput>
                                </td>
                            </tr>

                            {i.entities && i.entities.map(e => <tr className="import-relation">
                                <td><FormCheck {...e.importEntity}/></td>
                                <td>{e.interpiEntityName.value}</td>
                                <td>{e.interpiRoleType.value}</td>
                                <td><Icon glyph="fa-arrow-right"/></td>
                                <td>
                                    <FormInput as="select"
                                               disabled={!hasPermission || !i.relationTypeId.value} {...e.relationRoleTypeId}
                                               onChange={(ev) => {
                                                   const save = window.confirm(i18n('extMapperForm.saveAsDefaultMapping'));
                                                   e.save.onChange(save);
                                                   e.relationRoleTypeId.onChange(ev);
                                               }}>
                                        <option key="null"/>
                                        {i.relationTypeId.value && relationRoleTypes.map(r => <option value={r.id}
                                                                                                      key={r.id}>{r.name}</option>)}
                                    </FormInput>
                                </td>
                            </tr>)}
                            </tbody>;
                        })}
                    </Table>
                </div>
                <div>
                    <label>{i18n('extMapperForm.recordExtSystemDescription')}</label>
                    <FormControl as="textarea" rows="10" value={record ? record.detail : ''} style={{height: '272px'}}/>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit"
                        disabled={submitting}>{isUpdate ? i18n('extMapperForm.update') : i18n('extMapperForm.import')}</Button>
                <Button variant="link" type="button" onClick={onClose}
                        disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
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
        'mappings[].entities[].id',
        'mappings[].entities[].interpiClass',
        'mappings[].entities[].interpiEntityName',
        'mappings[].entities[].interpiRoleType',
        'mappings[].entities[].relationRoleTypeId',
        'mappings[].entities[].interpiEntityType',
        'mappings[].entities[].interpiId',
        'mappings[].entities[].importEntity',
        'mappings[].entities[].save',
    ],
    form: 'extMapperForm',
    validate: ExtMapperForm.validate,
}, (state) => ({
    partyTypes: state.refTables.partyTypes.fetched ? state.refTables.partyTypes.items : false,
    userDetail: state.userDetail,
}))(ExtMapperForm);
