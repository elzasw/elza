import { getIntl } from 'components/shared/lang/intlInstance';
import { globalMessages } from 'components/shared/lang/messages';
import {FormErrors} from 'redux-form';

const requireFields = <T>(...names) => <T>(data: T): FormErrors<T, FormErrors<T>> =>
    names.reduce((errors, name) => {
        if (data[name] == null) {
            errors[name] = getIntl().formatMessage(globalMessages.validationRequired);
        }
        return errors;
    }, {});

export default requireFields;
