import PropTypes from 'prop-types';
import React from 'react';
import {FieldArray, reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {Form, FormGroup, FormLabel, Modal, Button} from 'react-bootstrap';
//import {Button} from '../../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import './AddDescItemTypeForm.scss';
import {ItemTypeField} from 'components/arr/nodeForm/ItemTypeField';
import FF from '../../shared/form/FF';
import {getInfoSpecType} from '../../../stores/app/accesspoint/itemFormUtils';

/**
 * Formulář přidání nové desc item type.
 */

class AddDescItemTypeForm extends AbstractReactComponent {
    static validate = (values, props) => {
        const errors = {};

        if (!values.descItemTypeId) {
            errors.descItemTypeId = i18n('global.validation.required');
        }

        return errors;
    };

    static propTypes = {
        descItemTypes: PropTypes.array.isRequired,
    };

    static ITEM_TYPE_POSSIBLE = 'POSSIBLE';

    constructor(props) {
        super(props);

        // Stromové uspořádání možných položek
        const possibleItemTypes = [];
        this.props.descItemTypes.forEach(node => {
            const children = [];
            node.children.forEach(item => {
                const itemType = getInfoSpecType(item.type);
                item.className = 'type-' + itemType.toLowerCase();
                if (itemType === AddDescItemTypeForm.ITEM_TYPE_POSSIBLE) {
                    children.push(item);
                }
            });
            if (children.length > 0) {
                possibleItemTypes.push({
                    ...node,
                    children,
                });
            }
        });

        this.state = {
            possibleItemTypes,
        };
    }

    submitOptions = {finishOnSubmit: true};

    submitReduxForm = (values, dispatch) =>
        submitForm(
            AddDescItemTypeForm.validate,
            values,
            this.props,
            this.props.onSubmitForm,
            dispatch,
            this.submitOptions,
        );

    selectDescItem = item => {
        this.submitReduxForm({descItemTypeId: item}, this.props.dispatch);
    };

    render() {
        const {handleSubmit, onClose, descItemTypes, submitting} = this.props;
        const {possibleItemTypes} = this.state;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <div>
                        {possibleItemTypes.map((node, index) => {
                            return (
                                <FormGroup key={index}>
                                    <FormLabel className={'d-block'}>{node.name}</FormLabel>
                                    {node.children.map(item => (
                                        <Button
                                            className="add-link"
                                            key={item.id}
                                            onClick={this.selectDescItem.bind(this, item)}
                                        >
                                            <Icon glyph="fa-plus" />
                                            {item.name}
                                        </Button>
                                    ))}
                                </FormGroup>
                            );
                        })}
                    </div>
                    <div className="autocomplete-desc-item-type">
                        <FF
                            name="descItemTypeId"
                            field={ItemTypeField}
                            label={i18n('subNodeForm.descItemType.all')}
                            descItemTypes={descItemTypes}
                        />
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.add')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'addDescItemTypeForm',
})(AddDescItemTypeForm);
