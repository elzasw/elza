import { useEffect } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useAppThunkDispatch } from "utils/hooks/useThunkDispatch";
import { extSystemListFetchIfNeeded } from "actions/admin/extSystem";

const AI_EXTERNAL_SYSTEM_CLASS = ".AiExternalSystemVO";

export interface AiExternalSystem {
    code: string;
    name: string;
}

export function useAiExternalSystems(): AiExternalSystem[] {
    const dispatch = useAppThunkDispatch();
    const extSystemList = useAppSelector(state => state.app.extSystemList);

    useEffect(() => {
        dispatch(extSystemListFetchIfNeeded());
    }, [dispatch]);

    return (extSystemList.rows ?? [])
        .filter(system => system["@class"] === AI_EXTERNAL_SYSTEM_CLASS && system.code != null)
        .map(system => ({ code: system.code as string, name: system.name || (system.code as string) }));
}
