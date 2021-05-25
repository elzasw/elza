import classNames from "classnames";
import { SmallButton } from "components/shared/button/small-button";
import React, { FC, useEffect, useState } from 'react';
import { ApPartVO } from "../../../../api/ApPartVO";
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

type Props = {
  label: string;
  part: ApPartVO;
  globalCollapsed: boolean;
  onDelete?: (part: ApPartVO) => void;
  onEdit?: (part: ApPartVO) => void;
  editMode?: boolean;
  globalEntity: boolean;
  partValidationError?: PartValidationErrorsVO;
  bindings: Bindings;
  itemTypeSettings: ItemType[];
};

const DetailRelatedPart: FC<Props> = ({
    label,
    part,
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

    const classNameHeader = classNames( "detail-part-header",);

    // Rozbalený content
    const classNameContent = classNames( { "detail-part-expanded": !collapsed });

    const showValidationError = () => {
        if (editMode && partValidationError && partValidationError.errors && partValidationError.errors.length > 0) {
            return <ValidationResultIcon message={partValidationError.errors} />
        }
    };

    const partBinding = bindings.partsMap[part.id];

    return <div className="detail-related-part">
        <div className={classNameHeader + " align-items-center"}>
            <div style={{display: "flex", alignItems: "center"}}>
                <div
                    className={'detail-part-label d-inline-block'}
                    onClick={() => setCollapsed(!collapsed)}
                    title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}
                >
                    <span className={classNames('detail-part-label', '', collapsed ? false : 'opened')}>
                        <Icon className=""
                            glyph={'fa-link'}
                            />&nbsp;
                        {label || <i>Popis záznamu entity</i>}
                    </span>
                </div>

                <div className="actions">
                    {partBinding != null && 
                        <SyncIcon syncState={partBinding ? SyncState.SYNC_OK : SyncState.NOT_SYNCED}/>
                    }
                    {showValidationError()}
                </div>

                <div className="actions hidable">
                    { editMode &&
                        <SmallButton
                            onClick={() => onEdit && onEdit(part)}
                            title={i18n("ap.detail.edit", "")}
                        >
                            <Icon glyph={'fa-pencil'} />
                        </SmallButton>
                    }
                    {editMode && (
                        <SmallButton
                            onClick={() => onDelete && onDelete(part)}
                            title={i18n("ap.detail.delete")}
                        >
                            <Icon glyph={'fa-trash'} />
                        </SmallButton>
                    )}
                </div>
            </div>
        </div>

        {!collapsed && <div className={classNameContent}>
            <div>
                <DetailPartInfo
                    items={part.items || []}
                    globalEntity={globalEntity}
                    bindings={bindings}
                    itemTypeSettings={itemTypeSettings}
                />
            </div>
        </div>}
    </div>
};

export default DetailRelatedPart;
