/**
 * Komponenta pro zobrazení specifikace pro daný atribut.
 */
import React from 'react';
import {Autocomplete, Icon, i18n, AbstractReactComponent} from 'components/index.jsx';
import {getSetFromIdsList} from "stores/app/utils.jsx";
const classNames = require('classnames');

require("./DescItemTypeSpec.less");

const DescItemTypeSpec = class DescItemTypeSpec extends AbstractReactComponent {

    constructor(props) {
        super(props);

        // Při vstupu inicializujeme filtr položek na aktuálne vybranou položku
        const {descItem, refType, infoType} = props;
        const value = this.getValueObj(descItem, refType, infoType);
        this.state = {
            items: this.getSpecItems(refType, infoType, value ? value.name : "")
        };
    }

    componentWillReceiveProps(nextProps) {
        // Při změně hodnoty inicializujeme filtr položek na aktuálne vybranou položku
        if (this.props.descItem.descItemSpecId !== nextProps.descItemSpecId) {  // při externí změně aktualizujeme seznam položek
            const {descItem, refType, infoType} = nextProps;
            const value = this.getValueObj(descItem, refType, infoType);

            this.setState({
                items: this.getSpecItems(refType, infoType, value ? value.name : "")
            });
        }
    }

    /**
     * Hledání v položkách.
     * @param text
     */
    handleSearchChange = (text) => {
        const {refType, infoType} = this.props;
        this.setState({
            items: this.getSpecItems(refType, infoType, text)
        });
    }

    /**
     * Sestaví strom specifikací, pokud je vyžadován strom, jinak sestaví plochý seznam. Vytváří nové instance položek stromu.
     * @param refType obecná definice typu
     * @param infoType definice typu pro daný formulář
     * @param filterText filtrovací text, uvežuje se pouze v případě stromu
     */
    getSpecItems = (refType, infoType, filterText) => {
        let result;

        // Položky
        if (refType.itemSpecsTree && refType.itemSpecsTree.length > 0) {    // stromová reprezentace
            result = [];
            const treeNodeIndex = {index: 0};
            refType.itemSpecsTree.forEach(node => {
                var newNode = this.getSpecTreeNode(node, refType, infoType, filterText, treeNodeIndex);
                if (newNode) {
                    result.push(newNode);
                }
            });
        } else {    // plochý seznam
            result = infoType.specs.map(spec => ( {...refType.descItemSpecsMap[spec.id], ...spec} ));
        }

        // Oblíbené položky
        if (infoType.favoriteSpecIds && infoType.favoriteSpecIds.length > 0) {  // má oblíbené položky
            const items = [];
            infoType.favoriteSpecIds.forEach(specId => {
                const refSpec = refType.descItemSpecsMap[specId];
                if (!filterText || (filterText && refSpec.name.toLocaleLowerCase().indexOf(filterText) >= 0)) { // vypnutý filtr nebo položka vyhovuje filtru
                    const infoSpec = infoType.descItemSpecsMap[specId];
                    const value = {
                        ...refSpec,
                        ...infoSpec,
                        className: 'spec-' + infoSpec.type.toLowerCase()
                    };
                    items.push(value);
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

        return result;
    }

    /**
     * Sestavuje strom pro daný node a jeho podstrom.
     * @param node node
     * @param refType ref type
     * @param infoType info type
     * @param filterText filtr
     * @param treeNodeIndex index položky ve stromu (index plochého rozbaleného seznamu)
     * @return {*}
     */
    getSpecTreeNode = (node, refType, infoType, filterText, treeNodeIndex) => {
        // Specifikace pro daný atribut v pro konkrétní formulář
        const specChildren = [];
        const nodeSpecIdsMap = getSetFromIdsList(node.specIds);
        infoType.specs.forEach(infoSpec => {    // projdeme všechny specifikace pro daný atribut a formulář
            if (nodeSpecIdsMap[infoSpec.id]) {   // v daném nodu má být konkrétní specifikace, může pro daný atribut být v daném formuláři
                const refSpec = refType.descItemSpecsMap[infoSpec.id];

                // Filtr
                if (!filterText || (filterText && refSpec.name.toLocaleLowerCase().indexOf(filterText) >= 0)) { // vypnutý filtr nebo položka vyhovuje filtru
                    specChildren.push({
                        ...refSpec,
                        ...infoSpec,
                        className: 'spec-' + infoSpec.type.toLowerCase()
                    });
                }
            }
        });

        const lowerFilterText = filterText ? filterText.toLocaleLowerCase() : filterText;

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
            expanded: filterText ? true : false,
            children: [...children, ...specChildren]
        };
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
    }

    render() {
        const {onChange, onBlur, onFocus, descItem, locked, infoType, refType, readMode} = this.props;
        const {items} = this.state;

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
            onChange,
            onBlur,
            onFocus,
            disabled: locked
        }

        // Získání hodnoty jako objekt specifikace = autocomplete pořebuje na vstupu objekt
        const value = this.getValueObj(descItem, refType, infoType);

        // Doplňující vlastnosti v případě rozdílných typů - strom nebo list
        let autocompleteAdditionalProps;
        if (refType.itemSpecsTree && refType.itemSpecsTree.length > 0) {    // tree
            autocompleteAdditionalProps = {
                customFilter: true,
                tree: true,
                allowSelectItem: (id, item) => !item.node,
                allowFocusItem: (id, item) => !item.group,
                onSearchChange: this.handleSearchChange,
            }
        } else {    // list
            autocompleteAdditionalProps = {};
        }
        return (
            <Autocomplete
                key="spec"
                {...descItemSpecProps}
                className={cls}
                value={value}
                title={descItem.error.spec}
                items={items}
                {...autocompleteAdditionalProps}
            />
        )
    }
}

export default DescItemTypeSpec;
