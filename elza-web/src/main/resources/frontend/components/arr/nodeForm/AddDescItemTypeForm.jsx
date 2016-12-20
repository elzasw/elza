import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button, Form, ControlLabel, FormGroup} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import './AddDescItemTypeForm.less';

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
        onSubmit2: React.PropTypes.func.isRequired,
        descItemTypes: React.PropTypes.array.isRequired,
    };

    static ITEM_TYPE_POSSIBLE = 'POSSIBLE';

    constructor(props) {
        super(props);

        // Stromové uspořádání možných položek
        const possibleItemTypes = [];
        this.props.descItemTypes.forEach(node => {
            const children = [];
            node.children.forEach(item => {
                if (item.type === AddDescItemTypeForm.ITEM_TYPE_POSSIBLE) {
                    children.push(item);
                }
            });
            if (children.length > 0) {
                possibleItemTypes.push({
                    ...node,
                    children
                });
            }
        });

        this.state = {
            possibleItemTypes
        }
    }

    render() {
        const {fields: {descItemTypeId}, handleSubmit, onClose, descItemTypes} = this.props;
        const {possibleItemTypes} = this.state;

        var submitForm = submitReduxForm.bind(this, AddDescItemTypeForm.validate);

        return <Form onSubmit={handleSubmit(submitForm)}>
                <Modal.Body>
                    <div>
                        {possibleItemTypes.map(node => {
                            return <FormGroup>
                                <ControlLabel>{node.name}</ControlLabel>
                                <div>
                                    {node.children.map(item => {
                                        return <a className="add-link btn btn-link" key={item.id} onClick={() => {
                                            this.props.onSubmit2({descItemTypeId: item});
                                       }}><Icon glyph="fa-plus" />{item.name}</a>
                                    })}
                                </div>
                            </FormGroup>
                        })}
                    </div>
                    <div className="autocomplete-desc-item-type">
                        <Autocomplete
                            tree
                            label={i18n('subNodeForm.descItemType.all')}
                            {...descItemTypeId}
                            {...decorateFormField(descItemTypeId)}
                            items={descItemTypes}
                            getItemRenderClass={item => item.groupItem ? null : ' type-' + item.type.toLowerCase()}
                            alwaysExpanded
                            allowSelectItem={(id, item) => !item.groupItem}
                            onBlurValidation={false}
                        />
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={handleSubmit(submitForm)}>{i18n('global.action.add')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
    }
}

export default reduxForm({
    form: 'addDescItemTypeForm',
    fields: ['descItemTypeId'],
})(AddDescItemTypeForm)



