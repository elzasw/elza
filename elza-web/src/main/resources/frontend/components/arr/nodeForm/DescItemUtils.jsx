var classNames = require('classnames');

/**
 * Tool třída pro desc item.
 */
export function decorateValue(component, descItem) {
    var cls = classNames({
        'form-control': true,
        value: true,
        error: descItem.error.value,
        active: descItem.hasFocus,
    });

    return {
        className: cls,
        title: descItem.error.value,
        onFocus: () => component.props.onFocus(),
        onBlur: () => component.props.onBlur()
    }
}
