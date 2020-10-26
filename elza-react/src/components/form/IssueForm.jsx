import PropTypes from 'prop-types';
import * as React from 'react';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {AbstractReactComponent} from 'components/shared';
import {Field, reduxForm} from 'redux-form';
import i18n from '../i18n';
import * as issueTypesActions from '../../actions/refTables/issueTypes';
import {connect} from "react-redux";
import {FormInputField} from "../shared";

const basicOptionMap = i => (
    <option key={i.id} value={i.id}>
        {i.name}
    </option>
);

class IssueForm extends AbstractReactComponent {
    componentDidMount() {
        this.props.dispatch(issueTypesActions.fetchIfNeeded());
    }

    static propTypes = {
        update: PropTypes.bool,
    };

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (!data[name]) {
                errors[name] = i18n('global.validation.required');
            }
            return errors;
        }, {});

    render() {
        const {
            handleSubmit,
            onClose,
            issueTypes,
            update,
        } = this.props;
        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <Field
                        name="issueTypeId"
                        type="select"
                        component={FormInputField}
                        label={i18n('issue.type')}>
                        {issueTypes.fetched && issueTypes.data.map(basicOptionMap)}
                    </Field>
                    <Field
                        name="description"
                        as="textarea"
                        component={FormInputField}
                        label={i18n('issue.text')}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary">{i18n(update ? 'global.action.update' : 'global.action.add')}</Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

function mapStateToProps(state) {
    const {refTables} = state;
    return {
        issueTypes: refTables.issueTypes,
    };
}

const form = reduxForm({
    form: 'issueForm',
    validate: (values, props) => {
        return IssueForm.requireFields('issueTypeId', 'description')(values);
    }
})(IssueForm);

export default connect(mapStateToProps)(form);
