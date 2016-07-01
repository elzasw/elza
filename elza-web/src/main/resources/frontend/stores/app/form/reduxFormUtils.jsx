const getValue = (field, state, dest, exportInitialValues) => {
    const dotIndex = field.indexOf('.');
    const openIndex = field.indexOf('[');
    const closeIndex = field.indexOf(']');
    if (openIndex > 0 && closeIndex !== openIndex + 1) {
        throw new Error('found [ not followed by ]');
    }
    if (openIndex > 0 && (dotIndex < 0 || openIndex < dotIndex)) {
        // array field
        const key = field.substring(0, openIndex);
        let rest = field.substring(closeIndex + 1);
        if (rest[0] === '.') {
            rest = rest.substring(1);
        }
        const array = state && state[key] || [];
        if (rest) {
            if (!dest[key]) {
                dest[key] = [];
            }
            array.forEach((item, index) => {
                if (!dest[key][index]) {
                    dest[key][index] = {};
                }
                getValue(rest, item, dest[key][index], exportInitialValues);
            });
        } else {
            dest[key] = array.map(item => exportInitialValues ? item.initial : item.value);
        }
    } else if (dotIndex > 0) {
        // subobject field
        const key = field.substring(0, dotIndex);
        const rest = field.substring(dotIndex + 1);
        if (!dest[key]) {
            dest[key] = {};
        }
        getValue(rest, state && state[key] || {}, dest[key], exportInitialValues);
    } else {
        dest[field] = state[field] && (exportInitialValues ? state[field].initial : state[field].value);
    }
}

const getValues = (fields, state, exportInitialValues = false) =>
    fields.reduce((accumulator, field) => {
        getValue(field, state, accumulator, exportInitialValues);
        return accumulator;
    }, {})

// ##########################################################################

/**
 * Reads any potentially deep value from an object using dot and array syntax
 */
const read = (path, object) => {
    if (!path || !object) {
        return object;
    }
    const dotIndex = path.indexOf('.');
    if (dotIndex === 0) {
        return read(path.substring(1), object);
    }
    const openIndex = path.indexOf('[');
    const closeIndex = path.indexOf(']');
    if (dotIndex >= 0 && (openIndex < 0 || dotIndex < openIndex)) {
        // iterate down object tree
        return read(path.substring(dotIndex + 1), object[path.substring(0, dotIndex)]);
    }
    if (openIndex >= 0 && (dotIndex < 0 || openIndex < dotIndex)) {
        if (closeIndex < 0) {
            throw new Error('found [ but no ]');
        }
        const key = path.substring(0, openIndex);
        const index = path.substring(openIndex + 1, closeIndex);
        if (!index.length) {
            return object[key];
        }
        if (openIndex === 0) {
            return read(path.substring(closeIndex + 1), object[index]);
        }
        if (!object[key]) {
            return undefined;
        }
        return read(path.substring(closeIndex + 1), object[key][index]);
    }
    return object[path];
};

// ##########################################################################

module.exports = {
    getValues,
    read,
}