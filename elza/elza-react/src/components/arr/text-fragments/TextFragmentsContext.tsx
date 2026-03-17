import { createContext, PropsWithChildren, useContext, useState } from "react";

type FieldCallback = (char: string) => void;

export interface TextFragmentsContextValue {
    registerField: (field: HTMLElement, callback: FieldCallback) => void;
    unregisterField: () => void;
    insertText: (text: string) => void;
    hasActiveField: boolean;
}

export const TextFragmentsContext = createContext<TextFragmentsContextValue | undefined>(undefined);

export function useTextFragmentsContext() {
    return useContext(TextFragmentsContext);
}

export function TextFragmentsProvider({ children }: PropsWithChildren) {
    const value = useTextFragmentsState();
    return <TextFragmentsContext.Provider value={value}>{children}</TextFragmentsContext.Provider>;
}

function useTextFragmentsState(): TextFragmentsContextValue {
    const [activeField, setActiveField] = useState<{ field: HTMLElement; callback: FieldCallback } | null>(null);

    function registerField(field: HTMLElement, callback: FieldCallback) {
        setActiveField({ field, callback });
    }

    function unregisterField() {
        setActiveField(null);
    }

    function insertText(text: string) {
        activeField?.callback(text);
    }

    return {
        registerField,
        unregisterField,
        insertText,
        hasActiveField: activeField != null,
    };
}
