import {ApPartVO} from "../ApPartVO";

export const AePartBodyClass = "AePartBody";
export const AePartCreationClass = "AePartCreation";
export const AePartEventClass = "AePartEvent";
export const AePartExtinctionClass = "AePartExtinction";
export const AePartIdentifierClass = "AePartIdentifier";
export const AePartNameClass = "AePartName";
export const AePartRelationClass = "AePartRelation";

export type PartSectionsType = {
  names: ApPartVO[];
  creation: ApPartVO[];
  extinction: ApPartVO[];
  body: ApPartVO[];
  events: ApPartVO[];
  relations: ApPartVO[];
  identifiers: ApPartVO[];
};

export function getPartSections(parts: ApPartVO[]): PartSectionsType {
  const result: PartSectionsType = {
    names: [],
    creation: [],
    extinction: [],
    body: [],
    events: [],
    relations: [],
    identifiers: [],
  };

  parts.forEach(part => {
    switch (part["@class"]) {
      case AePartBodyClass:
        result.body.push((part as ApPartVO));
        break;
      case AePartCreationClass:
        result.creation.push((part as ApPartVO));
        break;
      case AePartEventClass:
        result.events.push((part as ApPartVO));
        break;
      case AePartExtinctionClass:
        result.extinction.push((part as ApPartVO));
        break;
      case AePartIdentifierClass:
        result.identifiers.push((part as ApPartVO));
        break;
      case AePartNameClass:
        result.names.push((part as ApPartVO));
        break;
      case AePartRelationClass:
        result.relations.push((part as ApPartVO));
        break;
      default:
        console.warn("Nepodporovaný typ partu", part["@class"], part);
        break;

    }
  });

  return result;
}
