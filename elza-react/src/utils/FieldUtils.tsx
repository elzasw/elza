import {i18n} from "../components";

export type Extend = {
    addEmpty: boolean;
    emptyName: string;
    emptyValue: any;
}

export function buildEnumItems(values: any[], getName, extend?: Extend) {
    const result: any[] = [];
    values && values.forEach(value => result.push({
        id: value,
        name: getName(value)
    }));
    if (extend && extend.addEmpty) {
        // vložení na první místo
        result.unshift({
            id: extend.emptyValue,
            name: extend.emptyName
        });
    }
    return result;
}

export function createItems(getItems, getName, addEmpty = false, emptyName = i18n('global.all'), emptyValue = -1) {
    return buildEnumItems(getItems(), getName, {
        addEmpty,
        emptyName,
        emptyValue
    })
}
