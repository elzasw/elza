import React from 'react';
import {Form, FormCheck, Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import {AbstractReactComponent, FormInput, i18n} from 'components/shared';
import {reduxForm, Field} from 'redux-form';
import {connect} from 'react-redux';
import StructureSubNodeForm from './StructureSubNodeForm';
import {structureNodeFormFetchIfNeeded, structureNodeFormSelectId} from '../../../actions/arr/structureNodeForm';
import PropTypes from 'prop-types';
import FF from '../../shared/form/FF';

class AddStructureDataForm extends AbstractReactComponent {
    static propTypes = {
        multiple: PropTypes.bool,
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        structureData: PropTypes.object.isRequired,
        descItemFactory: PropTypes.func.isRequired,
    };

    static defaultProps = {
        multiple: false,
    };

    UNSAFE_componentWillMount() {
        const {fundVersionId, structureData} = this.props;
        this.props.dispatch(structureNodeFormSelectId(fundVersionId, structureData.id));
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, structureData.id));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {
            fundVersionId,
            structureData: {id},
        } = nextProps;
        if (id !== this.props.structureData.id) {
            this.props.dispatch(structureNodeFormSelectId(fundVersionId, id));
        }
        if (id) {
            this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
        }
    }

    customRender = (code, infoType) => {
        if (code === 'INT') {
            /*const index = incrementedTypeIds.value.indexOf(infoType.id);
            const checked = index !== -1;*/

            return (
                <FF
                    key={'increment'}
                    type="checkbox"
                    name={'incrementedTypeIds'}
                    label={i18n('arr.structure.modal.increment')}
                />
            );
        }
        return null;
    };

    render() {
        const {
            error,
            handleSubmit,
            onClose,
            submitting,
            structureData,
            fundVersionId,
            fundId,
            multiple,
            storeStructure,
        } = this.props;

        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {error && <p>{error}</p>}
                    {storeStructure && storeStructure.fetched && (
                        <StructureSubNodeForm
                            id={structureData.id}
                            versionId={fundVersionId}
                            fundId={fundId}
                            selectedSubNodeId={structureData.id}
                            customActions={multiple && this.customRender} // pokud form je mnohonásobný renderujeme doplňkově inkrementaci
                            descItemFactory={this.props.descItemFactory}
                        />
                    )}
                    {multiple && (
                        <FF name="count" min="2" type="number" label={i18n('arr.structure.modal.addMultiple.count')} />
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.add')}
                    </Button>
                    <Button variant="link" disabled={submitting} onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const form = reduxForm({
    form: 'AddStructureData',
    initialValues: {
        count: '',
        incrementedTypeIds: [],
    },
    validate: (values, props) => {
        const errors = {};
        if (props.multiple) {
            if (!values.count || values.count < 2) {
                errors.count = i18n('arr.structure.modal.addMultiple.error.count.tooSmall');
            }
            if (!values.incrementedTypeIds || values.incrementedTypeIds.length < 1) {
                errors._error = i18n('arr.structure.modal.addMultiple.error.itemTypeIds.required');
            }
        }
        return errors;
    },
})(AddStructureDataForm);

export default connect((state, props) => {
    const {structures} = state;

    const key = props.structureData.id ? String(props.structureData.id) : null;
    return {
        storeStructure:
            key && props.structureData && structures.stores.hasOwnProperty(key) ? structures.stores[key] : null,
    };
})(form);
