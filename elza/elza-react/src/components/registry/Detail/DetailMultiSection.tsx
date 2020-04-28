import React, {FC, useContext, useState} from 'react';
import DetailPart from './DetailPart';
import {AePartNameVO, AePartVO, AeValidationErrorsVO} from '../../../api/generated/model';
import {AePartNameClass} from "../../../api/AePartInfo";
//import DetailActionButton from "../DetailActionButton";
//import {faPlus, faTrash} from "@fortawesome/free-solid-svg-icons";
import {connect} from "react-redux";
//import {CodelistData} from "../../shared/reducers/codelist/CodelistTypes";
import {compareParts} from "./partutils";
import DetailRelatedPart from "./DetailRelatedPart";
import Icon from '../../shared/icon/Icon';
import {MOCK_CODE_DATA} from './mock';

interface Props {
  label: string;
  parts: AePartVO[];
  relatedParts?: AePartVO[];
  globalCollapsed: boolean;
  onSetPreferred?: (part: AePartNameVO) => void;
  onDelete?: (part: AePartVO) => void;
  onEdit?: (part: AePartVO) => void;
  onAdd?: () => void;
  onAddRelated?: (part: AePartVO) => void;
  onDeleteParts?: (parts: Array<AePartVO>) => void;
  editMode?: boolean;
  codelist: any;
  globalEntity: boolean;
  deletableWholePart?: boolean;
  singlePart?: boolean;
  validationResult?: AeValidationErrorsVO;
}
const DetailMultiSection: FC<Props> = ({
                                         singlePart,
                                         editMode,
                                         label,
                                         parts,
                                         globalEntity,
                                         relatedParts = [],
                                         onSetPreferred,
                                         onDelete,
                                         onEdit,
                                         onAdd,
                                         onAddRelated,
                                         globalCollapsed,
                                         onDeleteParts,
                                         deletableWholePart,
                                         validationResult
}) => {
  if (!editMode && parts.length === 0) {
    return null;
  }

  let relatedPartsMap: Record<number, AePartVO[]> = {};
  if (relatedParts && relatedParts.length > 0) {
    relatedParts.forEach(rp => {
      if (rp.parentPartId) {
        if (!relatedPartsMap[rp.parentPartId]) {
          relatedPartsMap[rp.parentPartId] = [];
        }
        relatedPartsMap[rp.parentPartId].push(rp);
      }
    })
  }

  return (
    <div className="ml-3 mt-3 mr-3 pb-2 brb-1">
      <h3 className="mb-2">
        <span className="mr-1">{label}</span>
        {editMode && (!singlePart || (singlePart && parts.length === 0)) && <Icon
          className="ml-1"
          glyph={'fa-plus'}
          onClick={() => onAdd && onAdd()}
        />}
        {editMode && !singlePart && deletableWholePart && parts.length > 0 && <Icon
          className="ml-1"
          glyph={'fa-trash'}
          onClick={() => onDeleteParts && onDeleteParts(parts)}
        />}
      </h3>

      {[...parts].sort(compareParts).map((part, index) => {
        let preferred = false;
        if (part["@class"] === AePartNameClass) {
          preferred = (part as AePartNameVO).preferred;
        }

        return <div>
          <DetailPart
            key={index}
            part={part}
            label={part.textValue}
            singlePart={parts.length === 1}
            editMode={editMode}
            preferred={preferred}
            globalCollapsed={globalCollapsed}
            onDelete={onDelete}
            onEdit={onEdit}
            onAddRelated={onAddRelated}
            onSetPreferred={onSetPreferred ? onSetPreferred : () => {
              console.error("Není definován set preferred callback")
            }}
            validationResult={validationResult}
            globalEntity={globalEntity}
          />
          {part.id && relatedPartsMap[part.id] && relatedPartsMap[part.id].map((part, index) => {
            return <DetailRelatedPart
              key={index}
              part={part}
              label={part.textValue}
              editMode={editMode}
              globalCollapsed={globalCollapsed}
              onDelete={onDelete}
              onEdit={onEdit}
              validationResult={validationResult}
              globalEntity={globalEntity}
            />
          })}
        </div>
      })}
    </div>
  );
};

DetailMultiSection.defaultProps = {
  deletableWholePart: false
};

const mapStateToProps = ({codelist}: any) => ({
    codelist: MOCK_CODE_DATA
});

export default connect(
  mapStateToProps
)(DetailMultiSection);
