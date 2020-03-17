export default function getMapFromList(list, attrName = 'id') {
    const map = {};
    list.forEach(x => {
        map[x[attrName]] = x;
    });
    return map;
}
