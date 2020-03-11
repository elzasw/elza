/**
 * Testovací stránka pro Autocomplete.
 */
import React from 'react';
import {AbstractReactComponent, Autocomplete} from 'components/index.jsx';

export default class extends AbstractReactComponent {
    state = {
        acValue: null,
        serverItems: []
    };

    handleChange = (id, obj) => {
        console.log(id, obj);
    };

    handleAcChange = (value) => {
        this.setState({
            acValue: value
        })
    };

    render() {
        const flatItems = [];
        for (let a=0; a<100; a++) {
            flatItems.push({
                id: a,
                name: "polozka #" + a
            })
        }
        flatItems.push({
            id: 1002,
            name: "XXpolozka #"
        });
        const favoriteItems = [];
        for (let a=2; a<6; a++) {
            favoriteItems.push({
                id: a,
                name: "polozka #" + a
            })
        }
        favoriteItems.push({
            id: 1002,
            name: "XXpolozka #"
        });

        const treeItems = [];
        for (let a=0; a<50; a++) {
            const root = {
                id: a,
                name: "polozka ROOT #" + a + "r",
                children: []
            };
            treeItems.push(root);
            for (let b=0; b<10; b++) {
                const subNode = {
                    id: 1000 + a * 100 + b,
                    name: "polozka #" + a + "-" + b + "u"
                };
                root.children.push(subNode);
                if (b === 0) {
                    subNode.children = [
                        {
                            id: 20000,
                            name: "xxxxxxxxxxxxxx"
                        }
                    ]
                }
            }
        }

        const {serverItems, acValue} = this.state;

        const handleSearchChange = (text) => {
            const fc = () => {
                const list = flatItems.filter(i => {
                    return i.name.toLowerCase().indexOf(text.toLowerCase()) !== -1
                });
                this.setState({
                    serverItems: list
                });
            };

            setTimeout(fc, 300);
        };

        return (
            <div style={{maxWidth: "200px"}}>
                <h1>LOCAL - COMBOBOX</h1>
                <Autocomplete
                    value={acValue}
                    items={flatItems}
                    onChange={this.handleAcChange}
                />
                <h1>LOCAL - COMBOBOX - favorite</h1>
                <Autocomplete
                    value={acValue}
                    items={flatItems}
                    favoriteItems={favoriteItems}
                    itemsTitleItem={{label: true, name: "Polozky"}}
                    favoriteItemsTitleItem={{label: true, name: "Oblibene polozky"}}
                    onChange={this.handleAcChange}
                    allowSelectItem={(id, item) => !item.children && !item.label}
                    allowFocusItem={(id, item) => !item.label}
                />

                <h1>LOCAL - COMBOBOX TREE</h1>
                <Autocomplete
                    value={acValue}
                    items={treeItems}
                    tree
                    onChange={this.handleAcChange}
                    allowSelectItem={(id, item) => !item.children}
                />

                <h1>LOCAL - COMBOBOX TREE - favorite</h1>
                <Autocomplete
                    value={acValue}
                    items={treeItems}
                    favoriteItems={favoriteItems}
                    itemsTitleItem={{label: true, name: "Polozky"}}
                    favoriteItemsTitleItem={{label: true, name: "Oblibene polozky"}}
                    tree
                    onChange={this.handleAcChange}
                    allowSelectItem={(id, item) => !item.children && !item.label}
                    allowFocusItem={(id, item) => !item.label}
                />


                <h1>SERVER - AC</h1>
                <Autocomplete
                    value={acValue}
                    onChange={this.handleAcChange}
                    allowSelectItem={(id, item) => !item.children && !item.label}
                    allowFocusItem={(id, item) => !item.label}
                    customFilter
                    items={serverItems}
                    onSearchChange={handleSearchChange}
                />
            </div>
        )
    }
}
