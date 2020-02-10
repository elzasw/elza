export default function filterObjectsByPropertyFulltext(list, value, attrName = 'name', everywhere=true) {
    if (value === null || value.length === 0) {
        return list;
    }

    const inputValue = value.trim().toLowerCase();
    const inputLength = inputValue.length;

    if (everywhere) {   // kdekoli
        return inputLength === 0 ? [] : list.filter(item =>
            item[attrName].toLowerCase().indexOf(inputValue) >= 0
        );
    } else {    // jen od začátku
        return inputLength === 0 ? [] : list.filter(item =>
            item[attrName].toLowerCase().slice(0, inputLength) === inputValue
        );
    }
}
