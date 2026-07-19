import { useCallback, useEffect, useState } from "react";
import { Api } from "api";
import { AiUsageBalance } from "elza-api";

/**
 * The user's usage/credit balance at the AI provider (their personal account
 * when a personal key is stored, else the shared organizational one). Null
 * until loaded or when the provider is unreachable / has no credit accounting
 * data; the panel simply hides the balance then. Call refresh() after a
 * finished exchange or a quota refusal — the provider serves the balance from
 * the same state its budget gate uses, so the display never disagrees with a
 * refusal.
 */
export function useAiUsageBalance(externalSystemCode: string) {
    const [balance, setBalance] = useState<AiUsageBalance | null>(null);

    const refresh = useCallback(async () => {
        if (!externalSystemCode) return;
        try {
            const { data } = await Api.aiprovider.aiProviderGetUsage(externalSystemCode);
            setBalance(data);
        } catch {
            setBalance(null);
        }
    }, [externalSystemCode]);

    useEffect(() => {
        refresh();
    }, [refresh]);

    return { balance, refresh };
}
