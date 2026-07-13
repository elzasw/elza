import { useContext } from "react";
import { FluentDialogContext } from "components/shared/dialog/FluentModalDialog";
import { AiAssistantPanel } from "./AiAssistantPanel";

export function useAiAssistantModal() {
    const { showModal } = useContext(FluentDialogContext);

    return function showAiAssistant(externalSystemCode: string) {
        return showModal<undefined, undefined>({
            isSingleInstance: true,
            name: "ai-assistant-modal",
            createDialog: ({ handleResult }) => (
                <AiAssistantPanel
                    onClose={() => handleResult(undefined, undefined)}
                    externalSystemCode={externalSystemCode}
                />
            ),
        });
    };
}
