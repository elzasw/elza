import PropTypes from 'prop-types';
import * as React from 'react';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {AbstractReactComponent} from 'components/shared';
import {reduxForm} from 'redux-form';
import FormInput from '../shared/form/FormInput';
import i18n from '../i18n';
import * as issueTypesActions from '../../actions/refTables/issueTypes';

const basicOptionMap = (i) => <option key={i.id} value={i.id}>{i.name}</option>;

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
        const {handleSubmit, onClose, issueTypes, fields: {description, issueTypeId}, update} = this.props;
        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <FormInput as="select" label={i18n('issue.type')} {...issueTypeId}>
                        {issueTypes.fetched && issueTypes.data.map(basicOptionMap)}
                    </FormInput>
                    <FormInput as="textarea" label={i18n('issue.text')} {...description}/>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit">{i18n(update ? 'global.action.update' : 'global.action.add')}</Button>
                    <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default reduxForm({
    fields: [
        'issueTypeId',
        'description',
    ],
    validate: (values, props) => {
        return IssueForm.requireFields('issueTypeId', 'description')(values);
    },
    form: 'issueForm',
}, (state) => {
    return {
        issueTypes: state.refTables.issueTypes,
    };
})(IssueForm);


