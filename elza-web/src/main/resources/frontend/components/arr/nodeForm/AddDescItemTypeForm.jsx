import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap';
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

    onChange = (value) => {
        console.log("a", value);
    };

    renderItem(item, isHighlighted, isSelected) {
        var cls = 'item';

        cls += ' type-' + item.type.toLowerCase();

        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return (
                <div className={cls} key={item.id} >
                    <div className="name" title={item.name}>{item.name}</div>
                </div>
        )
    }

    render() {
        const {fields: {descItemTypeId}, handleSubmit, onClose, descItemTypes} = this.props;

        var submitForm = submitReduxForm.bind(this, AddDescItemTypeForm.validate);

        var descItemTypeValue;
        if (typeof descItemTypeId.value !== 'undefined') {
            var index = indexById(descItemTypes, descItemTypeId.value);
            if (index !== null) {
                descItemTypeValue = descItemTypes[index]
            }
        }

        return <Form onSubmit={handleSubmit(submitForm)}>
                <Modal.Body>
                    <div>
                        {descItemTypes.map(i => {
                            if (i.type == AddDescItemTypeForm.ITEM_TYPE_POSSIBLE) {
                                return <a className="add-link btn btn-link" key={i.id} onClick={() => {
                                    this.props.onSubmit2({descItemTypeId: i});
                               }}><Icon glyph="fa-plus" />{i.name}</a>
                            }
                        })}
                    </div>
                    <div className="autocomplete-desc-item-type">
                        <Autocomplete
                            label={i18n('subNodeForm.descItemType')}
                            {...descItemTypeId}
                            {...decorateFormField(descItemTypeId)}
                            //value={descItemTypeValue}
                            items={descItemTypes}
                            renderItem={this.renderItem}
                            onBlurValidation={false}
                        />
                    </div>
                    {false && <FormInput componentClass="select" label={i18n('subNodeForm.descItemType')} {...descItemTypeId} {...decorateFormField(descItemTypeId)}>
                        <option key="blank"/>
                        {descItemTypes.map(i=> <option value={i.id}>{i.name}</option>)}
                    </FormInput>}
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



