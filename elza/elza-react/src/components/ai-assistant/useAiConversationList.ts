import { useCallback, useEffect, useState } from "react";
import { Api } from "api";
import { AiConversation } from "elza-api";

export function useAiConversationList(refreshToken: unknown) {
    const [conversations, setConversations] = useState<AiConversation[]>([]);

    const reload = useCallback(async () => {
        try {
            const { data } = await Api.aiprovider.aiProviderListConversations();
            setConversations(data);
        } catch {
            setConversations([]);
        }
    }, []);

    useEffect(() => {
        reload();
    }, [reload, refreshToken]);

    return { conversations, reload };
}
