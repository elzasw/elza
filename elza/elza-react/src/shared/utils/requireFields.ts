import i18n from '../../components/i18n';
import {FormErrors} from 'redux-form';

const requireFields = <T>(...names) => <T>(data: T): FormErrors<T, FormErrors<T>> =>
    names.reduce((errors, name) => {
        if (data[name] == null) {
            errors[name] = i18n('global.validation.required');
        }
        return errors;
    }, {});

export default requireFields;
