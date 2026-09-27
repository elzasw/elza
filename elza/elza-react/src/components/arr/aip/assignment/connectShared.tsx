import { Text } from "@fluentui/react-components";
import { useEffect, useState } from "react";
import { defineMessages } from "react-intl";
import { AipConnectBlockedVO } from "elza-api";
import { Api } from "../../../../api";

/** Texty společné připojení jednoho balíčku (karta průzkumníku) i hromadnému (stránka). */
export const connectMessages = defineMessages({
    target: { id: "arr.aip.assignment.target", defaultMessage: "Cíl - archivní soubor" },
    noTarget: { id: "arr.aip.single.noTarget", defaultMessage: "Vyberte ve stromu jednotku popisu, ke které se připojí." },
    what: { id: "arr.aip.assignment.what", defaultMessage: "Co připojit" },
    modeSublevels: { id: "arr.aip.assignment.mode.sublevels", defaultMessage: "Úrovně pod vybranou úrovní" },
    modeSublevelsHint: {
        id: "arr.aip.assignment.mode.sublevels.hint",
        defaultMessage: "Pro každou úroveň přímo pod vybranou úrovní vytvoří pod vybranou jednotkou popisu podúroveň a připojí k ní její část balíčků.",
    },
    modeStructure: { id: "arr.aip.assignment.mode.structure", defaultMessage: "Převzít strukturu a popis" },
    modeStructureHint: {
        id: "arr.aip.assignment.mode.structure.hint",
        defaultMessage: "Vytvoří úrovně pod vybranou úrovní až po dokumenty, s prvky popisu z balíčků, a připojí k nim soubory.",
    },
    fileplanAsRoot: { id: "arr.aip.assignment.fileplanAsRoot", defaultMessage: "Spisový plán jako kořenová série" },
    connect: { id: "arr.aip.assignment.connect", defaultMessage: "Připojit" },
    chooseMode: { id: "arr.aip.assignment.chooseMode", defaultMessage: "Zvolte, co připojit." },
    readOnly: {
        id: "arr.aip.assignment.readOnly",
        defaultMessage: "Archivní soubor je jen pro čtení (režim čtení nebo uzavřená verze) - připojit nelze.",
    },
});

/** Název způsobu připojení a jednou větou, co udělá (nebo proč teď nejde). */
export function ModeLabel({ text, hint }: { text: string; hint: string }) {
    return (
        <span className="aip-connect-mode-label">
            <span>{text}</span>
            <Text size={200} className="hint">{hint}</Text>
        </span>
    );
}

/**
 * Co brání připojit balíčky k cíli - připojení celého balíčku nebo jeho úrovně server odmítne,
 * je-li už napojený. Zjišťuje se předem, aby to uživatel věděl dřív, než potvrdí; znovu se
 * zjišťuje s každou změnou {@code refresh} (po akci se stav napojení mění).
 */
export function useConnectCheck(targetId: number | undefined, aipIds: number[], refresh: unknown) {
    const [blocked, setBlocked] = useState<AipConnectBlockedVO[]>([]);
    const key = aipIds.join(",");

    useEffect(() => {
        if (targetId == null || aipIds.length === 0) {
            setBlocked([]);
            return;
        }
        let current = true;
        Api.aips.aipConnectCheck(targetId, aipIds)
            .then(response => current && setBlocked(response.data.blocked ?? []))
            .catch(() => current && setBlocked([]));
        return () => { current = false; };
    }, [targetId, key, refresh]);

    return blocked;
}
