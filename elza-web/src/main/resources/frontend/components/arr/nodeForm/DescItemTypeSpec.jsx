import React from 'react';
import {Autocomplete, Icon, i18n, AbstractReactComponent} from 'components/index.jsx';
import {getSetFromIdsList} from "stores/app/utils.jsx";
import classNames from 'classnames';

import "./DescItemTypeSpec.less";

/**
 * Komponenta pro zobrazení specifikace pro daný atribut.
 */
class DescItemTypeSpec extends AbstractReactComponent {

    constructor(props) {
        super(props);

        // Při vstupu inicializujeme filtr položek na aktuálne vybranou položku
        this.state = this.getStateFromProps(props);
    }

    componentWillReceiveProps(nextProps) {
        // Při změně hodnoty inicializujeme filtr položek na aktuálne vybranou položku
        if (this.props.descItem.descItemSpecId !== nextProps.descItemSpecId) {  // při externí změně aktualizujeme seznam položek
            this.setState(this.getStateFromProps(nextProps));
        }
    }

    getStateFromProps = (props = this.props) => {
        const {refType, infoType, strictMode} = props;

        const specItemsInfo = this.getSpecItems(refType, infoType, "", strictMode) // filr je vždy vypnutý, nově se filtruje v autocomplete;
        return {
            items: specItemsInfo.items,
            favoriteItems: specItemsInfo.favoriteItems,
        }
    };

    /**
     * Sestaví strom specifikací, pokud je vyžadován strom, jinak sestaví plochý seznam. Vytváří nové instance položek stromu.
     * @param refType obecná definice typu
     * @param infoType definice typu pro daný formulář
     * @param filterText filtrovací text, uvežuje se pouze v případě stromu
     */
    getSpecItems = (refType, infoType, filterText, strictMode) => {
        let items;
        let favoriteItems = [];

        const lowerFilterText = filterText ? filterText.toLocaleLowerCase() : filterText;

        // Položky
        if (refType.itemSpecsTree && refType.itemSpecsTree.length > 0) {    // stromová reprezentace
            items = [];
            const treeNodeIndex = {index: 0};
            refType.itemSpecsTree.forEach(node => {
                const newNode = this.getSpecTreeNode(node, refType, infoType, lowerFilterText, treeNodeIndex);
                if (newNode) {
                    items.push(newNode);
                }
            });
        } else {    // plochý seznam
            items = [];
            infoType.specs.forEach(spec => {
                const refSpec = refType.descItemSpecsMap[spec.id];
                if (!lowerFilterText || (lowerFilterText && refSpec.name.toLocaleLowerCase().indexOf(lowerFilterText) >= 0)) { // vypnutý filtr nebo položka vyhovuje filtru
                    const infoSpec = infoType.descItemSpecsMap[spec.id];
                    items.push({
                        ...refSpec,
                        ...spec,
                        className: 'spec-' + infoSpec.type.toLowerCase()
                    });
                }
            });
        }

        // Oblíbené položky
        if (infoType.favoriteSpecIds && infoType.favoriteSpecIds.length > 0) {  // má oblíbené položky
            infoType.favoriteSpecIds.forEach(specId => {
                const refSpec = refType.descItemSpecsMap[specId];
                if (!lowerFilterText || (lowerFilterText && refSpec.name.toLocaleLowerCase().indexOf(lowerFilterText) >= 0)) { // vypnutý filtr nebo položka vyhovuje filtru
                    const infoSpec = infoType.descItemSpecsMap[specId];
                    const value = {
                        ...refSpec,
                        ...infoSpec,
                        className: 'spec-' + infoSpec.type.toLowerCase()
                    };
                    favoriteItems.push(value);
                }
            });

            // Pokud nějaké jsou, upravíme výstupní result a přidáme je tam včetně skupin, které oddělují oblíbené a ostatní položky
            if (items.length > 0) {
                result = [
                    {id: -1111, name: i18n("subNodeForm.descItemType.spec.favorite"), className: "spec-group", group: true},
                    ...items,
                    {id: -2222, name: i18n("subNodeForm.descItemType.spec.all"), className: "spec-group", group: true},
                    ...result
                ]
            }
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
     * @return {*}
     */
    getSpecTreeNode = (node, refType, infoType, lowerFilterText, treeNodeIndex) => {
        // Specifikace pro daný atribut v pro konkrétní formulář
        const specChildren = [];
        const nodeSpecIdsMap = getSetFromIdsList(node.specIds);
        infoType.specs.forEach(infoSpec => {    // projdeme všechny specifikace pro daný atribut a formulář
            if (nodeSpecIdsMap[infoSpec.id]) {   // v daném nodu má být konkrétní specifikace, může pro daný atribut být v daném formuláři
                const refSpec = refType.descItemSpecsMap[infoSpec.id];

                // Filtr
                if (!lowerFilterText || (lowerFilterText && refSpec.name.toLocaleLowerCase().indexOf(lowerFilterText) >= 0)) { // vypnutý filtr nebo položka vyhovuje filtru
                    specChildren.push({
                        ...refSpec,
                        ...infoSpec,
                        className: 'spec-' + infoSpec.type.toLowerCase()
                    });
                }
            }
        });

        // Podřízené nody
        const children = [];
        node.children && node.children.forEach(subNode => {
            const newSubNode = this.getSpecTreeNode(subNode, refType, infoType, lowerFilterText, treeNodeIndex);
            if (newSubNode) {
                children.push(newSubNode);
            }
        });

        if (specChildren.length === 0 && children.length === 0) {   // nemá potomky, vůbec ho nechceme ve stromu
            return null;
        }

        return {
            id: treeNodeIndex.index++,
            name: node.name,
            node: true,
            expanded: !!lowerFilterText,
            children: [...children, ...specChildren]
        };
    };

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
            if (descItem.descItemSpecId == null || descItem.descItemSpecId == "") {
                nameVal = "";
            } else {
                nameVal = refType.descItemSpecsMap[descItem.descItemSpecId].name;
            }
            return (
                <span key="spec" className="desc-item-spec-label">{nameVal}</span>
            )
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
            disabled: locked
        };

        // Získání hodnoty jako objekt specifikace = autocomplete pořebuje na vstupu objekt
        const value = this.getValueObj(descItem, refType, infoType);

        // Doplňující vlastnosti v případě rozdílných typů - strom nebo list
        let autocompleteAdditionalProps;
        if (refType.itemSpecsTree && refType.itemSpecsTree.length > 0) {    // tree
            autocompleteAdditionalProps = {
                tree: true,
                allowSelectItem: (id, item) => !item.node && !item.label,
                allowFocusItem: (id, item) => !item.group && !item.label,
            }
        } else {    // list
            autocompleteAdditionalProps = {
                allowSelectItem: (id, item) => !item.group && !item.label,
                allowFocusItem: (id, item) => !item.group && !item.label,
            };
        }

        // ---

        return <Autocomplete
            key="spec"
            {...descItemSpecProps}
            className={cls}
            value={value}
            title={descItem.error.spec}
            items={items}
            favoriteItems={favoriteItems}
            itemsTitleItem={{label: true, className: "spec-group", name: i18n("subNodeForm.descItemType.spec.all")}}
            favoriteItemsTitleItem={{label: true, className: "spec-group", name: i18n("subNodeForm.descItemType.spec.favorite")}}
            {...autocompleteAdditionalProps}
        />
    }
}

export default DescItemTypeSpec;
