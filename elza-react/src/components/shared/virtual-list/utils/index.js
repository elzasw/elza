function areArraysEqual(a, b) {
    if (!a || !b) return false;

    if (a.length !== b.length) return false;

    for (let i = 0, length = a.length; i < length; i++) {
        if (a[i] !== b[i]) return false;
    }

    return true;
}

function topDifference(element, container) {
    return topFromWindow(element) - topFromWindow(container);
}

function topFromWindow(element) {
    if (!element || element === window) return 0;

    return element.offsetTop + topFromWindow(element.offsetParent);
}

function debounce(func, wait, immediate) {
    if (!wait) return func;

    let timeout;

    return function() {
        const context = this,
            args = arguments;

        const later = function() {
            timeout = null;

            if (!immediate) func.apply(context, args);
        };

        const callNow = immediate && !timeout;

        clearTimeout(timeout);
        timeout = setTimeout(later, wait);

        if (callNow) func.apply(context, args);
    };
}

export default {
    areArraysEqual: areArraysEqual,
    topDifference: topDifference,
    topFromWindow: topFromWindow,
    debounce: debounce,
};
