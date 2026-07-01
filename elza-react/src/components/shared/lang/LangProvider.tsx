import { serverContextPath } from "api";
import { useUserSettings } from "contexts/user";
import { PropsWithChildren, useEffect, useState } from "react";
import { IntlProvider } from "react-intl";

export function LangProvider({ children }: PropsWithChildren) {
  const { settings } = useUserSettings();
  // The language selector is an experimental feature; without it enabled there is no way to switch
  // back, so a non-default language only applies while experimental features are on.
  const locale = settings.showExperimentalFeatures ? settings.language ?? "cs" : "cs";
  const [messages, setMessages] = useState<Record<string, string>>({});

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const response = await fetch(`${serverContextPath}/static/res/locale/${locale}.json`);
      const loadedMessages: Record<string, string> = await response.json();
      if (!cancelled) {
        setMessages(loadedMessages);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [locale]);

  return (
    <IntlProvider messages={messages} defaultLocale="cs" locale={locale}>
      {children}
    </IntlProvider>
  );
}
