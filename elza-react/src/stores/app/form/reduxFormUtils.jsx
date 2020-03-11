import {valuesEquals} from 'components/Utils.jsx';

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
const mergeStateForField = (field, localState, serverState) => {
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

        const localArray = localState && localState[key] || [];

        if (!serverState[key]) {
            serverState[key] = [];
        }
        const serverArray = serverState[key];

        // Necháme z lokálního pole jen ty, které nemají id
        if (key !== "creators") { /// Hotfix - TODO @stanekpa - ignorace merge
            localArray.forEach(item => {
                if (!item._merged) {
                    if (item.id.value) {    // existující položka v local, zatím ignorujeme, v budoucnu můžeme její aktuální změny promítnout do server
                    } else {    // nová položka v local
                        if (!item.sendToServer) {
                            item._merged = true;
                            serverArray.push(item);
                        }
                    }
                }
            })
        }

        // const array = state && state[key] || [];
        // if (rest) {
        //     if (!dest[key]) {
        //         dest[key] = [];
        //     }
        //     array.forEach((item, index) => {
        //         if (!dest[key][index]) {
        //             dest[key][index] = {};
        //         }
        //         getValue(rest, item, dest[key][index], exportInitialValues);
        //     });
        // } else {
        //     dest[key] = array.map(item => exportInitialValues ? item.initial : item.value);
        // }
    } else if (dotIndex > 0) {
        // subobject field
        // const key = field.substring(0, dotIndex);
        // const rest = field.substring(dotIndex + 1);
        // if (!dest[key]) {
        //     dest[key] = {};
        // }
        // getValue(rest, state && state[key] || {}, dest[key], exportInitialValues);
    } else {
        if (!localState[field]) {
            console.log(localState, field)
        }
        if (localState[field] && localState[field].touched) {    // hodnota byla lokálně upravena, pokud initial je stejná jako server initial, vezmeme naší lokální aktuálně upravovanou
            if (valuesEquals(localState[field].initial, serverState[field].initial)) {
                serverState[field].value = localState[field].value;
            }
        }
        // dest[field] = state[field] && (exportInitialValues ? state[field].initial : state[field].value);
    }
}

const setAttribute = (field, state, dest, commonAttrs, fieldAttrs) => {
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
                    dest[key][index] = {...commonAttrs};
                }
                setAttribute(rest, item, dest[key][index], commonAttrs, fieldAttrs);
            });
        } else {
            dest[key] = array.map(item => {
                return {
                    ...item,
                    ...fieldAttrs,
                }
            });
        }
    } else if (dotIndex > 0) {
        // subobject field
        const key = field.substring(0, dotIndex);
        const rest = field.substring(dotIndex + 1);
        if (!dest[key]) {
            dest[key] = {...commonAttrs};
        }
        setAttribute(rest, state && state[key] || {...commonAttrs}, dest[key], commonAttrs, fieldAttrs);
    } else {
        dest[field] = state[field] && ({
                ...state[field],
                ...fieldAttrs
            });
    }
}

const getValues = (fields, state, exportInitialValues = false) =>
    fields.reduce((accumulator, field) => {
        getValue(field, state, accumulator, exportInitialValues);
        return accumulator;
    }, {})
const setAttributes = (fields, state, commonAttrs, fieldAttrs) =>
    fields.reduce((accumulator, field) => {
        setAttribute(field, state, accumulator, commonAttrs, fieldAttrs);
        return accumulator;
    }, {...commonAttrs})
const mergeState = (fields, localState, serverState) => {
    fields.forEach(field => {
        mergeStateForField(field, localState, serverState);
    })
}
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

export default {
    getValues,
    setAttributes,
    mergeState,
    read,
}
