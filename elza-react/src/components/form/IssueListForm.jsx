import PropTypes from 'prop-types';
import * as React from 'react';
import {connect} from 'react-redux';
import {Col, Form, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {AbstractReactComponent, Icon} from 'components/shared';
import UserField from '../admin/UserField';
import FormInput from 'components/shared/form/FormInput';
import { defineMessages, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    name: { id: 'issueList.name', defaultMessage: 'Název' },
    open: { id: 'issueList.open', defaultMessage: 'Stav' },
    openTrue: { id: 'issueList.open.true', defaultMessage: 'Otevřený' },
    openFalse: { id: 'issueList.open.false', defaultMessage: 'Zavřený' },
    permission: { id: 'arr.issuesList.form.permission', defaultMessage: 'Oprávnění' },
    permissionRead: { id: 'arr.issuesList.form.permission.read', defaultMessage: 'Čtení' },
    permissionWrite: { id: 'arr.issuesList.form.permission.write', defaultMessage: 'Zápis a změny' },
});
import ListBox from '../shared/listbox/ListBox';
import {formValueSelector, Field, FieldArray, reduxForm} from 'redux-form';
import {WebApi} from '../../actions';
import FormInputField from '../shared/form/FormInputField';

class IssueListForm extends AbstractReactComponent {
    static propTypes = {
        onCreate: PropTypes.func.isRequired,
        onSave: PropTypes.func.isRequired,
        id: PropTypes.number,
        fundId: PropTypes.number.isRequired,
    };

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (data[name] == null) {
                errors[name] = getIntl().formatMessage(globalMessages.validationRequired);
            }
            return errors;
        }, {});

    static validate = (values, props) => {
        return IssueListForm.requireFields('name', 'open')(values);
    };

    static fields = [
        'name',
        'open',
        'rdUsers[].id',
        'rdUsers[].username',
        'rdUsers[].description',
        'wrUsers[].id',
        'wrUsers[].username',
        'wrUsers[].description',
    ];

    static initialValues = {open: true, rdUsers: [], wrUsers: []};

    static FORM = 'issueList';

    componentDidUpdate(prevProps, prevState, prevContext) {
        if (prevProps.id && !this.props.id) {
            this.props.reset();
        } else if (
            this.props.id &&
            prevProps.id &&
            prevProps.id === this.props.id &&
            (this.props.rdUsers.length !== prevProps.rdUsers.length ||
                this.props.wrUsers.length !== prevProps.wrUsers.length)
        ) {
            this.props.asyncValidate();
        }
    }

    addUser = target => value => {
        if (!value) {
            return;
        }
        var curValues = target.getAll();
        if(curValues && curValues.filter(i => i.id === value.id).length > 0) {
            return;
        }
        target.push(value);
    };

    deleteUser = (target, index, e) => {
        target.remove(index);
    };

    renderUser = target => ({item, index}) => {
        return (
            <div key={item.username}>
                {item.username}{' '}
                <Button
                    variant="action"
                    bsSize="xs"
                    className="pull-right"
                    onClick={this.deleteUser.bind(this, target, index)}
                >
                    <Icon glyph="fa-trash" />
                </Button>
            </div>
        );
    };

    render() {
        const {id} = this.props;

        const customProps = {
            disabled: id == null,
        };

        return (
            <Form onSubmit={null}>
                <Field
                    component={FormInputField}
                    type="text"
                    name={'name'}
                    {...customProps}
                    label={this.props.intl.formatMessage(messages.name)}
                />
                <Field
                    component={FormInputField}
                    type="select"
                    name={'open'}
                    {...customProps}
                    label={this.props.intl.formatMessage(messages.open)}
                >
                    <option value={true}>{this.props.intl.formatMessage(messages.openTrue)}</option>
                    <option value={false}>{this.props.intl.formatMessage(messages.openFalse)}</option>
                </Field>
                <label>{this.props.intl.formatMessage(messages.permission)}</label>
                <Row>
                    <FieldArray
                        name={'rdUsers'}
                        component={({fields, meta}) => {
                            return (
                                <Col xs={6}>
                                    <label>{this.props.intl.formatMessage(messages.permissionRead)}</label>
                                    <UserField onChange={this.addUser(fields)} {...customProps} value={null} />
                                    <ListBox items={fields.getAll()} renderItemContent={this.renderUser(fields)} />
                                </Col>
                            );
                        }}
                    />
                    <FieldArray
                        name={'wrUsers'}
                        component={({fields, meta}) => {
                            return (
                                <Col xs={6}>
                                    <label>{this.props.intl.formatMessage(messages.permissionWrite)}</label>
                                    <UserField onChange={this.addUser(fields)} {...customProps} value={null} />
                                    <ListBox items={fields.getAll()} renderItemContent={this.renderUser(fields)} />
                                </Col>
                            );
                        }}
                    />
                </Row>
            </Form>
        );
    }
}

export const IssueListFormInitial = IssueListForm.initialValues;

const form = reduxForm({
    form: IssueListForm.FORM,
    initialValues: IssueListForm.initialValues,
    validate: IssueListForm.validate,
    asyncBlurFields: IssueListForm.fields,
    // Async validace zneužita k ukládání dat
    asyncValidate: (values, dispatch, props) => {
        const errors = IssueListForm.validate(values, props);
        if (Object.keys(errors).length > 0) {
            return Promise.resolve(errors);
        }
        const {id} = props;
        if (id) {
            return WebApi.updateIssueList(id, {...values, fundId: props.fundId, id}).then(data => {
                props.onSave(data);
                return {}; // No errors saved correctly
            });
        } else {
            return WebApi.addIssueList({...values, fundId: props.fundId}).then(data => {
                props.onCreate(data);
                return {}; // No errors saved correctly
            });
        }
    },
})(IssueListForm);

const selector = formValueSelector(IssueListForm.FORM);

export default connect((state, props) => {
    return {
        rdUsers: selector(state, 'rdUsers'),
        wrUsers: selector(state, 'wrUsers'),
    };
})(injectIntl(form));
