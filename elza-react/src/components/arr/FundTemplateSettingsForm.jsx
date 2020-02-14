import React from 'react';
import {reduxForm} from 'redux-form';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {Button, Form, Modal, Panel, PanelGroup} from 'react-bootstrap';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {Shortcuts} from 'react-shortcuts';
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {visiblePolicyFetchIfNeeded} from 'actions/arr/visiblePolicy.jsx'

import './FundSettingsForm.scss';
import NoFocusButton from "../shared/button/NoFocusButton";
import FormInput from "../shared/form/FormInput";

import './FundTemplateSettingsForm.scss';

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
        edit: {}
    };

    submitReduxForm = (values, dispatch) => submitForm(FundTemplateSettingsForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    renderItems = (formData) => {
        const {descItemTypes} = this.props;

        const results = [];
        let i = 0;
        Object.keys(formData).map(itemTypeId => {
            const itemType = descItemTypes.items[indexById(descItemTypes.items, itemTypeId)];
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
            let csl = "item-spec";
            if (item['@class'] !== '.ArrItemEnumVO') {
                csl += " item-noenum";
            }
            spec = <span className={csl}>{itemSpec.name}</span>
        }

        let val;

        switch (item['@class']) {
            case '.ArrItemTextVO':
            case '.ArrItemStringVO':
            case '.ArrItemFormattedTextVO':
                val = item.value && item.value.length > 100 ? item.value.substring(0, 100).trim() + "..." : item.value;
                break;
            case '.ArrItemRecordRefVO':
            case '.ArrItemFileRefVO':
            case '.ArrItemPartyRefVO':
                val = item.strValue;
                break;
            default:
                val = item.value;
        }

        if (item.undefined) {
            val = <span className="item-undefined">{i18n('arr.fund.template.undefined')}</span>
        }

        return <div key={key}><span className="item-type">{itemType.shortcut}</span>{spec}{val}</div>
    };

    render() {
        const {fields: {templates}, handleSubmit, onClose, error} = this.props;
        const {edit, open} = this.state;

        return <Form className="templates-form" onSubmit={handleSubmit(this.submitReduxForm)}>
                {error && <div className="form-error">{error}</div>}
                <Modal.Body className="template-items">
                    {templates.map((val, index) => {
                        const header = <div onClick={(e) => {if (edit[index]) {e.preventDefault(); e.stopPropagation();}}} className="pull-left template-name">{edit[index] ? <FormInput type="text" label={false} {...val.name}  {...decorateFormField(val.name)} /> : val.name.value}</div>;
                        return <PanelGroup activeKey={open[index]} onClick={() => {
                            open[index] = !open[index];
                            this.setState({open});
                        }} accordion>
                            <Panel eventKey={true}
                                   header={<div className="clearfix">
                                       {header}
                                       <div className="pull-right">
                                           <NoFocusButton className={"btn-action"} onClick={(e) => {
                                               const newEdit = {};
                                               newEdit[index] = !edit[index];
                                               this.setState({edit: newEdit});
                                               e.stopPropagation();
                                           }}>
                                               <Icon glyph="fa-edit" />
                                           </NoFocusButton>
                                           <NoFocusButton className={"btn-action"} onClick={(e) => {
                                               this.setState({edit: {}});
                                               templates.removeField(index);
                                               e.stopPropagation();
                                           }}>
                                               <Icon glyph="fa-trash" />
                                           </NoFocusButton>
                                       </div>
                                   </div>}>
                                <div onClick={(e) => {e.preventDefault(); e.stopPropagation();}}>
                                     {this.renderItems(val.formData.initialValue)}
                                </div>
                            </Panel>
                        </PanelGroup>

                    })}
                    {templates.length === 0 && <div>
                        {i18n('arr.fund.template.empty')}
                    </div>}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit">{i18n('visiblePolicy.action.save')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
    }
}

function mapStateToProps(state) {
    const {refTables} = state
    return {
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
    }
}

export default connect(mapStateToProps)(reduxForm({
    form: 'fundTemplateSettingsForm',
    fields: ['templates[].formData', 'templates[].withValues',  'templates[].name']
})(FundTemplateSettingsForm))
