export function getMapFromList<T>(list: T[], attrName: any = 'id'): Map<any, T> {
    const map = new Map<any, T>();

    if (!list)
        return map;

    list.forEach(x => {
        map.set(x[attrName], x);
    });
    return map;
}

export function getSetFromIdsList<T>(list: T[]): Set<T> {
    const map = new Set<T>();
    list &&
        list.forEach(x => {
            !map.has(x) && map.add(x);
        });
    return map;
}

export function getIdsList<T>(objectList: T[], attrName: any = 'id'): any[] {
    return objectList.map(obj => obj[attrName]);
}

export function indexById<T>(arr: T[], id: any, attrName: any = 'id'): number | null {
    if (arr == null) {
        return null;
    }

    for (let a = 0; a < arr.length; a++) {
        if (arr[a][attrName] == id) {
            return a;
        }
    }
    return null;
}

export function objectById<T>(arr: T[], id: any, attrName: any = 'id'): T | null {
    if (arr == null) {
        return null;
    }

    for (let a = 0; a < arr.length; a++) {
        if (arr[a][attrName] == id) {
            return arr[a];
        }
    }
    return null;
}
