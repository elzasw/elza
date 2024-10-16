import React from 'react';
import {AbstractReactComponent, Autocomplete, i18n} from 'components/shared';
import classNames from 'classnames';

import './DescItemTypeSpec.scss';
import {getInfoSpecType, ItemAvailability} from '../../../stores/app/accesspoint/itemFormUtils';

/**
 * Komponenta pro zobrazení specifikace pro daný atribut.
 */
class DescItemTypeSpec extends AbstractReactComponent {
    constructor(props) {
        super(props);

        // Při vstupu inicializujeme filtr položek na aktuálne vybranou položku
        this.state = this.getStateFromProps(props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        // Při změně hodnoty inicializujeme filtr položek na aktuálne vybranou položku
        if (this.props.descItem.descItemSpecId !== nextProps.descItemSpecId) {
            // při externí změně aktualizujeme seznam položek
            this.setState(this.getStateFromProps(nextProps));
        }
    }

    getStateFromProps = (props = this.props) => {
        const {refType, infoType, strictMode} = props;

        const specItemsInfo = this.getSpecItems(refType, infoType, '', strictMode); // filr je vždy vypnutý, nově se filtruje v autocomplete;
        return {
            items: specItemsInfo.items,
            favoriteItems: specItemsInfo.favoriteItems,
        };
    };

    /**
     * Sestaví strom specifikací, pokud je vyžadován strom, jinak sestaví plochý seznam. Vytváří nové instance položek stromu.
     * @param refType obecná definice typu
     * @param infoType definice typu pro daný formulář
     * @param filterText filtrovací text, uvežuje se pouze v případě stromu
     * @param strictMode true, pokud se jedná o striktní mód pořádání
     */
    getSpecItems = (refType, infoType, filterText, strictMode) => {
        let items;
        let favoriteItems = [];

        const lowerFilterText = filterText ? filterText.toLocaleLowerCase() : filterText;

        // Položky
        if (refType.itemSpecsTree && refType.itemSpecsTree.length > 0) {
            // stromová reprezentace
            items = [];
            const treeNodeIndex = {index: 0};
            refType.itemSpecsTree.forEach(node => {
                const newNode = this.getSpecTreeNode(
                    node,
                    refType,
                    infoType,
                    lowerFilterText,
                    treeNodeIndex,
                    strictMode,
                );
                if (newNode) {
                    items.push(newNode);
                }
            });
        } else {
            // plochý seznam
            items = [];
            infoType.specs.forEach(spec => {
                const refSpec = refType.descItemSpecsMap[spec.id];
                if (
                    !lowerFilterText ||
                    (lowerFilterText && refSpec.name.toLocaleLowerCase().indexOf(lowerFilterText) >= 0)
                ) {
                    // vypnutý filtr nebo položka vyhovuje filtru
                    const infoSpec = infoType.descItemSpecsMap[spec.id];
                    const infoSpecType = getInfoSpecType(infoSpec.type);
                    if (!strictMode || (strictMode && infoSpecType !== ItemAvailability.IMPOSSIBLE)) {
                        items.push({
                            ...refSpec,
                            ...spec,
                            className: 'spec-' + infoSpecType.toLowerCase(),
                        });
                    }
                }
            });
        }

        // Oblíbené položky
        if (infoType.favoriteSpecIds && infoType.favoriteSpecIds.length > 0) {
            // má oblíbené položky
            infoType.favoriteSpecIds.forEach(specId => {
                const refSpec = refType.descItemSpecsMap[specId];
                if (
                    !lowerFilterText ||
                    (lowerFilterText && refSpec.name.toLocaleLowerCase().indexOf(lowerFilterText) >= 0)
                ) {
                    // vypnutý filtr nebo položka vyhovuje filtru
                    const infoSpec = infoType.descItemSpecsMap[specId];
                    const infoSpecType = getInfoSpecType(infoSpec.type);
                    if (!strictMode || (strictMode && infoSpecType !== ItemAvailability.IMPOSSIBLE)) {
                        favoriteItems.push(specId);
                    }
                }
            });
        }

        return {
            items,
            favoriteItems,
        };
    };

    /**
     * Sestavuje strom pro daný node a jeho podstrom.
     * @param node node
     * @param refType ref type
     * @param infoType info type
     * @param lowerFilterText filtr
     * @param treeNodeIndex index položky ve stromu (index plochého rozbaleného seznamu)
     * @param strictMode true, pokud se jedná o striktní mód pořádání
     * @return {*}
     */
    getSpecTreeNode = (node, refType, infoType, lowerFilterText, treeNodeIndex, strictMode) => {
        switch (node.type) {
            case 'GROUP': {
                const newNode = {
                    id: 'g-' + treeNodeIndex.index++,
                    name: node.name,
                    node: true,
                    expanded: !!lowerFilterText,
                    children: [],
                };

                // Přidání potomků
                node.children.forEach(child => {
                    const newChild = this.getSpecTreeNode(
                        child,
                        refType,
                        infoType,
                        lowerFilterText,
                        treeNodeIndex,
                        strictMode,
                    );
                    newChild && newNode.children.push(newChild);
                });

                if (newNode.children.length === 0) {
                    // nemá potomky, vůbec ho nechceme ve stromu
                    return null;
                }

                return newNode;
            }
            case 'ITEM': {
                const infoSpec = infoType.descItemSpecsMap[node.specId];
                if (infoSpec) {
                    // jen když je pro daný formulář povolena
                    const refSpec = refType.descItemSpecsMap[node.specId];

                    // Filtr
                    if (
                        !lowerFilterText ||
                        (lowerFilterText && refSpec.name.toLocaleLowerCase().indexOf(lowerFilterText) >= 0)
                    ) {
                        // vypnutý filtr nebo položka vyhovuje filtru
                        const infoSpecType = getInfoSpecType(infoSpec.type);
                        if (!strictMode || (strictMode && infoSpecType !== ItemAvailability.IMPOSSIBLE)) {
                            return {
                                ...refSpec,
                                ...infoSpec,
                                className: 'spec-' + infoSpecType.toLowerCase(),
                            };
                        }
                    }
                }
                return null;
            }
            default:
                break;
        }
    };

    focus() {
        this.refs.autocomplete.focus();
    }

    /**
     * Načtení aktuálně vybrané hodnoty - jako desc item type - složení ref a info type.
     * @param descItem hodnota
     * @param refType ref type
     * @param infoType info type
     * @return {*}
     */
    getValueObj = (descItem, refType, infoType) => {
        let value;
        if (descItem.descItemSpecId !== null && descItem.descItemSpecId !== '') {
            const info = infoType.descItemSpecsMap[descItem.descItemSpecId];
            const ref = refType.descItemSpecsMap[descItem.descItemSpecId];
            value = {...ref, ...info};
        }
        return value;
    };

    render() {
        const {onChange, onBlur, onFocus, descItem, locked, infoType, refType, readMode} = this.props;
        const {favoriteItems, items} = this.state;

        if (readMode) {
            let nameVal;
            if (descItem.descItemSpecId == null || descItem.descItemSpecId == '') {
                nameVal = '';
            } else {
                nameVal = refType.descItemSpecsMap[descItem.descItemSpecId].name;
            }
            return (
                <span key="spec" className={classNames("desc-item-spec-label", {"inhibited": descItem.inhibited})} title={nameVal}>
                    {nameVal}
                </span>
            );
        }

        const cls = classNames({
            value: true,
            'desc-item-spec': true,
            error: descItem.error.spec,
            active: descItem.hasFocus,
        });

        const descItemSpecProps = {
            onChange: value => onChange(value ? value.id : null),
            onBlur: value => onBlur(value ? value.id : null),
            onFocus,
            disabled: locked,
            readOnly: descItem.saving,
        };

        // Získání hodnoty jako objekt specifikace = autocomplete pořebuje na vstupu objekt
        const value = this.getValueObj(descItem, refType, infoType);

        // Doplňující vlastnosti v případě rozdílných typů - strom nebo list
        let autocompleteAdditionalProps;
        if (refType.itemSpecsTree && refType.itemSpecsTree.length > 0) {
            // tree
            autocompleteAdditionalProps = {
                tree: true,
                allowSelectItem: item => !item.node && !item.label,
                allowFocusItem: item => !item.group && !item.label,
            };
        } else {
            // list
            autocompleteAdditionalProps = {
                allowSelectItem: item => !item.group && !item.label,
                allowFocusItem: item => !item.group && !item.label,
            };
        }

        // ---

        return (
            <Autocomplete
                key="spec"
                ref="autocomplete"
                {...descItemSpecProps}
                className={cls}
                value={value}
                title={descItem.error.spec}
                items={items}
                favoriteItems={favoriteItems}
                itemsTitleItem={{label: true, className: 'spec-group', name: i18n('subNodeForm.descItemType.spec.all')}}
                favoriteItemsTitleItem={{
                    label: true,
                    className: 'spec-group',
                    name: i18n('subNodeForm.descItemType.spec.favorite'),
                }}
                {...autocompleteAdditionalProps}
            />
        );
    }
}

export default DescItemTypeSpec;
