import { Button, Tooltip, Menu, MenuTrigger, MenuPopover, MenuList, MenuItem } from "@fluentui/react-components";
import { ChatSparkleRegular } from "@fluentui/react-icons";
import { useIntl } from "react-intl";
import { useAiAssistantModal } from "./useAiAssistantModal";
import { useAiExternalSystems } from "./useAiExternalSystem";
import { aiAssistantMessages } from "./messages";

export function AiAssistantRibbonButton() {
    const intl = useIntl();
    const systems = useAiExternalSystems();
    const showAiAssistant = useAiAssistantModal();
    const label = intl.formatMessage(aiAssistantMessages.windowTitle);

    if (systems.length === 0) return null;

    if (systems.length === 1) {
        return (
            <Tooltip content={label} relationship="label">
                <Button
                    appearance="subtle"
                    icon={<ChatSparkleRegular />}
                    onClick={() => showAiAssistant(systems[0].code)}
                    aria-label={label}
                />
            </Tooltip>
        );
    }

    return (
        <Menu>
            <MenuTrigger disableButtonEnhancement>
                <Tooltip content={label} relationship="label">
                    <Button appearance="subtle" icon={<ChatSparkleRegular />} aria-label={label} />
                </Tooltip>
            </MenuTrigger>
            <MenuPopover>
                <MenuList>
                    {systems.map(system => (
                        <MenuItem key={system.code} onClick={() => showAiAssistant(system.code)}>
                            {system.name}
                        </MenuItem>
                    ))}
                </MenuList>
            </MenuPopover>
        </Menu>
    );
}
