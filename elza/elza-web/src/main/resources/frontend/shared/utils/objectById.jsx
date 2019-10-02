export default function objectById(arr, id, attrName = 'id') {
    if (arr == null) {
        return null;
    }
    const len = arr.length;
    for (let a = 0; a < len; a++) {
        if (arr[a][attrName] == id) {
            return arr[a];
        }
    }
    return null;
}
