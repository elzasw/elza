import {} from "../components";
import { getIntl } from 'components/shared/lang/intlInstance';
import { defineMessages } from 'react-intl';

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    all: { id: 'global.all', defaultMessage: 'Vše' },
});

export type Extend = {
    addEmpty: boolean;
    emptyName: string;
    emptyValue: any;
}

export function buildEnumItems(values: any[], getName: (value: any) => string, extend?: Extend) {
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

export function createItems(
    getItems: () => any[],
    getName: (value: any) => string,
    addEmpty = false,
    emptyName = getIntl().formatMessage(messages.all),
    emptyValue = -1,
) {
    return buildEnumItems(getItems(), getName, {
        addEmpty,
        emptyName,
        emptyValue
    })
}
