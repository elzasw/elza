import './FundFilterSettings.scss';

import React from 'react';
import {AbstractReactComponent, FilterableListBox, FormInput, HorizontalLoader, i18n} from 'components/shared';
import DescItemCoordinates from './nodeForm/DescItemCoordinates.jsx';
import {Accordion, Card, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {WebApi} from 'actions/index.jsx';
import {hasDescItemTypeValue} from 'components/arr/ArrUtils.jsx';
import {createFilterStructure, FILTER_NULL_VALUE} from 'actions/arr/fundDataGrid.jsx';
import {
    normalizeDouble,
    normalizeInt,
    validateCoordinatePoint,
    validateDouble,
    validateInt,
} from 'components/validate.jsx';
import {getMapFromList} from 'stores/app/utils.jsx';
import {COL_REFERENCE_MARK} from './FundDataGridConst';
import FundNodesSelect from './FundNodesSelect';
import SimpleCheckListBox from './SimpleCheckListBox';
import FundFilterCondition from './FundFilterCondition';
import DateTimePicker from 'react-widgets/lib/DateTimePicker';
import {formatDate} from '../validate';

/**
 * Formulář nastavení filtru na sloupečku.
 */

var _ffs_validateTimer;
var _ffs_prevReject = null;

const renderTextFields = (fields) => {
    return fields.map((field, index) => {
        var decorate;
        if (field.error) {
            decorate = {
                variant: 'error',
                hasFeedback: true,
                help: field.error,
            };
        }

        return (
            <div key={index} className='value-container'>
                <FormInput {...decorate} type="text" value={field.value}
                           onChange={(e) => field.onChange(e.target.value)}/>
            </div>
        );
    });
};

const renderDateFields = (fields) => {

    return fields.map((field, index) => {
        let decorate;
        if (field.error) {
            decorate = {
                variant: 'error',
                hasFeedback: true,
                help: field.error,
            };
        }

        return (
            <div key={index} className='value-container'>
                <DateTimePicker
                    {...decorate}
                    time={false}
                    value={field.value == null ? null : new Date(field.value)}
                    onChange={(value) => field.onChange(formatDate(value))}
                />
            </div>
        );
    });
};

const renderCoordinatesFields = (fields) => {
    switch (fields.length) {
        case 0:
            return null;
        case 1:
            var descItem = {
                hasFocus: false,
                value: typeof fields[0].value !== 'undefined' ? fields[0].value : '',
                error: {value: fields[0].error},
            };
            return (
                <div key={0} className='value-container'>
                    <DescItemCoordinates
                        onChange={fields[0].onChange}
                        descItem={descItem}
                        onFocus={() => {
                        }}
                        onBlur={() => {
                        }}
                    />
                    {false && <FormInput type="text" value={fields[0].value}
                                         onChange={(e) => fields[0].onChange(e.target.value)}/>}
                </div>
            );
        case 2:
            var vals = [];
            var descItem = {
                hasFocus: false,
                value: typeof fields[0].value !== 'undefined' ? fields[0].value : '',
                error: {},
            };
            vals.push(
                <div key={0} className='value-container'>
                    <DescItemCoordinates
                        onChange={fields[0].onChange}
                        descItem={descItem}
                        onFocus={() => {
                        }}
                        onBlur={() => {
                        }}
                    />
                </div>,
            );
            vals.push(
                <div key={1} className='value-container'>
                    <FormInput as="select" defaultValue={10000} value={fields[1].value}
                               onChange={(e) => fields[1].onChange(e.target.value)}>
                        {[100, 500, 1000, 10000, 20000, 50000, 100000].map(l => {
                            return <option key={l}
                                           value={l}>{i18n('arr.fund.filterSettings.condition.coordinates.near.' + l)}</option>;
                        })}
                    </FormInput>
                </div>,
            );
            return vals;
        default:
            return null;
    }
};

const renderUnitdateFields = (calendarTypes, fields) => {
    switch (fields.length) {
        case 0:
            return null;
        case 2:
            var vals = [];
            vals.push(
                <div key={0} className='value-container'>
                    <FormInput as="select"
                               value={typeof fields[0].value !== 'undefined' ? fields[0].value : 1}
                               onChange={(e) => {
                                   fields[0].onChange(e.target.value);
                               }}
                    >
                        {calendarTypes.items.map(calendarType => (
                            <option key={calendarType.id} value={calendarType.id}>{calendarType.name}</option>
                        ))}
                    </FormInput>
                </div>,
            );
            vals.push(
                <div key={1} className='value-container'>
                    <FormInput type="text"
                               value={fields[1].value}
                               onChange={(e) => {
                                   fields[1].onChange(e.target.value);
                               }}
                    />
                </div>,
            );
            return vals;
    }
};

const FundFilterSettings = class FundFilterSettings extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callValueSearch', 'handleValueSearch',
            'handleValueItemsChange', 'renderConditionFilter', 'handleSpecItemsChange', 'handleConditionChange',
            'handleSubmit', 'renderValueFilter', 'getConditionInfo');

        let state = {
            valueItems: [],
            valueSearchText: '',
            selectedValueItems: [],
            selectedValueItemsType: 'unselected',
            selectedSpecItems: [],
            selectedSpecItemsType: 'unselected',
            conditionSelectedCode: 'NONE',
            conditionValues: [],
            conditionHasErrors: false,
            refMarkSelectedNode: null,
            specItems: [],
            isFetchingSpecIds: false,
            isFetchingItemTypeValues: false,
        };

        const {filter} = props;
        if (typeof filter !== 'undefined' && filter) {
            state.selectedValueItems = filter.values;
            state.selectedValueItemsType = filter.valuesType;
            state.selectedSpecItems = filter.specs;
            state.selectedSpecItemsType = filter.specsType;
            state.conditionSelectedCode = filter.conditionType;
            state.conditionValues = filter.condition;
        }

        // Určení typu uplatněného filtru - podmínka nebo hodnota
        // pokud je vybrana podminka, tak ma prednost
        state.valueAccodrionType = 'VALUE';
        const condInfo = this.getConditionInfo();
        // pokud existuji podminky muze byt rizeno podminkou
        if (condInfo.items.length > 0) {
            // ? je vybrana podminka
            const conditionHasValue = state.conditionSelectedCode !== 'NONE';
            const valueHasValue = state.selectedValueItemsType === 'selected' || (state.selectedValueItems && state.selectedValueItems.length > 0);
            if (conditionHasValue || !valueHasValue) {
                state.valueAccodrionType = 'CONDITION';
            }
        }

        this.state = state;
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        const {dataType, refType} = this.props;
        if (refType.id !== COL_REFERENCE_MARK) {
            if (refType.useSpecification) {  // má specifikace, nebo u obalů budeme místo specifikací zobrazovat výběr typů obsalů
                this.callFilterUniqueSpecs();   // v metodě se dále volá value search - až po načtení specifikací
            } else {
                this.callValueSearch('');   // zde musíme volat value search ručně, protože se nenačítají specifikace
            }
        }
    }

    handleValueSearch(text) {
        this.setState({
            valueSearchText: text,
        }, this.callValueSearch);
    }

    callFilterUniqueSpecs = () => {
        const {versionId, refType, dataType} = this.props;

        this.setState({isFetchingSpecIds: true});
        WebApi.findUniqueSpecIds(versionId, refType.id, createFilterStructure(this.props.filters)).then(specIds => {
            let specItems = [];

            if (specIds.indexOf(null) >= 0) {
                specItems.push({
                    id: FILTER_NULL_VALUE,
                    name: i18n('arr.fund.filterSettings.value.empty'),
                });
            }

            refType.descItemSpecs.forEach((spec) => {
                if (specIds.indexOf(spec.id) >= 0) {
                    specItems.push(spec);
                }
            });


            this.setState({specItems, isFetchingSpecIds: false}, () => this.callValueSearch(''));
        });
    };

    callValueSearch() {
        const {versionId, refType, dataType} = this.props;
        const {valueSearchText} = this.state;

        if (!hasDescItemTypeValue(dataType)) {  // pokud nemá hodnotu, nemůžeme volat
            return;
        }

        var specIds = [];
        if (refType.useSpecification) {
            specIds = this.refs.specsListBox.getSpecsIds();

            if (specIds.length === 0) { // pokud nemá nic vybráno, nevrátily by se žádné položky a není třeba volat server
                this.setState({
                    valueItems: [],
                });
                return;
            }
        }

        if (dataType.code !== 'UNITDATE' && dataType.code !== 'TEXT' && dataType.code !== 'COORDINATES') {
            // Ladění objektu pro server
            var useSpecIds = specIds.map(id => {
                return id === FILTER_NULL_VALUE ? null : id;
            });

            this.setState({isFetchingItemTypeValues: true});
            WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, useSpecIds, 200)
                  .then(json => {
                      var valueItems = json.map(i => ({id: i, name: i}));

                      // TODO [stanekpa] Toto zde nebude, když se na server přidělá podpora na vracení a hledání NULL hodnot - problé je ale v locales (řetězec arr.fund.filterSettings.value.empty), měly by se doplnit i na server
                      if (valueSearchText == '' || i18n('arr.fund.filterSettings.value.empty').toLowerCase().indexOf(valueSearchText) !== -1) {   // u prázdného hledání a případně u hledání prázdné hodnoty doplňujeme null položku
                          valueItems = [{
                              id: FILTER_NULL_VALUE,
                              name: i18n('arr.fund.filterSettings.value.empty'),
                          }, ...valueItems];
                      }

                      this.setState({
                          valueItems: valueItems,
                          isFetchingItemTypeValues: false,
                      });
                  });
        }
    }

    handleSpecItemsChange(data) {
        const {type, ids} = data;
        this.setState({
            selectedSpecItems: ids,
            selectedSpecItemsType: type,
        }, this.callValueSearch);
    }

    handleValueItemsChange(type, ids) {
        const {selectedValueItems, selectedValueItemsType, valueSearchText, valueItems} = this.state;
        const filtered = valueSearchText !== '';
        const prevType = selectedValueItemsType;
        //console.warn('input', type, ids, selectedValueItems, prevType, filtered, valueItems);

        const valueItemsMap = valueItems.map(item => item.id);

        let resultValueItems = [];
        if (filtered) {
            resultValueItems = selectedValueItems.filter(item => valueItemsMap.indexOf(item) === -1);
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

        this.setState({
            selectedValueItems: resultValueItems,
            selectedValueItemsType: type,
        });
    }

    handleConditionChange(selectedCode, values, hasErrors) {
        const {dataType} = this.props;
        var useValues = [...values];

        // Inicializace implicitních hodnot, musí být i u input prvků v render metodě
        switch (dataType.code) {
            case 'TEXT':
            case 'STRING':
            case 'FORMATTED_TEXT':
            case 'UNITID':
            case 'INT':
            case 'DATE':
            case 'DECIMAL':
            case 'PARTY_REF':
            case 'STRUCTURED':
            case 'JSON_TABLE':
            case 'ENUM':
            case 'RECORD_REF':
                break;
            case 'UNITDATE':
                if (useValues.length > 0) {
                    if (!useValues[0]) {
                        useValues[0] = 1;
                    }
                    if (!useValues[1]) {
                        useValues[1] = '';
                    }
                }
                break;
            case 'COORDINATES':
                if (selectedCode === 'NEAR' && !useValues[1]) {
                    useValues[1] = 10000;
                }
                break;
        }

        this.setState({
            conditionSelectedCode: selectedCode,
            conditionValues: useValues,
            conditionHasErrors: hasErrors,
        });
    }

    renderValueFilter() {
        const {dataType} = this.props;
        const {isFetchingItemTypeValues, valueItems, selectedValueItems, selectedValueItemsType, conditionSelectedCode} = this.state;

        if (!hasDescItemTypeValue(dataType)) {
            return null;
        }

        if (dataType.code === 'UNITDATE' || dataType.code === 'TEXT' || dataType.code === 'COORDINATES') { // zde je výjimka a nechceme dle hodnoty
            return null;
        }

        return (
            <FilterableListBox
                className='filter-content-container'
                searchable
                items={valueItems}
                selectionType={selectedValueItemsType}
                selectedIds={selectedValueItems}
                onChange={this.handleValueItemsChange}
                onSearch={this.handleValueSearch}
            >
                {isFetchingItemTypeValues && <HorizontalLoader hover showText={false}/>}
            </FilterableListBox>
        );
    }

    getConditionInfo() {
        const {dataType, calendarTypes} = this.props;

        let renderFields;
        let validateField;
        let normalizeField;
        var items = [];
        if (dataType) {
            switch (dataType.code) {
                case 'TEXT':
                case 'STRING':
                case 'FORMATTED_TEXT':
                case 'UNITID':
                    renderFields = renderTextFields;
                    validateField = (code, valuesCount, value, index) => {
                        return value ? null : i18n('global.validation.required');
                    };
                    items = [
                        {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                        {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                        {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                        {values: 0, code: 'UNDEFINED', name: i18n('arr.fund.filterSettings.condition.undefined')},
                        {values: 1, code: 'CONTAIN', name: i18n('arr.fund.filterSettings.condition.string.contain')},
                        {
                            values: 1,
                            code: 'NOT_CONTAIN',
                            name: i18n('arr.fund.filterSettings.condition.string.notContain'),
                        },
                        {values: 1, code: 'BEGIN', name: i18n('arr.fund.filterSettings.condition.begin')},
                        {values: 1, code: 'END', name: i18n('arr.fund.filterSettings.condition.end')},
                        {values: 1, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                    ];
                    break;
                case 'INT':
                case 'DECIMAL':
                    renderFields = renderTextFields;
                    normalizeField = (code, valuesCount, value, index) => {
                        return dataType.code === 'INT' ? normalizeInt(value) : normalizeDouble(value);
                    };
                    validateField = (code, valuesCount, value, index) => {
                        if (!value) return i18n('global.validation.required');
                        return dataType.code === 'INT' ? validateInt(value) : validateDouble(value);
                    };
                    items = [
                        {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                        {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                        {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                        {values: 0, code: 'UNDEFINED', name: i18n('arr.fund.filterSettings.condition.undefined')},
                        {values: 1, code: 'GT', name: i18n('arr.fund.filterSettings.condition.gt')},
                        {values: 1, code: 'GE', name: i18n('arr.fund.filterSettings.condition.ge')},
                        {values: 1, code: 'LT', name: i18n('arr.fund.filterSettings.condition.lt')},
                        {values: 1, code: 'LE', name: i18n('arr.fund.filterSettings.condition.le')},
                        {values: 1, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                        {values: 1, code: 'NE', name: i18n('arr.fund.filterSettings.condition.ne')},
                        {values: 2, code: 'INTERVAL', name: i18n('arr.fund.filterSettings.condition.interval')},
                        {values: 2, code: 'NOT_INTERVAL', name: i18n('arr.fund.filterSettings.condition.notInterval')},
                    ];
                    break;
                case 'DATE':
                    renderFields = renderDateFields;
                    normalizeField = (code, valuesCount, value, index) => {
                        return value;
                    };
                    validateField = (code, valuesCount, value, index) => {
                        if (!value) return i18n('global.validation.required');
                    };
                    items = [
                        {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                        {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                        {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                        {values: 0, code: 'UNDEFINED', name: i18n('arr.fund.filterSettings.condition.undefined')},
                        {values: 1, code: 'GT', name: i18n('arr.fund.filterSettings.condition.gt')},
                        {values: 1, code: 'GE', name: i18n('arr.fund.filterSettings.condition.ge')},
                        {values: 1, code: 'LT', name: i18n('arr.fund.filterSettings.condition.lt')},
                        {values: 1, code: 'LE', name: i18n('arr.fund.filterSettings.condition.le')},
                        {values: 1, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                        {values: 1, code: 'NE', name: i18n('arr.fund.filterSettings.condition.ne')},
                        {values: 2, code: 'INTERVAL', name: i18n('arr.fund.filterSettings.condition.interval')},
                        {values: 2, code: 'NOT_INTERVAL', name: i18n('arr.fund.filterSettings.condition.notInterval')},
                    ];
                    break;
                case 'PARTY_REF':
                case 'RECORD_REF':
                    renderFields = renderTextFields;
                    validateField = (code, valuesCount, value, index) => {
                        return value ? null : i18n('global.validation.required');
                    };
                    items = [
                        {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                        {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                        {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                        {values: 0, code: 'UNDEFINED', name: i18n('arr.fund.filterSettings.condition.undefined')},
                        {values: 1, code: 'CONTAIN', name: i18n('arr.fund.filterSettings.condition.string.contain')},
                    ];
                    break;
                case 'UNITDATE':
                    renderFields = renderUnitdateFields.bind(this, calendarTypes);
                    validateField = (code, valuesCount, value, index) => {
                        return new Promise(function(resolve, reject) {
                            if (_ffs_validateTimer) {
                                clearTimeout(_ffs_validateTimer);
                                if (_ffs_prevReject) {
                                    _ffs_prevReject();
                                    _ffs_prevReject = null;
                                }
                            }
                            _ffs_prevReject = reject;
                            var fc = () => {
                                WebApi.validateUnitdate(value)
                                      .then(json => {
                                          resolve(json.message);
                                      });
                            };
                            _ffs_validateTimer = setTimeout(fc, 250);
                        });
                    };
                    items = [
                        {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                        {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                        {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                        {values: 0, code: 'UNDEFINED', name: i18n('arr.fund.filterSettings.condition.undefined')},
                        {values: 2, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                        {values: 2, code: 'LT', name: i18n('arr.fund.filterSettings.condition.unitdate.lt')},
                        {values: 2, code: 'GT', name: i18n('arr.fund.filterSettings.condition.unitdate.gt')},
                        {values: 2, code: 'SUBSET', name: i18n('arr.fund.filterSettings.condition.unitdate.subset')},
                        {
                            values: 2,
                            code: 'INTERSECT',
                            name: i18n('arr.fund.filterSettings.condition.unitdate.intersect'),
                        },
                    ];
                    break;
                case 'COORDINATES':
                    renderFields = renderCoordinatesFields;
                    validateField = (code, valuesCount, value, index) => {
                        return validateCoordinatePoint(value);
                    };
                    items = [
                        {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                        {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                        {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                        {values: 0, code: 'UNDEFINED', name: i18n('arr.fund.filterSettings.condition.undefined')},
                        {values: 1, code: 'SUBSET', name: i18n('arr.fund.filterSettings.condition.coordinates.subset')},
                        {values: 2, code: 'NEAR', name: i18n('arr.fund.filterSettings.condition.coordinates.near')},
                    ];
                    break;
                case 'JSON_TABLE':
                case 'ENUM':
                    break;
            }
        }

        return {
            renderFields,
            validateField,
            normalizeField,
            items,
        };
    }

    renderConditionFilter() {
        const {dataType} = this.props;
        const {conditionSelectedCode, conditionValues} = this.state;

        if (!hasDescItemTypeValue(dataType)) {
            return null;
        }

        const info = this.getConditionInfo();
        // no conditions -> do not render
        if (info.items.length === 0) {
            return null;
        }

        return (
            <FundFilterCondition
                className='filter-content-container'
                selectedCode={conditionSelectedCode}
                values={conditionValues}
                onChange={this.handleConditionChange}
                items={info.items}
                renderFields={info.renderFields}
                validateField={info.validateField}
                normalizeField={info.normalizeField}
            />
        );
    }

    handleRefMarkSubmit = () => {
        const {onSubmitForm} = this.props;
        const {refMarkSelectedNode} = this.state;
        const data = {
            nodeId: refMarkSelectedNode.id,
        };

        onSubmitForm(data);
    };

    handleClearSubmit = () => {
        const {onSubmitForm} = this.props;
        onSubmitForm(null);
    };

    handleSubmit() {
        const {selectedValueItems, selectedValueItemsType, valueAccodrionType, selectedSpecItems, selectedSpecItemsType, conditionSelectedCode, conditionValues} = this.state;
        const {onSubmitForm, refType, dataType} = this.props;

        var data = {
            values: selectedValueItems,
            valuesType: selectedValueItemsType,
            condition: conditionValues,
            conditionType: conditionSelectedCode,
        };

        if (refType.useSpecification) {
            data.specs = selectedSpecItems;
            data.specsType = selectedSpecItemsType;
        }

        // Filtrování podle podmínky a hodnoty - jsou výlučné a jedno musí být zrušeno - dle valueAccodrionType
        switch (valueAccodrionType) {
            case 'CONDITION':
                data.valuesType = 'unselected';
                data.values = [];
                break;
            case 'VALUE':
                data.conditionType = 'NONE';
                data.condition = null;
                break;
        }

        // ##
        // # Test, zda není filtr prázdný
        // ##
        var outData = null;

        if (data.valuesType === 'selected' || data.values.length > 0) {      // je zadáno filtrování podle hodnoty
            outData = data;
        } else if ((refType.useSpecification) && (data.specsType === 'selected' || data.specs.length > 0)) {     // je zadáno filtrování podle specifikace
            outData = data;
        } else if (data.conditionType !== 'NONE') { // je zadáno filtrování podle podmínky
            outData = data;
        }

        onSubmitForm(outData);
    }

    handleNodeSelectChange = (ids, nodes) => {
        this.setState({
            refMarkSelectedNode: nodes.length > 0 ? nodes[0] : null,
        });
    };

    render() {
        const {filter, refType, onClose, dataType} = this.props;
        const {isFetchingSpecIds, refMarkSelectedNode, conditionHasErrors, valueAccodrionType, conditionSelectedCode, conditionValues, selectedValueItems, selectedSpecItems, selectedSpecItemsType, specItems} = this.state;

        var specContent = null;
        if (refType.id === COL_REFERENCE_MARK) {
            // nemá specifikaci
        } else if (refType.useSpecification) {
            specContent = (
                <SimpleCheckListBox
                    ref='specsListBox'
                    items={specItems}
                    label={i18n('arr.fund.filterSettings.filterBySpecification.title')}
                    value={{type: selectedSpecItemsType, ids: selectedSpecItems}}
                    onChange={this.handleSpecItemsChange}
                >
                    {isFetchingSpecIds && <HorizontalLoader hover showText={false}/>}
                </SimpleCheckListBox>
            );
        }

        var valueContent;
        var conditionContent;
        var hasAllValues = true;

        let okButtons = [<Button key="clear" className="pull-left"
                                 onClick={this.handleClearSubmit}>{i18n('arr.fund.filterSettings.action.clear')}</Button>];
        if (refType.id !== COL_REFERENCE_MARK) {
            valueContent = this.renderValueFilter();

            conditionContent = this.renderConditionFilter();

            if (hasDescItemTypeValue(dataType)) {
                const info = this.getConditionInfo();
                if (info.items.length > 0) {
                    const itemsCodeMap = getMapFromList(info.items, 'code');
                    const selectedItem = itemsCodeMap[conditionSelectedCode];

                    for (var a = 0; a < selectedItem.values; a++) {
                        if (!conditionValues[a]) {
                            hasAllValues = false;
                        }
                    }
                }
            }

            const okDisabled = conditionHasErrors || !hasAllValues;
            okButtons.push(<Button key="store" disabled={okDisabled}
                                   onClick={this.handleSubmit}>{i18n('global.action.store')}</Button>);
        } else {    // referenční označení
            valueContent = <FundNodesSelect
                selectedId={filter && filter.nodeId != null ? filter.nodeId : null}
                multipleSelection={false}
                onChange={this.handleNodeSelectChange}
            />;

            const okDisabled = refMarkSelectedNode === null;
            okButtons.push(<Button key="store" disabled={okDisabled}
                                   onClick={this.handleRefMarkSubmit}>{i18n('global.action.select')}</Button>);
        }

        let accordion;
        if (conditionContent && valueContent) {
            accordion =
                <Accordion className="accordion-simple bordered" activeKey={valueAccodrionType} onSelect={type => {
                    this.setState({valueAccodrionType: type});
                }}>
                    <Card className={valueAccodrionType === 'CONDITION' ? 'open' : ''}>
                        <Card.Header>
                            <h4>{i18n('arr.fund.filterSettings.filterByCondition.title')}</h4>
                        </Card.Header>
                        <Accordion.Collapse eventKey="CONDITION">
                            <Card.Body>
                                {conditionContent}
                            </Card.Body>
                        </Accordion.Collapse>
                    </Card>
                    <Card className={valueAccodrionType === 'VALUE' ? 'open' : ''}>
                        <Card.Header>
                            <h4>{i18n('arr.fund.filterSettings.filterByValue.title')}</h4>
                        </Card.Header>
                        <Accordion.Collapse eventKey="VALUE">
                            <Card.Body>
                                {valueContent}
                            </Card.Body>
                        </Accordion.Collapse>
                    </Card>
                </Accordion>;
        } else {
            accordion = <div>
                {conditionContent && <div>
                    <h4>{i18n('arr.fund.filterSettings.filterByCondition.title')}</h4>
                    {conditionContent}
                </div>}
                {valueContent && <div>
                    <h4>{i18n('arr.fund.filterSettings.filterByValue.title')}</h4>
                    {valueContent}
                </div>}
            </div>;
        }

        return (
            <div className='fund-filter-settings-container'>
                <Modal.Body>
                    <div className='filters-container'>
                        {specContent}
                        {accordion}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    {okButtons}
                    <Button variant="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        );
    }
};

export default FundFilterSettings;
