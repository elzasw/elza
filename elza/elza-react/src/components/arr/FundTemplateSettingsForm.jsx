import React from 'react';
import {Field, FieldArray, reduxForm} from 'redux-form';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {Accordion, Card, Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {indexById} from 'stores/app/utils.jsx';
import {decorateFormField, submitForm} from 'components/form/FormUtils';

import './FundSettingsForm.scss';
import NoFocusButton from '../shared/button/NoFocusButton';
import FormInput from '../shared/form/FormInput';

import './FundTemplateSettingsForm.scss';
import {JAVA_ATTR_CLASS} from '../../constants';
import FormInputField from "../shared/form/FormInputField";

class FundTemplateSettingsForm extends AbstractReactComponent {
    /**
     * Validace formuláře.
     */
    static validate = (values, props) => {
        const errors = {};
        const names = {};
        let i = 0;
        values.templates.forEach(template => {
            const name = template.name.trim().toLowerCase();
            if (names[name]) {
                errors._error = i18n('arr.fund.template.error.duplicate', template.name);
            } else {
                names[name] = true;
            }
            i++;
        });
        return errors;
    };

    state = {
        open: {},
        edit: {},
    };

    submitReduxForm = (values, dispatch) =>
        submitForm(FundTemplateSettingsForm.validate, values, this.props, this.props.onSubmitForm, dispatch);

    renderItems = formData => {
        const {descItemTypes} = this.props;

        const results = [];
        let i = 0;
        Object.keys(formData).forEach(itemTypeId => {
            const itemType = descItemTypes.items[indexById(descItemTypes.items, parseInt(itemTypeId))];
            const items = formData[itemTypeId];
            items.forEach(item => {
                results.push(this.renderItem(i++, itemType, item));
            });
        });

        return results;
    };

    renderItem = (key, itemType, item) => {
        let spec;
        if (item.descItemSpecId) {
            const itemSpec = itemType.descItemSpecs[indexById(itemType.descItemSpecs, item.descItemSpecId)];
            let csl = 'item-spec';
            if (item[JAVA_ATTR_CLASS] !== '.ArrItemEnumVO') {
                csl += ' item-noenum';
            }
            spec = <span className={csl}>{itemSpec.name}</span>;
        }

        let val;

        switch (item[JAVA_ATTR_CLASS]) {
            case '.ArrItemTextVO':
            case '.ArrItemStringVO':
            case '.ArrItemFormattedTextVO':
                val = item.value && item.value.length > 100 ? item.value.substring(0, 100).trim() + '...' : item.value;
                break;
            case '.ArrItemRecordRefVO':
            case '.ArrItemFileRefVO':
                val = item.strValue;
                break;
            default:
                val = item.value;
        }

        if (item.undefined) {
            val = <span className="item-undefined">{i18n('arr.fund.template.undefined')}</span>;
        }

        return (
            <div key={key}>
                <span className="item-type">{itemType.shortcut}</span>
                {spec}
                {val}
            </div>
        );
    };

    render() {
        const {
            handleSubmit,
            onClose,
            error,
        } = this.props;
        const {edit, open} = this.state;

        return (
            <Form className="templates-form" onSubmit={handleSubmit(this.submitReduxForm)}>
                {error && <div className="form-error">{error}</div>}
                <Modal.Body className="template-items">
                    <FieldArray
                        name={'templates'}
                        component={({fields, meta}) => {
                            if (fields.length === 0) {
                                return <div>{i18n('arr.fund.template.empty')}</div>
                            }

                            return fields.map((item, index, fields) => {
                                const header = (
                                    <div
                                        onClick={e => {
                                            if (edit[index]) {
                                                e.preventDefault();
                                                e.stopPropagation();
                                            }
                                        }}
                                        className="pull-left template-name"
                                    >
                                        {edit[index] ? (
                                                <Field
                                                    name={`${item}.name`}
                                                    type="text"
                                                    component={FormInputField}
                                                    label={false}
                                                />
                                        ) : (
                                            fields.get(index).name.value
                                        )}
                                    </div>
                                );
                                return (
                                    <Accordion
                                        activeKey={open[index]}
                                        onClick={() => {
                                            open[index] = !open[index];
                                            this.setState({open});
                                        }}
                                        accordion
                                    >
                                        <Card>
                                            <Card.Header>
                                                <div className="clearfix">
                                                    {header}
                                                    <div className="pull-right">
                                                        <NoFocusButton
                                                            className={'btn-action'}
                                                            onClick={e => {
                                                                const newEdit = {};
                                                                newEdit[index] = !edit[index];
                                                                this.setState({edit: newEdit});
                                                                e.stopPropagation();
                                                            }}
                                                        >
                                                            <Icon glyph="fa-edit" />
                                                        </NoFocusButton>
                                                        <NoFocusButton
                                                            className={'btn-action'}
                                                            onClick={e => {
                                                                this.setState({edit: {}});
                                                                fields.remove(index);
                                                                e.stopPropagation();
                                                            }}
                                                        >
                                                            <Icon glyph="fa-trash" />
                                                        </NoFocusButton>
                                                    </div>
                                                </div>
                                            </Card.Header>
                                            <div
                                                onClick={e => {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                }}
                                            >
                                                {this.renderItems(fields.get(index).formData)}
                                            </div>
                                        </Card>
                                    </Accordion>
                                );
                            });
                        }}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary">
                        {i18n('visiblePolicy.action.save')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

function mapState(state) {
    const {refTables} = state;
    return {
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
    };
}

const connector = connect(mapState);

export default reduxForm({
    form: 'fundTemplateSettingsForm',
})(connector(FundTemplateSettingsForm));
