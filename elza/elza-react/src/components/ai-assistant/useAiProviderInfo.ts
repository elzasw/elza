import { useEffect, useState } from "react";
import { Api } from "api";
import { AiTaskType } from "elza-api";

export function useAiProviderInfo(externalSystemCode: string) {
    const [taskTypes, setTaskTypes] = useState<AiTaskType[]>([]);

    useEffect(() => {
        if (!externalSystemCode) return;
        let cancelled = false;
        (async () => {
            try {
                const { data } = await Api.aiprovider.aiProviderGetInfo(externalSystemCode);
                if (!cancelled) setTaskTypes(data.taskTypes ?? []);
            } catch {
                if (!cancelled) setTaskTypes([]);
            }
        })();
        return () => { cancelled = true; };
    }, [externalSystemCode]);

    return { taskTypes };
}
