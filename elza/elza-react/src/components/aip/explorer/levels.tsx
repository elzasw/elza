import { ReactElement, useCallback } from 'react';
import { useIntl } from 'react-intl';
import {
    Box16Regular,
    DocumentBulletList16Regular,
    DocumentMultiple16Regular,
    TextBulletListTree16Regular,
} from '@fluentui/react-icons';
import { AipLevelType } from 'elza-api';

import { levelMessages } from '../messages';
import { getFileName } from './utils';

/**
 * Uzel stromu, jak přichází ze serveru. Virtuální úrovně nesou `levelType`, reálné složky
 * a soubory ne. Strom průzkumníka pojmenovává uzly `label`, strom logických kontejnerů `name`.
 */
export interface NamedNode {
    levelType?: AipLevelType;
    label?: string;
    name?: string;
    filename?: string;
}

/**
 * Ikony úrovní stromu průzkumníka. Úroveň "bez logické struktury" existuje jen ve stromu
 * logických kontejnerů, který ikony nekreslí, a nic výstižného by pro ni ani nebylo.
 */
const levelIcons: Partial<Record<AipLevelType, ReactElement>> = {
    [AipLevelType.Package]: <Box16Regular />,
    [AipLevelType.Representations]: <DocumentMultiple16Regular />,
    [AipLevelType.LogicalStructure]: <TextBulletListTree16Regular />,
    [AipLevelType.Metadata]: <DocumentBulletList16Regular />,
};

/** Ikona virtuální úrovně; reálné uzly i neznámý typ zůstávají bez ikony. */
export const levelIcon = (levelType?: AipLevelType): ReactElement | undefined =>
    levelType ? levelIcons[levelType] : undefined;

/**
 * Název uzlu. Virtuální úroveň se překládá podle typu, ostatní uzly si název nesou samy.
 * Popisek ze serveru je záložní i u virtuální úrovně - typ, který klient nezná, tak nezůstane
 * bez jména. Vrácená funkce mění identitu jen se změnou jazyka, takže na ní lze stavět memoizaci.
 */
export function useNodeName() {
    const { formatMessage } = useIntl();

    return useCallback((node?: NamedNode | null): string => {
        if (!node) {
            return '';
        }
        const message = node.levelType && levelMessages[node.levelType];
        if (message) {
            return formatMessage(message);
        }
        return node.label || node.name || (node.filename ? getFileName(node.filename) : '');
    }, [formatMessage]);
}
