import classNames from "classnames";
import { SmallButton } from "components/shared/button/small-button";
import React, { FC, useEffect, useState } from 'react';
import { ItemType } from "../../../../api/ApViewSettings";
import { PartValidationErrorsVO } from "../../../../api/PartValidationErrorsVO";
import { Bindings } from "../../../../types";
import i18n from "../../../i18n";
import Icon from '../../../shared/icon/Icon';
import ValidationResultIcon from "../../../ValidationResultIcon";
import { DetailPartInfo } from "./DetailPartInfo";
import "./DetailRelatedPart.scss";
import { SyncIcon } from "../sync-icon";
import { SyncState } from '../../../../api/SyncState';
import { PartName } from "./PartName";
import { RevisionPart, RevisionDisplay, getRevisionItems } from '../../revision';

type Props = {
  label: string;
  part: RevisionPart;
  globalCollapsed: boolean;
  onDelete?: (part?: RevisionPart) => void;
  onEdit?: (part?: RevisionPart) => void;
  editMode?: boolean;
  globalEntity: boolean;
  partValidationError?: PartValidationErrorsVO;
  bindings: Bindings;
  itemTypeSettings: ItemType[];
};

const DetailRelatedPart: FC<Props> = ({
    label,
    part: {part, updatedPart},
    globalEntity,
    editMode,
    onDelete,
    onEdit,
    globalCollapsed = true,
    partValidationError,
    bindings,
    itemTypeSettings,
}) => {
    const [collapsed, setCollapsed] = useState(true);
    // const [modalVisible, setModalVisible] = useState(false);

    useEffect(() => {
        setCollapsed(globalCollapsed);
    }, [globalCollapsed]);


    const showValidationError = () => {
        if (editMode && partValidationError && partValidationError.errors && partValidationError.errors.length > 0) {
            return <ValidationResultIcon message={partValidationError.errors} />
        }
    };

    const partBinding = !updatedPart && part ? bindings.partsMap[part.id] : false;
    const hasBinding = partBinding != null;
    const isModified = hasBinding && !partBinding;
    const isCollapsed = collapsed && !isModified;
    const isDeleted = updatedPart ? updatedPart.value == null : false;
    const isNew = !part?.value && updatedPart?.value != undefined;
    const areValuesEqual = (value: string, prevValue: string) => value === prevValue
    
    const revisionItems = getRevisionItems(part?.items || undefined, updatedPart?.items || undefined)

    const classNameHeader = classNames( "detail-part-header",);
    const classNameContent = classNames( { "detail-part-expanded": !isCollapsed }); // Rozbalený content
    
    return <div className="detail-related-part">
        <div className={classNameHeader + " align-items-center"}>
            <div style={{display: "flex", alignItems: "center"}}>
                <RevisionDisplay 
                    isDeleted={isDeleted}
                    isNew={isNew}
                    valuesEqual={areValuesEqual(part?.value || "", updatedPart ? updatedPart.value : part?.value || "")}
                    renderPrevValue={() => {
                        return <PartName 
                            label={part?.value || "no value"} 
                            collapsed={isCollapsed} 
                            onClick={() => setCollapsed(!collapsed)}
                            />
                    }} 
                    renderValue={() => {
                        return <PartName 
                            label={ updatedPart ? updatedPart.value : part?.value || "no new value"} 
                            collapsed={isCollapsed} 
                            onClick={() => setCollapsed(!collapsed)}
                            />
                    }} 
                >

                </RevisionDisplay>

                <div className="actions">
                    {partBinding != null && 
                        <SyncIcon syncState={partBinding ? SyncState.SYNC_OK : SyncState.LOCAL_CHANGE}/>
                    }
                    {showValidationError()}
                </div>

                <div className="actions hidable">
                    { editMode &&
                        <SmallButton
                            onClick={() => onEdit && onEdit({part, updatedPart})}
                            title={i18n("ap.detail.edit", "")}
                        >
                            <Icon glyph={'fa-pencil'} />
                        </SmallButton>
                    }
                    {editMode && (
                        <SmallButton
                            onClick={() => onDelete && onDelete({part, updatedPart})}
                            title={i18n("ap.detail.delete")}
                        >
                            <Icon glyph={'fa-trash'} />
                        </SmallButton>
                    )}
                </div>
            </div>
        </div>

        {!isCollapsed && <div className={classNameContent}>
            <div>
                <DetailPartInfo
                    items={revisionItems}
                    globalEntity={globalEntity}
                    bindings={bindings}
                    itemTypeSettings={itemTypeSettings}
                />
            </div>
        </div>}
    </div>
};

export default DetailRelatedPart;
