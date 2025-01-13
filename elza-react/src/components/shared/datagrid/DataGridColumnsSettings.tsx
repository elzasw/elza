/**
 * Komponenta výběru sloupců pro grid - s možností změny jejich pořadí.
 */
import React, { useEffect, useState } from 'react';
import { FormControl, Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import { getMapFromList } from 'stores/app/utils';

import './DataGridColumnsSettings.scss';
import ListBox from '../listbox/ListBox';
import { Api } from 'api';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { UsedItemType } from 'elza-api';
import { Icon, TooltipTrigger } from '..';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { globalMessages } from '../lang';

const messages = defineMessages({
    add: {
        id: "dataGridColumns_add",
        defaultMessage: "Přidat",
    },
    remove: {
        id: "dataGridColumns_remove",
        defaultMessage: "Odebrat",
    },
    addUsed: {
        id: "dataGridColumns_addUsed",
        defaultMessage: "Přidat použité",
    },
    visible: {
        id: "dataGridColumns_visible",
        defaultMessage: "Vybrané"
    },
    available: {
        id: "dataGridColumns_available",
        defaultMessage: "Dostupné"
    }
})

interface Column {
    code: string;
    id: string | number;
    desc: string;
    name: string;
}

interface Item {
    item: Column;
    active?: boolean;
    index: number;
}

interface Props {
    columns: Column[];
    itemTypeCodes: string[];
    visibleColumns: Record<string, boolean>;
    onClose: () => void;
    onSubmitForm: (visible: Column[]) => void;
    className?: string;
}

export default function DataGridColumnSettingsFn({
    columns = [],
    itemTypeCodes = [],
    visibleColumns,
    onClose,
    onSubmitForm,
    className,
}: Props) {
    const [leftSelected, setLeftSelected] = useState<string[]>([]);
    const [rightSelected, setRightSelected] = useState<string[]>([]);
    const [visible, setVisible] = useState<Column[]>([]);
    const [available, setAvailable] = useState<Column[]>(columns || []);
    const [usedItemTypes, setUsedItemTypes] = useState<UsedItemType[]>([]);
    const [leftFilter, setLeftFilter] = useState("");
    const [rightFilter, setRightFilter] = useState("");
    const intl = useIntl();

    const fund = useSelector(({ arrRegion }: AppState) => {
        if (arrRegion.activeIndex != undefined && arrRegion.activeIndex < arrRegion.funds.length) {
            return arrRegion.funds[arrRegion.activeIndex]
        }
        return undefined;
    })

    useEffect(() => {
        if (fund?.id != undefined && fund?.versionId != undefined) {
            (async () => {
                const { data } = await Api.funds.fundUsedItemTypes(parseInt(fund.id.toString()), fund.versionId)
                setUsedItemTypes(data);
            })()
        }
    }, [fund?.id, fund?.versionId])

    useEffect(() => {
        const visible: Column[] = [];
        const available: Column[] = [];
        columns.forEach(col => {
            if (visibleColumns[col.id]) {
                visible.push(col);
            } else {
                if (itemTypeCodes?.length > 0) {
                    itemTypeCodes.forEach(itemTypeCode => {
                        if (itemTypeCode === col.code) {
                            available.push(col);
                        }
                    });
                } else {
                    available.push(col);
                }
            }
        });

        // Seřazení dostupných sloupečků podle abecedy
        available.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

        setAvailable(available);
        setVisible(visible);
    }, [columns, itemTypeCodes, visibleColumns]);

    function handleChangeOrder(from: number, to: number) {
        const _visible = [...visible];

        const moved = _visible.splice(from, 1);
        _visible.splice(to, 0, ...moved); // insert moved item on 'to' index

        const _rightSelected = [to.toString()];

        setVisible(_visible);
        setRightSelected(_rightSelected)
    }

    /* Unused version for multiple items*/
    /*
    function handleChangeOrder(_from: number, to: number) {
        const _visible = [...visible];

        const moved:Column[] = [];

        // extract all selected
        rightSelected.forEach((selected) => {
            moved.push(..._visible.splice(parseInt(selected), 1))
        })
        _visible.splice(to, 0, ...moved); // insert moved item on 'to' index

        const _rightSelected = moved.map((_column, index) => (to+index).toString());

        setVisible(_visible);
        setRightSelected(_rightSelected)
    }
    */

    function handleAddVisible() {
        const selectedMap = {};
        leftSelected.forEach(index => {
            selectedMap[index] = true;
        });

        const newAvailable = [];
        const newVisible = [...visible];

        available.forEach((item, index) => {
            if (selectedMap[index]) {
                newVisible.push(item);
            } else {
                newAvailable.push(item);
            }
        });

        const newRightSelected = [];
        const originalLength = visible.length;
        for (let newLength = newVisible.length; newLength > originalLength; newLength--) {
            newRightSelected.push(newLength - 1);
        }

        setAvailable(newAvailable);
        setVisible(newVisible);
        setRightSelected(newRightSelected);
        setLeftSelected([]);
    }

    function handleAddVisibleFromUsed() {
        const newAvailable = [];
        const newVisible = [...visible];

        available.forEach((item) => {
            const isUsed = usedItemTypes.find((used) => used.rulItemTypeId === item.id)
            if (isUsed) {
                newVisible.push(item);
            } else {
                newAvailable.push(item);
            }
        });

        const newRightSelected = [];
        const originalLength = visible.length;
        for (let newLength = newVisible.length; newLength > originalLength; newLength--) {
            newRightSelected.push(newLength - 1);
        }

        setAvailable(newAvailable);
        setVisible(newVisible);
        setRightSelected(newRightSelected);
        setLeftSelected([]);
    }

    function handleRemoveVisible() {
        const selectedMap = {};
        rightSelected.forEach(index => {
            selectedMap[index] = true;
        });

        const rightSelectedIds = [];
        visible.forEach((item, index) => {
            if (selectedMap[index]) {
                rightSelectedIds.push(item.id);
            }
        });

        // Upravení seznamu visible
        const newVisible = [];
        visible.forEach((item, index) => {
            if (!selectedMap[index]) {
                newVisible.push(item);
            }
        });

        // Získání nového seznamu available
        const visibleMap = getMapFromList(newVisible);
        const newAvailable = [];
        columns.forEach(col => {
            if (!visibleMap[col.id]) {
                if (itemTypeCodes !== null && itemTypeCodes.length !== 0) {
                    itemTypeCodes.forEach(itemTypeCode => {
                        if (itemTypeCode === col.code) {
                            newAvailable.push(col);
                        }
                    });
                } else {
                    newAvailable.push(col);
                }
            }
        });
        // Seřazení dostupných sloupečků podle abecedy
        newAvailable.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

        // ---

        // Ziskání

        const newLeftSelected = [];
        newAvailable.forEach((item, index) => {
            if (rightSelectedIds.indexOf(item.id) > -1) {
                newLeftSelected.push(index);
            }
        });

        setAvailable(newAvailable);
        setVisible(newVisible);
        setRightSelected([]);
        setLeftSelected(newLeftSelected);
    }

    function handleChangeLeftSelection(selected: string[]) {
        setLeftSelected(selected);
    }

    function handleChangeRightSelection(selected: string[]) {
        setRightSelected(selected);
    }

    function renderItemContent({ item }: Item) {
        const usedItemType = usedItemTypes?.find(type => type.rulItemTypeId == item.id);
        return <div style={{ fontWeight: usedItemType?.count > 0 ? "bold" : undefined }}>{item.name} {usedItemType && `(${usedItemType?.count})`}</div>;
    }

    const cls = className ? 'datagrid-columns-settings-container ' + className : 'datagrid-columns-settings-container';

    const filteredAvailable = available.filter((item) => item.name.toLowerCase().indexOf(leftFilter.toLowerCase()) > -1);
    const filteredVisible = visible.filter((item) => item.name.toLowerCase().indexOf(rightFilter.toLowerCase()) > -1);
    const usedAvailable = available.filter((item) => usedItemTypes.find(type => type.rulItemTypeId == item.id));

    return (
        <div>
            <Modal.Body>
                <div className={cls}>
                    <div className="panels-container">
                        <div className="left">
                            <h4><FormattedMessage {...messages.available} /></h4>
                            <FormControl className="listbox-filter" onChange={({ currentTarget }) => setLeftFilter(currentTarget.value)} placeholder='Filtrovat...' />
                            <ListBox
                                items={filteredAvailable}
                                multiselect
                                activeIndexes={leftSelected}
                                renderItemContent={renderItemContent}
                                onChangeSelection={handleChangeLeftSelection}
                                onDoubleClick={handleAddVisible}
                            />
                        </div>
                        <div className="center">
                            <div className="action-buttons">
                                <TooltipTrigger content={intl.formatMessage(messages.add)} placement="top">
                                    <Button variant="outline-secondary" disabled={leftSelected.length === 0} onClick={handleAddVisible}>
                                        <Icon glyph="fa-angle-right" />
                                    </Button>
                                </TooltipTrigger>
                                <TooltipTrigger content={intl.formatMessage(messages.addUsed)} placement="top">
                                    <Button variant="outline-secondary" disabled={usedAvailable.length === 0} onClick={handleAddVisibleFromUsed}>
                                        <Icon glyph="fa-angle-right" />
                                        <Icon glyph="fa-angle-right" />
                                    </Button>
                                </TooltipTrigger>
                                <TooltipTrigger content={intl.formatMessage(messages.remove)} placement="top">
                                    <Button variant="outline-secondary" disabled={rightSelected.length === 0} onClick={handleRemoveVisible}>
                                        <Icon glyph="fa-angle-left" />
                                    </Button>
                                </TooltipTrigger>
                            </div>
                        </div>
                        <div className="right">
                            <h4><FormattedMessage {...messages.visible} /></h4>
                            <FormControl className="listbox-filter" onChange={({ currentTarget }) => setRightFilter(currentTarget.value)} placeholder='Filtrovat...' />
                            <ListBox
                                items={filteredVisible}
                                sortable
                                multiselect
                                activeIndexes={rightSelected}
                                renderItemContent={renderItemContent}
                                onChangeOrder={handleChangeOrder}
                                onChangeSelection={handleChangeRightSelection}
                                onDoubleClick={handleRemoveVisible}
                            />
                        </div>
                    </div>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="outline-secondary" onClick={() => onSubmitForm(visible)}>
                    <FormattedMessage {...globalMessages.ok} />
                </Button>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...globalMessages.cancel} />
                </Button>
            </Modal.Footer>
        </div>
    );
}
