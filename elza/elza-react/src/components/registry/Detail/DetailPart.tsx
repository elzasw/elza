import React, {FC, useEffect, useState} from 'react';
import {Col, Row} from 'react-bootstrap';
import DetailItem from './DetailItem';
//import EditModal from '../EditModal';
import classNames from "classnames";
import "./DetailPart.scss";
import {AePartNameClass} from "../../../api/old/ApPartInfo";
//import DetailActionButton from "../DetailActionButton";
//import {CodelistData} from "../../shared/reducers/codelist/CodelistTypes";
import {connect} from "react-redux";
import DetailMultipleItem from "./DetailMultipleItem";
import Icon from '../../shared/icon/Icon';
import {ApPartVO} from "../../../api/ApPartVO";
import {ApValidationErrorsVO} from "../../../api/ApValidationErrorsVO";
import {ApItemWithTypeVO} from "../../../api/ApItemWithTypeVO";
import {RulPartTypeVO} from "../../../api/RulPartTypeVO";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {PartType} from "../../../api/generated/model";
//import {sortItems} from "../../itemutils";
//import ValidationResultIcon from "../ValidationResultIcon";

type Props = {
    label: string;
    part: ApPartVO;
    globalCollapsed: boolean;
    preferred?: boolean;
    onSetPreferred?: (part: ApPartVO) => void;
    onDelete?: (part: ApPartVO) => void;
    onEdit?: (part: ApPartVO) => void;
    onAddRelated?: (part: ApPartVO) => void;
    editMode?: boolean;
    singlePart: boolean;
    globalEntity: boolean;
    validationResult?: ApValidationErrorsVO;
} & ReturnType<typeof mapStateToProps>;

const DetailPart: FC<Props> = ({label, part, editMode, onSetPreferred, singlePart, onDelete, onEdit, globalCollapsed, preferred, onAddRelated, globalEntity, validationResult, descItemTypesMap, partTypesMap}) => {
    const [collapsed, setCollapsed] = useState(true);
    const [modalVisible, setModalVisible] = useState(false);
    const partType = partTypesMap[part.typeId];

    useEffect(() => {
        setCollapsed(globalCollapsed);
    }, [globalCollapsed]);

    const classNameHeader = classNames(
        "detail-part",
        "detail-part-header",
        {
            "pb-1": collapsed,
            "detail-part-preferred": preferred,
            "detail-part-expanded": !collapsed
        }
    );

    // Rozbalený content
    const classNameContent = classNames(
        "detail-part mb-4 pt-1",
        {
            "detail-part-preferred": preferred,
            "detail-part-expanded": !collapsed
        }
    );

    let showPreferredSwitch = false;
    if (partType.code === PartType.NAME) {
        showPreferredSwitch = !singlePart;
    }

    const renderItems = (items: ApItemWithTypeVO[]) => {
        if (items.length === 0) {
            return <Col className={"mt-1"}><i>Nejsou definovány žádné hodnoty atributů</i></Col>;
        }

        let result: any = [];

        let index = 0;
        while (index < items.length) {
            let index2 = index + 1;
            while (index2 < items.length && items[index].typeId === items[index2].typeId) {
                index2++;
            }

            let itemInfo = items[index].type;
            let width = itemInfo && itemInfo.width ? itemInfo.width : 2;

            let sameItems = items.slice(index, index2);
            index = index2;

            let rows: any = [];
            if (sameItems.length > 1) {
                rows.push(<DetailMultipleItem key={index} items={sameItems} globalEntity={globalEntity}/>);
            } else {
                rows.push(<DetailItem key={index} item={sameItems[0]} globalEntity={globalEntity}/>);
            }

            result.push(<Col key={index} xs={width <= 0 ? 12 : width}>
                {rows}
            </Col>);
        }

        return result;
    };

    const itemsWithType = ((part.items ? part.items : []) as ApItemWithTypeVO[]).map((i) => {
        i.type = descItemTypesMap[i.typeId] ? descItemTypesMap[i.typeId] : null;
        return i;
    });

    const sortedItems = itemsWithType.sort((a, b) => {
        if (a.type && b.type) {
            return a.type.viewOrder - b.type.viewOrder;
        }

        return 0;
    });

    const showValidationError = () => {
        if (validationResult && validationResult.partErrors && validationResult.partErrors.length > 0) {
            const index = validationResult.partErrors.findIndex(value => value.id === part.id);
            if (index >= 0) {
                const errors = validationResult.partErrors[index].errors;
                if (errors && errors.length > 0) {
                    return <Col>
                        ValidationResultIcon
                        {validationResult.partErrors[index].errors}
                    </Col>;
                }
            }
        }
    };

    return <div className="detail-part ml-4 mb-2 pt-3">
        <Row className={classNameHeader + " align-items-center"}>
            <Col>
                <div
                    className={'detail-part-label d-inline-block'}
                    onClick={() => setCollapsed(!collapsed)}
                    title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}
                >
                <span
                    className={classNames('detail-part-label', preferred ? 'preferred' : 'mr-2', collapsed ? false : 'opened')}>
                    {label || <i>Popis záznamu entity</i>}
                </span>
                    {preferred && <span
                        className={classNames("detail-part-label-alt mr-2", collapsed ? false : 'opened')}> (preferované)</span>}
                </div>
                {showPreferredSwitch && !preferred && <Icon
                    className={'mr-2 cursor-pointer'}
                    glyph={'fa-star'}
                    onClick={() => onSetPreferred && onSetPreferred(part)}
                    style={{visibility: preferred ? "hidden" : "inherit"}}
                />}

                <Icon
                    className={'mr-2 cursor-pointer'}
                    glyph={'fa-pencil'}
                    onClick={() => onEdit && onEdit(part)}
                />

                {!preferred && <Icon
                    className={'mr-2 cursor-pointer'}
                    glyph={'fa-trash'}
                    onClick={() => onDelete && onDelete(part)}
                />}
                {onAddRelated && <Icon
                    className={'mr-2 cursor-pointer'}
                    glyph={'fa-plus'}
                    onClick={() => onAddRelated(part)}
                />}
            </Col>
            <Col style={{flex: 1}} className="ml-2">
                {showValidationError()}
            </Col>
        </Row>

        {!collapsed && <div className={classNameContent}>
            <Row>
                {renderItems(sortedItems)}
            </Row>
        </div>}
    </div>
};

const mapStateToProps = (state) => ({
    partTypesMap: state.refTables.partTypes.itemsMap as Record<number, RulPartTypeVO>,
    descItemTypesMap: state.refTables.descItemTypes.itemsMap as Record<number, RulDescItemTypeExtVO>,
});

export default connect(
    mapStateToProps
)(DetailPart);
