import * as PropTypes from 'prop-types';
import * as React from 'react';
import {Field, FormErrors, InjectedFormProps, reduxForm} from 'redux-form';
import {i18n} from 'components/shared';
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
                        as="textarea"
                        component={FormInputField}
                        label={i18n('arr.request.title.description')}
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
