import * as PropTypes from 'prop-types';
import * as React from 'react';
import {Field, FormErrors, InjectedFormProps, reduxForm} from 'redux-form';
import {} from 'components/shared';
import { FormattedMessage } from 'react-intl';
import { daoMessages } from 'components/arr/daoMessages';
import FormInputField from '../shared/form/FormInputField';

type OwnProps = {disabled: boolean};
type FormData = {description: string};
type Props = OwnProps & InjectedFormProps<FormData, OwnProps, FormErrors<FormData>>;

/**
 * Formulář inline editace požadavku na externí systém.
 */
class RequestInlineForm extends React.Component<Props> {
    static propTypes = {
        disabled: PropTypes.bool,
    };

    render() {
        const {disabled} = this.props;

        return (
            <div className="edit-request-form-container">
                <form>
                    <Field
                        name="description"
                        type="textarea"
                        component={FormInputField}
                        label={<FormattedMessage {...daoMessages.requestTitleDescription} />}
                        disabled={disabled}
                    />
                </form>
            </div>
        );
    }
}

export default reduxForm<FormData, OwnProps, FormErrors<FormData>>({
    form: 'requestEditForm',
    asyncBlurFields: ['description'],
})(RequestInlineForm as any);
