import { Button } from "@fluentui/react-components";
import { serverContextPath } from "api";
import { PropsWithChildren, useEffect, useState } from "react";
import { IntlProvider } from "react-intl";
// import { messages } from "./lang.cs.ts";

interface Props {
  test?: string;
}

const locales = ["cs", "en"]

export function LangProvider({ children }: PropsWithChildren<Props>) {
  const [messages, setMessages] = useState<Record<string, string>>({});
  const [locale, setLocale] = useState<string>("cs");

  useEffect(() => {
    (async () => {
      const response = await fetch(`${serverContextPath}/static/locale/${locale}.json`);
      const _messages:Record<string,string> = await response.json();

      console.log("#### _messages", _messages);
      setMessages(_messages);
    })()
  }, [locale])

  function handleLocaleChange(){
    const selectedLocaleIndex = locales.indexOf(locale);
    if(selectedLocaleIndex + 1 < locales.length){
      setLocale(locales[selectedLocaleIndex + 1]);
    } else {
      setLocale(locales[0]);
    }
  }

  console.log("#### messages", messages, children);

  return (
    <IntlProvider messages={messages} defaultLocale="cs" locale={locale}>
      <Button onClick={handleLocaleChange}>{locale}</Button>
      {children}
    </IntlProvider>
  );
}
