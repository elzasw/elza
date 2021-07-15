import PropTypes from 'prop-types';
import * as React from 'react';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {AbstractReactComponent} from 'components/shared';
import {Field, reduxForm} from 'redux-form';
import i18n from '../i18n';
import * as issueTypesActions from '../../actions/refTables/issueTypes';
import * as issuesActions from '../../actions/arr/issues';
import {connect} from "react-redux";
import {FormInputField} from "../shared";
import storeFromArea from '../../shared/utils/storeFromArea';

const basicOptionMap = i => (
    <option key={i.id} value={i.id}>
        {i.name}
    </option>
);

class IssueForm extends AbstractReactComponent {
    componentDidMount() {
        this.props.dispatch(issueTypesActions.fetchIfNeeded());
        this.props.dispatch(issuesActions.protocols.fetchIfNeeded(this.props.activeFund.id));
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
            issueProtocol,
            issueProtocols,
            issueTypes,
            update,
        } = this.props;

        console.log(issueProtocols);
        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <Field
                        type="select"
                        name="issueListId"
                        component={FormInputField}
                        value={issueProtocol.id}
                        label="Protokol"
                    >
                        {issueProtocols.fetched && issueProtocols.count === 0 && <option value={''} />}
                        {issueProtocols.fetched && issueProtocols.rows.map(basicOptionMap)}
                    </Field>
                    <Field
                        name="issueTypeId"
                        type="select"
                        component={FormInputField}
                        value={issueTypes.data?.[0].id}
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
    const { arrRegion, refTables } = state;
    return {
        activeFund: arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null,
        issueTypes: refTables.issueTypes,
        issueProtocol: storeFromArea(state, issuesActions.AREA_PROTOCOL),
        issueProtocols: storeFromArea(state, issuesActions.AREA_PROTOCOLS),
    };
}

const form = reduxForm({
    form: 'issueForm',
    validate: (values, props) => {
        return IssueForm.requireFields('issueTypeId', 'description')(values);
    }
})(IssueForm);

export default connect(mapStateToProps)(form);
