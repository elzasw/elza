import { useEffect, useState } from "react";
import { Api } from "api";
import { AiProfile, AiTaskType } from "elza-api";

export function useAiProviderInfo(externalSystemCode: string) {
    const [taskTypes, setTaskTypes] = useState<AiTaskType[]>([]);
    const [profiles, setProfiles] = useState<AiProfile[]>([]);

    useEffect(() => {
        if (!externalSystemCode) return;
        let cancelled = false;
        (async () => {
            try {
                const { data } = await Api.aiprovider.aiProviderGetInfo(externalSystemCode);
                if (cancelled) return;
                setTaskTypes(data.taskTypes ?? []);
                setProfiles(data.profiles ?? []);
            } catch {
                if (cancelled) return;
                setTaskTypes([]);
                setProfiles([]);
            }
        })();
        return () => { cancelled = true; };
    }, [externalSystemCode]);

    return { taskTypes, profiles };
}
