var classNames = require('classnames');

/**
 * Tool třída pro desc item.
 */
export function decorateValue(component, active, error, additionalClassNames = []) {
    var clsObj = {
        'form-control': true,
        value: true,
        error: error,
        active: active,
    };
    additionalClassNames.forEach(cls => {clsObj[cls] = true});

    var cls = classNames(clsObj);

    return {
        className: cls,
        title: error,
        onFocus: () => component.props.onFocus(),
        onBlur: () => component.props.onBlur()
    }
}
