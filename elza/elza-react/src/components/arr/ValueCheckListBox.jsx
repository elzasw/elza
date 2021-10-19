/**
 *  ListBox komponenta s možností filtrování a hledání a vybrání položek.
 *
 **/

import React from 'react';
import {AbstractReactComponent, FilterableListBox, i18n, HorizontalLoader} from 'components/shared';
import {WebApi} from 'actions/index';

class ValueCheckListBox extends AbstractReactComponent {

    constructor(props) {
        super(props);

        this.bindMethods(
            'callValueSearch',
            'handleValueSearch',
            'handleValueItemsChange',
        );

        this.state = {
            valueItems: [],
            valueSearchText: '',
            isFetchingItemTypeValues: false,
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {}

    componentDidMount() {
        this.callValueSearch('');
    }

    handleValueItemsChange(type, ids) {
        const {value} = this.props;
        const {valueSearchText, valueItems} = this.state;
        const filtered = valueSearchText !== '';
        const prevType = value.type;

        const valueItemsMap = valueItems.map(item => item.id);

        let resultValueItems = [];
        if (filtered) {
            resultValueItems = value.ids.filter(item => valueItemsMap.indexOf(item) === -1);
            if (prevType === 'selected') {
                if (type === 'selected') {
                    resultValueItems.push(...ids);
                } else {
                    valueItemsMap.forEach(item => resultValueItems.push(item));
                    ids.forEach(item => {
                        if (resultValueItems.indexOf(item) === -1) {
                            resultValueItems.push(item);
                        }
                    });
                    type = 'selected';
                }
            } else {
                if (type === 'selected') {
                    valueItemsMap.forEach(item => resultValueItems.push(item));
                    ids.forEach(item => {
                        if (resultValueItems.indexOf(item) === -1) {
                            resultValueItems.push(item);
                        }
                    });
                    type = 'unselected';
                } else {
                    resultValueItems.push(...ids);
                }
            }
        } else {
            resultValueItems = ids;
        }

        this.props.onChange({type, ids: resultValueItems});
    }

    handleValueSearch(text) {
        this.setState(
            {
                valueSearchText: text,
            },
            this.callValueSearch,
        );
    }

    callValueSearch() {
        const {versionId, refType} = this.props;
        const {valueSearchText} = this.state;

        this.setState({isFetchingItemTypeValues: true});
        WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, null, 200).then(json => {
            var valueItems = json.map(i => ({id: i.id, name: i.value}));

            if (
                valueSearchText === '' ||
                i18n('arr.fund.filterSettings.value.empty').toLowerCase().indexOf(valueSearchText) !== -1
            ) {

                this.setState({
                    allValueItems: valueItems,
                });
                // u prázdného hledání a případně u hledání prázdné hodnoty doplňujeme null položku
                valueItems = [
                    {
                        id: -1,
                        name: i18n('arr.fund.filterSettings.value.empty'),
                    },
                    ...valueItems,
                ];
            }

            this.setState({
                valueItems: valueItems,
                isFetchingItemTypeValues: false,
            });
        });
    }

    getValue = () => {
        let {value} = this.props;
        if (typeof value === 'undefined') {
            value = {type: 'unselected', ids: []};
        }

        return {
            type: value.type || 'unselected',
            ids: value.ids || [],
        };
    };

    render() {
        const {isFetchingItemTypeValues, valueItems} = this.state;

        const value = this.getValue();

        return (
            <FilterableListBox
                className="filter-content-container"
                searchable
                items={valueItems}
                selectionType={value.type}
                selectedIds={value.ids}
                onChange={this.handleValueItemsChange}
                onSearch={this.handleValueSearch}
            >
                {isFetchingItemTypeValues && <HorizontalLoader hover showText={false} />}
            </FilterableListBox>
        );
    }
}

export default ValueCheckListBox;
