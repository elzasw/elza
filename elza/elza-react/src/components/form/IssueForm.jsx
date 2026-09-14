import PropTypes from 'prop-types';
import * as React from 'react';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {AbstractReactComponent} from 'components/shared';
import {Field, reduxForm} from 'redux-form';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    type: { id: 'issue.type', defaultMessage: 'Typ připomínky' },
    text: { id: 'issue.text', defaultMessage: 'Text připomínky' },
});
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
                errors[name] = getIntl().formatMessage(globalMessages.validationRequired);
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
                        label={this.props.intl.formatMessage(messages.type)}>
                        {issueTypes.fetched && issueTypes.data.map(basicOptionMap)}
                    </Field>
                    <Field
                        name="description"
                        type="textarea"
                        component={FormInputField}
                        label={this.props.intl.formatMessage(messages.text)}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary"><FormattedMessage {...(update ? globalMessages.save : globalMessages.add)} /></Button>
                    <Button variant="link" onClick={onClose}>
                        <FormattedMessage {...globalMessages.cancel} />
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
})(injectIntl(IssueForm));

export default connect(mapStateToProps)(form);
