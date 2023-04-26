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
  onRevert?: (part: RevisionPart) => void;
  onEdit?: (part?: RevisionPart) => void;
  editMode?: boolean;
  globalEntity: boolean;
  partValidationError?: PartValidationErrorsVO;
  bindings: Bindings;
  revision?: boolean;
  itemTypeSettings: ItemType[];
  select: boolean;
};

const DetailRelatedPart: FC<Props> = ({
    label,
    part: {part, updatedPart},
    globalEntity,
    editMode,
    onDelete,
    onRevert = () => console.warn("Neni definovan 'onRevert' callback"),
    onEdit,
    globalCollapsed = true,
    partValidationError,
    bindings,
    revision,
    itemTypeSettings,
    select,
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

    
    const partBinding = part ? bindings.partsMap[part.id] : undefined;
    const hasBinding = partBinding != null;
    const hasLocalChange = hasBinding && !partBinding;
    const isRevisionModified = updatedPart?.changeType === "UPDATED";
    const isCollapsed = collapsed && !isRevisionModified;
    const isDeleted = updatedPart?.changeType === "DELETED";
    const isNew = updatedPart?.changeType === "NEW";
    const revisionItems = getRevisionItems(part?.items || undefined, updatedPart?.items || undefined)
    const areValuesEqual = (value: string, prevValue: string) => value === prevValue

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
                    {hasBinding && 
                        <SyncIcon syncState={!hasLocalChange ? SyncState.SYNC_OK : SyncState.LOCAL_CHANGE}/>
                    }
                    {showValidationError()}
                </div>

                <div className="actions hidable">
                    { editMode && !isDeleted &&
                        <SmallButton
                            onClick={() => onEdit && onEdit({part, updatedPart})}
                            title={i18n("ap.detail.edit", "")}
                        >
                            <Icon glyph={'fa-pencil'} />
                        </SmallButton>
                    }
                    {editMode && !isDeleted &&
                        <SmallButton
                            onClick={() => onDelete && onDelete({part, updatedPart})}
                            title={i18n("ap.detail.delete")}
                        >
                            <Icon glyph={'fa-trash'} />
                        </SmallButton>
                    }
                    {(isDeleted || hasLocalChange) &&
                        <SmallButton title={i18n("ap.detail.revert")} onClick={()=> onRevert({part, updatedPart})}>
                            <Icon glyph="fa-undo" />
                        </SmallButton>
                    }
                </div>
            </div>
        </div>

        {!isCollapsed && <div className={classNameContent}>
            <div>
                <DetailPartInfo
                    select={select}
                    items={revisionItems}
                    globalEntity={globalEntity}
                    bindings={bindings}
                    itemTypeSettings={itemTypeSettings}
                    isModified={isRevisionModified}
                    revision={revision}
                />
            </div>
        </div>}
    </div>
};

export default DetailRelatedPart;
