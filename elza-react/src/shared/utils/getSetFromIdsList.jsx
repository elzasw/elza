export default function getSetFromIdsList(list) {
    const map = {};
    list &&
        list.forEach(x => {
            map[x] = true;
        });
    return map;
}
