import PropTypes from 'prop-types';
import React from 'react';
import {FieldArray, reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {Form, FormGroup, FormLabel, Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import {submitForm} from 'components/form/FormUtils.jsx';
import './AddDescItemTypeForm.scss';
import {ItemTypeField} from 'components/arr/nodeForm/ItemTypeField';

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
                item.className = 'type-' + item.type.toLowerCase();
                if (item.type === AddDescItemTypeForm.ITEM_TYPE_POSSIBLE) {
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

    render() {
        const {
            fields: {descItemTypeId},
            handleSubmit,
            onClose,
            descItemTypes,
            submitting,
        } = this.props;
        const {possibleItemTypes} = this.state;

        return (
            <Form onSubmit={handleSubmit(this.submitReduxForm)}>
                <Modal.Body>
                    <div>
                        {possibleItemTypes.map((node, index) => {
                            return (
                                <FormGroup key={index}>
                                    <FormLabel>{node.name}</FormLabel>
                                    <div>
                                        {node.children.map(item => {
                                            return (
                                                <button
                                                    className="add-link btn btn-link"
                                                    key={item.id}
                                                    onClick={() => {
                                                        this.submitReduxForm(
                                                            {descItemTypeId: item},
                                                            this.props.dispatch,
                                                        );
                                                    }}
                                                >
                                                    <Icon glyph="fa-plus" />
                                                    {item.name}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </FormGroup>
                            );
                        })}
                    </div>
                    <div className="autocomplete-desc-item-type">
                        <FieldArray
                            name="descItemTypeId"
                            component={ItemTypeField}
                            label={i18n('arr.fund.dateRange')}
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
