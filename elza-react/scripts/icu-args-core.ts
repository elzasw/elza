import { parse, TYPE, type MessageFormatElement } from "@formatjs/icu-messageformat-parser";

/**
 * Jména argumentů, která zpráva používá.
 *
 * Prochází se i větve `plural`/`select` a obsah tagů - argument uvnitř větve je
 * pořád argument, jen ho nejde vidět na první pohled. Bez toho by kontrola
 * hlásila falešný nesoulad všude, kde jeden jazyk používá `{count}` přímo
 * a druhý `{count, plural, ...}`.
 */
export function messageArguments(message: string): Set<string> {
    const found = new Set<string>();

    const walk = (elements: MessageFormatElement[]): void => {
        for (const element of elements) {
            switch (element.type) {
                case TYPE.argument:
                case TYPE.number:
                case TYPE.date:
                case TYPE.time:
                    found.add(element.value);
                    break;
                case TYPE.select:
                case TYPE.plural:
                    found.add(element.value);
                    for (const option of Object.values(element.options)) {
                        walk(option.value);
                    }
                    break;
                case TYPE.tag:
                    walk(element.children);
                    break;
                default:
                    break;
            }
        }
    };

    walk(parse(message));
    return found;
}

export interface ArgumentMismatch {
    id: string;
    /** Argumenty, které zdroj používá a překlad ne - hodnota by se ztratila. */
    missing: string[];
    /** Argumenty navíc v překladu - při formátování se nedosadí nic. */
    unexpected: string[];
}

/**
 * Porovná jména argumentů zdrojového a přeloženého katalogu.
 *
 * Chybějící argument v překladu je tichá ztráta dat: text se vypíše, jen v něm
 * chybí hodnota. Argument navíc se nedosadí a zůstane viditelný v textu.
 */
export function compareArguments(
    source: Record<string, string>,
    target: Record<string, string>,
): ArgumentMismatch[] {
    const mismatches: ArgumentMismatch[] = [];

    for (const [id, sourceMessage] of Object.entries(source)) {
        const targetMessage = target[id];
        if (targetMessage === undefined) {
            continue;
        }
        const sourceArgs = messageArguments(sourceMessage);
        const targetArgs = messageArguments(targetMessage);

        const missing = [...sourceArgs].filter((name) => !targetArgs.has(name));
        const unexpected = [...targetArgs].filter((name) => !sourceArgs.has(name));

        if (missing.length > 0 || unexpected.length > 0) {
            mismatches.push({ id, missing, unexpected });
        }
    }

    return mismatches;
}
