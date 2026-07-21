import { useEffect, useState } from "react";
import { Api } from "api";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useAppThunkDispatch } from "utils/hooks/useThunkDispatch";
import { refExternalSystemsFetchIfNeeded } from "actions/refTables/externalSystems";
import { usePermissions } from "contexts/user";

const AI_EXTERNAL_SYSTEM_CLASS = ".AiExternalSystemSimpleVO";
const APIKEY_ID = "apiKeyId";

export interface AiExternalSystem {
    code: string;
    name: string;
}

export function useAiExternalSystems(): AiExternalSystem[] {
    const dispatch = useAppThunkDispatch();
    const { isAdmin } = usePermissions();
    const externalSystems = useAppSelector(state => state.refTables.externalSystems.items ?? []);
    const userId = useAppSelector(state => state.userDetail.id);
    const [keyedSystemIds, setKeyedSystemIds] = useState<number[] | null>(null);

    useEffect(() => {
        dispatch(refExternalSystemsFetchIfNeeded());
    }, [dispatch]);

    const admin = isAdmin();

    useEffect(() => {
        // Admins may use the instance-wide key, so no per-user key lookup is needed.
        if (admin || userId == null) {
            setKeyedSystemIds(null);
            return;
        }
        let cancelled = false;
        (async () => {
            try {
                const { data } = await Api.externalSystems.externalSystemAllProperties(undefined, userId);
                const ids = data
                    .filter(property => property.userId === userId && property.name === APIKEY_ID && property.extSystemId != null)
                    .map(property => property.extSystemId as number);
                if (!cancelled) setKeyedSystemIds(ids);
            } catch {
                if (!cancelled) setKeyedSystemIds([]);
            }
        })();
        return () => { cancelled = true; };
    }, [admin, userId]);

    const aiSystems = externalSystems
        .filter(system => system["@class"] === AI_EXTERNAL_SYSTEM_CLASS && system.code != null)
        .map(system => ({ id: system.id, code: system.code as string, name: system.name || (system.code as string) }));

    // The button is shown whenever an AI external system exists. The per-user API-key
    // lookup above is retained (but disconnected) for a future gate that hides systems
    // the user has no personal key for; to reinstate it, filter aiSystems by keyedSystemIds
    // (admins bypass, as they use the instance-wide key).
    return aiSystems.map(({ code, name }) => ({ code, name }));
}
