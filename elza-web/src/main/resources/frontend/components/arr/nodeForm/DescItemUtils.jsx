var classNames = require('classnames');

/**
 * Tool třída pro desc item.
 */
export function _decorateValue(component, active, error, locked, additionalClassNames = []) {
    var clsObj = {
        value: true,
        error: error,
        active: active,
    };
    additionalClassNames.forEach(cls => {clsObj[cls] = true});

    var cls = classNames(clsObj);

    return {
        className: cls,
        title: error,
        disabled: locked,
        onFocus: () => component.props.onFocus(),
        onBlur: () => component.props.onBlur()
    }
}
/**
 * Tool třída pro desc item.
 */
export function decorateValue(component, active, error, locked, additionalClassNames = []) {
    return _decorateValue(component, active, error, locked, [...additionalClassNames, 'form-control'])
}
// nechceme form-control
export function decorateAutocompleteValue(component, active, error, locked, additionalClassNames = []) {
    return _decorateValue(component, active, error, locked, [...additionalClassNames])
}
