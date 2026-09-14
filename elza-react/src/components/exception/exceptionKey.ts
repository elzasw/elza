/** Skupiny kódů chyb podle typu výjimky ze serveru. */
export const TYPE2GROUP: Record<string, string> = {
    ArrangementCode: "arr",
    BaseCode: "base",
    BulkActionCode: "ba",
    DigitizationCode: "dig",
    ExternalCode: "ext",
    OutputCode: "out",
    PackageCode: "pkg",
    RegistryCode: "reg",
    StructObjCode: "sobj",
    UserCode: "usr",
};

export interface ExceptionPayload {
    type?: string;
    code?: string;
    message?: string;
    properties?: Record<string, unknown> | null;
}

/** Id hlášky pro danou výjimku, nebo null, když typ neznáme. */
export function exceptionMessageId(data: ExceptionPayload): string | null {
    const group = data.type ? TYPE2GROUP[data.type] : undefined;
    if (!group || !data.code) {
        return null;
    }
    return `exception.${group}.${data.code}`;
}
