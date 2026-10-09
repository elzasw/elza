import {
    Button,
    Menu,
    MenuItemRadio,
    MenuList,
    MenuPopover,
    MenuTrigger,
    Tooltip,
} from '@fluentui/react-components';
import { LocalLanguageRegular } from '@fluentui/react-icons';
import { defineMessages, useIntl } from 'react-intl';
import { browserLanguage, nativeLanguageName } from './language';
import { useLanguage } from './useLanguage';

const messages = defineMessages({
    language: {
        id: 'lang.picker.language',
        defaultMessage: 'Jazyk',
    },
});

/**
 * Small language switch for places without the user settings (the login dialog).
 *
 * Normally only an icon: most users never need it. It shows a language by name - one click away -
 * when the user has not chosen a language yet and the browser prefers another offered language
 * than the one shown, which is when someone is likely to look for it. The name is in that language
 * itself, so a reader who does not understand the current UI recognizes it.
 */
export function LanguagePicker() {
    const intl = useIntl();
    const { current, chosen, offered, switchLanguage } = useLanguage();

    if (offered.length < 2) {
        return null;
    }

    const preferred = chosen ? undefined : browserLanguage(offered);
    const suggestion = preferred && preferred !== current ? preferred : undefined;
    const label = intl.formatMessage(messages.language);

    const menu =
        suggestion && offered.length === 2 ? null : (
            <Menu
                checkedValues={{ language: [current] }}
                onCheckedValueChange={(_event, data) => {
                    const language = data.checkedItems[0];
                    if (language) {
                        switchLanguage(language);
                    }
                }}
            >
                <MenuTrigger disableButtonEnhancement>
                    <Tooltip content={label} relationship="label">
                        <Button type="button" appearance="subtle" icon={<LocalLanguageRegular />} />
                    </Tooltip>
                </MenuTrigger>
                <MenuPopover>
                    <MenuList>
                        {offered.map((tag) => (
                            <MenuItemRadio key={tag} name="language" value={tag} lang={tag}>
                                {nativeLanguageName(tag)}
                            </MenuItemRadio>
                        ))}
                    </MenuList>
                </MenuPopover>
            </Menu>
        );

    return (
        <>
            {suggestion && (
                <Button
                    type="button"
                    appearance="subtle"
                    icon={<LocalLanguageRegular />}
                    lang={suggestion}
                    onClick={() => switchLanguage(suggestion)}
                >
                    {nativeLanguageName(suggestion)}
                </Button>
            )}
            {menu}
        </>
    );
}
