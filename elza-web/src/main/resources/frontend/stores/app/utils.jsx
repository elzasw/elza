function indexById(arr, id) {
    if (arr == null) {
        return null;
    }

    for (var a=0; a<arr.length; a++) {
        if (arr[a].id === id) {
            return a;
        }
    }
    return null;
}
exports.indexById = indexById

function selectedAfterClose(arr, index) {
    if (index >= arr.length - 1) {
        if (index - 1 >= 0) {
            return index - 1;
        } else {
            return null;
        }
    } else {
        return index;
    }
}
exports.selectedAfterClose = selectedAfterClose
