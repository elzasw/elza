import { DaDaoType } from "elza-api";

export const DaDaoTypeCaption = (value: DaDaoType): string => {
    switch (value) {
        case DaDaoType.Logical:
            return 'Úroveň inherentního popisu';
        case DaDaoType.Representation:
            return 'Reprezentace';
        case DaDaoType.File:
            return "Komponenta";
        case DaDaoType.Metaamd:
            return "Administrativní metadata";
        case DaDaoType.Metadmdinherent:
            return "Inherentní archivní popis";
        case DaDaoType.Metadmdcontextual:
            return "Kontextuální archivní popis";
        default:
            return 'Balíček';
    }
}
