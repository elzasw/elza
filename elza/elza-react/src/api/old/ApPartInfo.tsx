import {
  ApPartBodyVO,
  ApPartCreationVO,
  ApPartEventVO,
  AePartExtinctionVO, AePartIdentifierVO,
  ApPartNameVO, AePartRelationVO,

} from "../generated/model";
import {ApPartVO} from "../ApPartVO";

export const AePartBodyClass = "AePartBody";
export const AePartCreationClass = "AePartCreation";
export const AePartEventClass = "AePartEvent";
export const AePartExtinctionClass = "AePartExtinction";
export const AePartIdentifierClass = "AePartIdentifier";
export const AePartNameClass = "AePartName";
export const AePartRelationClass = "AePartRelation";

export type PartSectionsType = {
  names: ApPartNameVO[];
  creation: ApPartCreationVO[];
  extinction: AePartExtinctionVO[];
  body: ApPartBodyVO[];
  events: ApPartEventVO[];
  relations: AePartRelationVO[];
  identifiers: AePartIdentifierVO[];
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
        result.body.push((part as ApPartBodyVO));
        break;
      case AePartCreationClass:
        result.creation.push((part as ApPartCreationVO));
        break;
      case AePartEventClass:
        result.events.push((part as ApPartEventVO));
        break;
      case AePartExtinctionClass:
        result.extinction.push((part as AePartExtinctionVO));
        break;
      case AePartIdentifierClass:
        result.identifiers.push((part as AePartIdentifierVO));
        break;
      case AePartNameClass:
        result.names.push((part as ApPartNameVO));
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
