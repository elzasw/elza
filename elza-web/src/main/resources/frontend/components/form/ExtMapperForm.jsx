import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput, Icon, Loading} from 'components'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {Modal, Form, Table, Checkbox, FormControl, Button} from 'react-bootstrap'
import objectById from '../../shared/utils/objectById'


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

    render() {
        const {handleSubmit, submitting, onClose, fields: {mappings, partyTypeId}, partyTypes, record} = this.props;

        if (partyTypes === false) {
            return <Loading />;
        }

        const partyType = objectById(partyTypes, partyTypeId.value);

        return <Form name="extMapperForm" onSubmit={handleSubmit}>
            <Modal.Body>
                <label>{i18n('extMapperForm.relationMapping')}</label>
                <Table>
                    <thead>
                        <tr>
                            <th colSpan={5}>Importovat</th>
                        </tr>
                    </thead>
                    {mappings.filter(i => i.importRelation.value === true).map(i => {
                        const relationType = i.relationTypeId.value ? objectById(partyType.relationTypes, i.relationTypeId.value) : null;
                        const relationRoleTypes = relationType && relationType.relationRoleTypes ? relationType.relationRoleTypes : [];
                        return <tbody>
                            <tr>
                                <td><Checkbox {...i.importRelation}/></td>
                                <td>{i.interpiRelationType.value}</td>
                                <td><Icon glyph="fa-arrow-right" /></td>
                                <td/>
                                <td>
                                    <FormInput componentClass="select" {...i.relationTypeId} onChange={(ev) => {
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
                                    <FormInput componentClass="select" disabled={!i.relationTypeId.value || !e.importEntity.value} {...e.relationRoleTypeId} onChange={(ev) => {
                                        const save = confirm(i18n('extMapperForm.saveAsDefaultMapping'));
                                        e.save.onChange(save);
                                        e.relationRoleTypeId.onChange(ev);
                                    }}>
                                        <option key="null"/>
                                        {i.relationTypeId.value && relationRoleTypes.map(r => <option value={r.id} key={r.id}>{r.name}</option>)}
                                    </FormInput>
                                </td>
                            </tr>)}
                        </tbody>
                    })}

                    <thead>
                    <tr>
                        <th colSpan={5}>Neimportovat</th>
                    </tr>
                    </thead>
                    {mappings.filter(i => i.importRelation.value === false).map(i => <tbody>
                    <tr>
                        <td><Checkbox {...i.importRelation}/></td>
                        <td>{i.interpiRelationType.value}</td>
                        <td><Icon glyph="fa-arrow-right" /></td>
                        <td/>
                        <td>
                            <FormInput componentClass="select" {...i.relationTypeId} disabled={true}>
                                <option key="null" />
                            </FormInput>
                        </td>
                    </tr>
                    {i.entities && i.entities.map(e => <tr>
                        <td><Checkbox {...e.importEntity}/></td>
                        <td>{e.interpiEntityName.value}</td>
                        <td>{e.interpiRoleType.value}</td>
                        <td><Icon glyph="fa-arrow-right" /></td>
                        <td>
                            <FormInput componentClass="select" {...e.relationRoleTypeId} disabled={true}>
                                <option key="null" />
                            </FormInput>
                        </td>
                    </tr>)}

                    </tbody>)}
                </Table>
                <div>
                    <label>{i18n('extMapperForm.recordExtSystemDescription')}</label>
                    <FormControl componentClass="textarea" rows="10" value={record ? record.detail : ''} style={{height: '272px'}} />
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('extImport.update')}</Button>
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
    form: 'extMapperForm'
}, (state) => ({
    partyTypes: state.refTables.partyTypes.fetched ? state.refTables.partyTypes.items : false
}))(ExtMapperForm);
