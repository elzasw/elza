import {
  AePartBodyVO,
  AePartCreationVO,
  AePartEventVO,
  AePartExtinctionVO, AePartIdentifierVO,
  AePartNameVO, AePartRelationVO,
  AePartVO
} from "./generated/model";

export const AePartBodyClass = "AePartBody";
export const AePartCreationClass = "AePartCreation";
export const AePartEventClass = "AePartEvent";
export const AePartExtinctionClass = "AePartExtinction";
export const AePartIdentifierClass = "AePartIdentifier";
export const AePartNameClass = "AePartName";
export const AePartRelationClass = "AePartRelation";

export type PartSectionsType = {
  names: AePartNameVO[];
  creation: AePartCreationVO[];
  extinction: AePartExtinctionVO[];
  body: AePartBodyVO[];
  events: AePartEventVO[];
  relations: AePartRelationVO[];
  identifiers: AePartIdentifierVO[];
};

export function getPartSections(parts: AePartVO[]): PartSectionsType {
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
        result.body.push((part as AePartBodyVO));
        break;
      case AePartCreationClass:
        result.creation.push((part as AePartCreationVO));
        break;
      case AePartEventClass:
        result.events.push((part as AePartEventVO));
        break;
      case AePartExtinctionClass:
        result.extinction.push((part as AePartExtinctionVO));
        break;
      case AePartIdentifierClass:
        result.identifiers.push((part as AePartIdentifierVO));
        break;
      case AePartNameClass:
        result.names.push((part as AePartNameVO));
        break;
      case AePartRelationClass:
        result.relations.push((part as AePartRelationVO));
        break;
      default:
        console.warn("Nepodporovaný typ partu", part["@class"], part);
        break;

    }
  });

  return result;
}
