import './FundFilterSettings.scss';

import React from 'react';
import {AbstractReactComponent, FilterableListBox, FormInput, HorizontalLoader} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { nodeListMessages } from './nodeListMessages';
import { arrMessages } from './messages';
import { coordinatesNearMessages } from './messages';
import { messageFor } from 'components/shared/lang/dynamicMessage';
import DescItemCoordinates from './nodeForm/DescItemCoordinates';
import {Accordion, Card, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {WebApi} from 'actions/index';
import {hasDescItemTypeValue} from 'components/arr/ArrUtils';
import {createFilterStructure, FILTER_NULL_VALUE} from 'actions/arr/fundDataGrid';
import {
    normalizeDouble,
    normalizeInt,
    validateCoordinatePoint,
    validateDouble,
    validateInt,
} from 'components/validate';
import {getMapFromList} from 'stores/app/utils';
import {COL_REFERENCE_MARK} from './FundDataGridConst';
import FundNodesSelect from './FundNodesSelect';
import SimpleCheckListBox from './SimpleCheckListBox';
import FundFilterCondition from './FundFilterCondition';
import {DateTimePicker} from 'react-widgets';
import {formatDateIso} from '../validate';
import {validateUnitDate} from '../registry/field/UnitdateField';

/**
 * Pole pro výběr entity (archivní entita) - používá se pro podmínku CONTAIN_ENTITY.
 */
class EntityField extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            query: '',
            accessPoints: [],
            selectedName: '',
        };
        this.searchTimeout = null;
    }

    componentDidMount() {
        // Pokud je již nastavena hodnota (accessPointId), načteme název entity
        if (this.props.value) {
            WebApi.getAccessPoint(this.props.value).then(ap => {
                this.setState({selectedName: ap.name});
            });
        } else {
            this.fetchAccessPoints('');
        }
    }

    fetchAccessPoints(search) {
        const {versionId} = this.props;
        WebApi.findAccessPoint(search, null, null, versionId, null, null, 0, 50)
            .then(result => {
                this.setState({accessPoints: result.rows || []});
            });
    }

    handleQueryChange = (e) => {
        const query = e.target.value;
        this.setState({query, selectedName: ''});
        if (this.searchTimeout) {
            clearTimeout(this.searchTimeout);
        }
        this.searchTimeout = setTimeout(() => {
            this.fetchAccessPoints(query);
        }, 300);
        // Smazání vybrané entity
        this.props.onChange(null);
    };

    handleSelect = (accessPoint) => {
        this.setState({
            selectedName: accessPoint.name,
            query: '',
            accessPoints: [],
        });
        this.props.onChange('' + accessPoint.id);
    };

    render() {
        const {query, accessPoints, selectedName} = this.state;
        const {error} = this.props;

        let decorate = {};
        if (error) {
            decorate = {
                variant: 'error',
                hasFeedback: true,
                help: error,
            };
        }

        return (
            <div className="value-container entity-field-container">
                <FormInput
                    {...decorate}
                    type="text"
                    value={selectedName || query}
                    onChange={this.handleQueryChange}
                    placeholder={<FormattedMessage {...arrMessages.fundFilterSettingsConditionContainEntity} />}
                />
                {!selectedName && accessPoints.length > 0 && (
                    <div className="entity-autocomplete-list">
                        {accessPoints.map(ap => (
                            <div
                                key={ap.id}
                                className="entity-autocomplete-item"
                                onMouseDown={() => this.handleSelect(ap)}
                            >
                                <div>{ap.name}</div>
                                {ap.description && <div className="entity-description"><small>{ap.description}</small></div>}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }
}

/**
 * Formulář nastavení filtru na sloupečku.
 */
const renderTextFields = fields => {
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
            <div key={index} className="value-container">
                <FormInput
                    {...decorate}
                    type="text"
                    value={field.value}
                    onChange={e => field.onChange(e.target.value)}
                />
            </div>
        );
    });
};

const renderDateFields = fields => {
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
            <div key={index} className="value-container">
                <DateTimePicker
                    {...decorate}
                    time={false}
                    value={field.value == null ? null : new Date(field.value)}
                    onChange={value => field.onChange(formatDateIso(value))}
                />
            </div>
        );
    });
};

const renderCoordinatesFields = fields => {
    let descItem;
    switch (fields.length) {
        case 0:
            return null;
        case 1:
            descItem = {
                hasFocus: false,
                value: typeof fields[0].value !== 'undefined' ? fields[0].value : '',
                error: {value: fields[0].error},
            };
            return (
                <div key={0} className="value-container">
                    <DescItemCoordinates
                        onChange={fields[0].onChange}
                        descItem={descItem}
                        onFocus={() => {}}
                        onBlur={() => {}}
                    />
                    {false && (
                        <FormInput
                            type="text"
                            value={fields[0].value}
                            onChange={e => fields[0].onChange(e.target.value)}
                        />
                    )}
                </div>
            );
        case 2:
            let vals = [];
            descItem = {
                hasFocus: false,
                value: typeof fields[0].value !== 'undefined' ? fields[0].value : '',
                error: {},
            };
            vals.push(
                <div key={0} className="value-container">
                    <DescItemCoordinates
                        onChange={fields[0].onChange}
                        descItem={descItem}
                        onFocus={() => {}}
                        onBlur={() => {}}
                    />
                </div>,
            );
            vals.push(
                <div key={1} className="value-container">
                    <FormInput
                        type="select"
                        defaultValue={10000}
                        value={fields[1].value}
                        onChange={e => fields[1].onChange(e.target.value)}
                    >
                        {[100, 500, 1000, 10000, 20000, 50000, 100000].map(l => {
                            return (
                                <option key={l} value={l}>
                                    {this.props.intl.formatMessage(messageFor(coordinatesNearMessages, 'm' + l, coordinatesNearMessages.m1000))}
                                </option>
                            );
                        })}
                    </FormInput>
                </div>,
            );
            return vals;
        default:
            return null;
    }
};

const FundFilterSettings = class FundFilterSettings extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'callValueSearch',
            'handleValueSearch',
            'handleValueItemsChange',
            'renderConditionFilter',
            'handleSpecItemsChange',
            'handleConditionChange',
            'handleSubmit',
            'renderValueFilter',
            'getConditionInfo',
        );

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
            const valueHasValue =
                state.selectedValueItemsType === 'selected' ||
                (state.selectedValueItems && state.selectedValueItems.length > 0);
            if (conditionHasValue || !valueHasValue) {
                state.valueAccodrionType = 'CONDITION';
            }
        }

        this.state = state;
    }

    UNSAFE_componentWillReceiveProps(nextProps) {}

    componentDidMount() {
        const {refType} = this.props;
        if (refType.id !== COL_REFERENCE_MARK) {
            if (refType.useSpecification) {
                // má specifikace, nebo u obalů budeme místo specifikací zobrazovat výběr typů obsalů
                this.callFilterUniqueSpecs(); // v metodě se dále volá value search - až po načtení specifikací
            } else {
                this.callValueSearch(''); // zde musíme volat value search ručně, protože se nenačítají specifikace
            }
        }
    }

    handleValueSearch(text) {
        this.setState(
            {
                valueSearchText: text,
            },
            this.callValueSearch,
        );
    }

    callFilterUniqueSpecs = () => {
        const {versionId, refType} = this.props;

        this.setState({isFetchingSpecIds: true});
        WebApi.findUniqueSpecIds(versionId, refType.id, createFilterStructure(this.props.filters)).then(specIds => {
            let specItems = [];

            if (specIds.indexOf(null) >= 0) {
                specItems.push({
                    id: FILTER_NULL_VALUE,
                    name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsValueEmpty),
                });
            }

            refType.descItemSpecs.forEach(spec => {
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

        if (!hasDescItemTypeValue(dataType)) {
            // pokud nemá hodnotu, nemůžeme volat
            return;
        }

        var specIds = [];
        if (refType.useSpecification) {
            specIds = this.refs.specsListBox.getSpecsIds();

            if (specIds.length === 0) {
                // pokud nemá nic vybráno, nevrátily by se žádné položky a není třeba volat server
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
            WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, useSpecIds, 200).then(json => {
                var valueItems = json.map(i => ({id: i.value, name: i.value}));

                // TODO [stanekpa] Toto zde nebude, když se na server přidělá podpora na vracení a hledání NULL hodnot - problé je ale v locales (řetězec arr.fund.filterSettings.value.empty), měly by se doplnit i na server
                if (
                    valueSearchText == '' ||
                    this.props.intl.formatMessage(arrMessages.fundFilterSettingsValueEmpty).toLowerCase().indexOf(valueSearchText) !== -1
                ) {
                    // u prázdného hledání a případně u hledání prázdné hodnoty doplňujeme null položku
                    valueItems = [
                        {
                            id: FILTER_NULL_VALUE,
                            name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsValueEmpty),
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
    }

    handleSpecItemsChange(data) {
        const {type, ids} = data;
        this.setState(
            {
                selectedSpecItems: ids,
                selectedSpecItemsType: type,
            },
            this.callValueSearch,
        );
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
            case 'STRUCTURED':
            case 'JSON_TABLE':
            case 'ENUM':
            case 'RECORD_REF': {
                const prevCode = this.state.conditionSelectedCode;
                if ((prevCode === 'CONTAIN' && selectedCode === 'CONTAIN_ENTITY') ||
                    (prevCode === 'CONTAIN_ENTITY' && selectedCode === 'CONTAIN')) {
                    useValues = [];
                    hasErrors = false;
                }
                break;
            }
            case 'UNITDATE':
                /*
                if (useValues.length > 0) {
                    if (!useValues[0]) {
                        useValues[0] = 1;
                    }
                    if (!useValues[1]) {
                        useValues[1] = '';
                    }
                }*/
                break;
            case 'COORDINATES':
                if (selectedCode === 'NEAR' && !useValues[1]) {
                    useValues[1] = 10000;
                }
                break;
            default:
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
        const {isFetchingItemTypeValues, valueItems, selectedValueItems, selectedValueItemsType} = this.state;

        if (!hasDescItemTypeValue(dataType)) {
            return null;
        }

        if (dataType.code === 'UNITDATE' || dataType.code === 'TEXT' || dataType.code === 'COORDINATES' || dataType.code === 'RECORD_REF') {
            // zde je výjimka a nechceme dle hodnoty
            return null;
        }

        return (
            <FilterableListBox
                className="filter-content-container"
                searchable
                items={valueItems}
                selectionType={selectedValueItemsType}
                selectedIds={selectedValueItems}
                onChange={this.handleValueItemsChange}
                onSearch={this.handleValueSearch}
            >
                {isFetchingItemTypeValues && <HorizontalLoader hover showText={false} />}
            </FilterableListBox>
        );
    }

    getConditionInfo() {
        const {dataType} = this.props;

        let renderFields;
        let validateField;
        let normalizeField;
        let items = [];
        if (dataType) {
            switch (dataType.code) {
                case 'TEXT':
                case 'STRING':
                case 'FORMATTED_TEXT':
                case 'UNITID':
                    renderFields = renderTextFields;
                    validateField = (code, valuesCount, value, index) => {
                        return value ? null : this.props.intl.formatMessage(globalMessages.validationRequired);
                    };
                    items = [
                        {values: 0, code: 'NONE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNone)},
                        {values: 0, code: 'EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEmpty)},
                        {values: 0, code: 'NOT_EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotEmpty)},
                        {values: 0, code: 'UNDEFINED', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUndefined)},
                        {values: 1, code: 'CONTAIN', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionStringContain)},
                        {
                            values: 1,
                            code: 'NOT_CONTAIN',
                            name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionStringNotContain),
                        },
                        {values: 1, code: 'BEGIN', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionBegin)},
                        {values: 1, code: 'END', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEnd)},
                        {values: 1, code: 'EQ', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEq)},
                    ];
                    break;
                case 'INT':
                case 'DECIMAL':
                    renderFields = renderTextFields;
                    normalizeField = (code, valuesCount, value, index) => {
                        return dataType.code === 'INT' ? normalizeInt(value) : normalizeDouble(value);
                    };
                    validateField = (code, valuesCount, value, index) => {
                        if (!value) return this.props.intl.formatMessage(globalMessages.validationRequired);
                        return dataType.code === 'INT' ? validateInt(value) : validateDouble(value);
                    };
                    items = [
                        {values: 0, code: 'NONE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNone)},
                        {values: 0, code: 'EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEmpty)},
                        {values: 0, code: 'NOT_EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotEmpty)},
                        {values: 0, code: 'UNDEFINED', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUndefined)},
                        {values: 1, code: 'GT', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionGt)},
                        {values: 1, code: 'GE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionGe)},
                        {values: 1, code: 'LT', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionLt)},
                        {values: 1, code: 'LE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionLe)},
                        {values: 1, code: 'EQ', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEq)},
                        {values: 1, code: 'NE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNe)},
                        {values: 2, code: 'INTERVAL', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionInterval)},
                        {values: 2, code: 'NOT_INTERVAL', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotInterval)},
                    ];
                    break;
                case 'DATE':
                    renderFields = renderDateFields;
                    normalizeField = (code, valuesCount, value, index) => {
                        return value;
                    };
                    validateField = (code, valuesCount, value, index) => {
                        if (!value) return this.props.intl.formatMessage(globalMessages.validationRequired);
                    };
                    items = [
                        {values: 0, code: 'NONE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNone)},
                        {values: 0, code: 'EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEmpty)},
                        {values: 0, code: 'NOT_EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotEmpty)},
                        {values: 0, code: 'UNDEFINED', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUndefined)},
                        {values: 1, code: 'GT', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionGt)},
                        {values: 1, code: 'GE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionGe)},
                        {values: 1, code: 'LT', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionLt)},
                        {values: 1, code: 'LE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionLe)},
                        {values: 1, code: 'EQ', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEq)},
                        {values: 1, code: 'NE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNe)},
                        {values: 2, code: 'INTERVAL', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionInterval)},
                        {values: 2, code: 'NOT_INTERVAL', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotInterval)},
                    ];
                    break;
                case 'RECORD_REF': {
                    const conditionSelectedCode = this.state ? this.state.conditionSelectedCode : 'NONE';
                    const {versionId} = this.props;
                    if (conditionSelectedCode === 'CONTAIN_ENTITY') {
                        renderFields = fields => {
                            return fields.map((field, index) => (
                                <EntityField
                                    key={index}
                                    value={field.value}
                                    error={field.error}
                                    onChange={field.onChange}
                                    versionId={versionId}
                                />
                            ));
                        };
                    } else {
                        renderFields = renderTextFields;
                    }
                    validateField = (code, valuesCount, value, index) => {
                        return value ? null : this.props.intl.formatMessage(globalMessages.validationRequired);
                    };
                    items = [
                        {values: 0, code: 'NONE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNone)},
                        {values: 0, code: 'EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEmpty)},
                        {values: 0, code: 'NOT_EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotEmpty)},
                        {values: 0, code: 'UNDEFINED', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUndefined)},
                        {values: 1, code: 'CONTAIN', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionStringContain)},
                        {values: 1, code: 'CONTAIN_ENTITY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionContainEntity)},
                    ];
                    break;
                }
                case 'UNITDATE':
                    renderFields = renderTextFields ;
                    validateField = (code, valuesCount, value, index) => {
                        const validateResult = validateUnitDate(value);
                        return validateResult.valid ? null : validateResult.message;
                    };
                    items = [
                        {values: 0, code: 'NONE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNone)},
                        {values: 0, code: 'EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEmpty)},
                        {values: 0, code: 'NOT_EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotEmpty)},
                        {values: 0, code: 'UNDEFINED', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUndefined)},
                        {values: 1, code: 'EQ', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEq)},
                        {values: 1, code: 'LT', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUnitdateLt)},
                        {values: 1, code: 'GT', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUnitdateGt)},
                        {values: 2, code: 'SUBSET', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUnitdateSubset)},
                        {
                            values: 2,
                            code: 'INTERSECT',
                            name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUnitdateIntersect),
                        },
                    ];
                    break;
                case 'COORDINATES':
                    renderFields = renderCoordinatesFields;
                    validateField = (code, valuesCount, value, index) => {
                        return validateCoordinatePoint(value);
                    };
                    items = [
                        {values: 0, code: 'NONE', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNone)},
                        {values: 0, code: 'EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionEmpty)},
                        {values: 0, code: 'NOT_EMPTY', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionNotEmpty)},
                        {values: 0, code: 'UNDEFINED', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionUndefined)},
                        {values: 1, code: 'SUBSET', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionCoordinatesSubset)},
                        {values: 2, code: 'NEAR', name: this.props.intl.formatMessage(arrMessages.fundFilterSettingsConditionCoordinatesNear)},
                    ];
                    break;
                case 'JSON_TABLE':
                case 'ENUM':
                    break;
                default:
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
                className="filter-content-container"
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
        const {
            selectedValueItems,
            selectedValueItemsType,
            valueAccodrionType,
            selectedSpecItems,
            selectedSpecItemsType,
            conditionSelectedCode,
            conditionValues,
        } = this.state;
        const {onSubmitForm, refType} = this.props;

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
            default:
                break;
        }

        // ##
        // # Test, zda není filtr prázdný
        // ##
        var outData = null;

        if (data.valuesType === 'selected' || data.values.length > 0) {
            // je zadáno filtrování podle hodnoty
            outData = data;
        } else if (refType.useSpecification && (data.specsType === 'selected' || data.specs.length > 0)) {
            // je zadáno filtrování podle specifikace
            outData = data;
        } else if (data.conditionType !== 'NONE') {
            // je zadáno filtrování podle podmínky
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
        const {
            isFetchingSpecIds,
            refMarkSelectedNode,
            conditionHasErrors,
            valueAccodrionType,
            conditionSelectedCode,
            conditionValues,
            selectedSpecItems,
            selectedSpecItemsType,
            specItems,
        } = this.state;

        var specContent = null;
        if (refType.id === COL_REFERENCE_MARK) {
            // nemá specifikaci
        } else if (refType.useSpecification) {
            specContent = (
                <SimpleCheckListBox
                    ref="specsListBox"
                    items={specItems}
                    label={<FormattedMessage {...arrMessages.fundFilterSettingsFilterBySpecificationTitle} />}
                    value={{type: selectedSpecItemsType, ids: selectedSpecItems}}
                    onChange={this.handleSpecItemsChange}
                >
                    {isFetchingSpecIds && <HorizontalLoader hover showText={false} />}
                </SimpleCheckListBox>
            );
        }

        var valueContent;
        var conditionContent;
        var hasAllValues = true;

        let okButtons = [
            <Button key="clear" variant="outline-secondary" className="mr-auto" onClick={this.handleClearSubmit}>
                {<FormattedMessage {...arrMessages.fundFilterSettingsActionClear} />}
            </Button>,
        ];
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
            okButtons.push(
                <Button key="store" variant="outline-secondary" disabled={okDisabled} onClick={this.handleSubmit}>
                    {<FormattedMessage {...globalMessages.save} />}
                </Button>,
            );
        } else {
            // referenční označení
            valueContent = (
                <FundNodesSelect
                    selectedId={filter && filter.nodeId != null ? filter.nodeId : null}
                    multipleSelection={false}
                    onChange={this.handleNodeSelectChange}
                />
            );

            const okDisabled = refMarkSelectedNode === null;
            okButtons.push(
                <Button
                    key="store"
                    variant="outline-secondary"
                    disabled={okDisabled}
                    onClick={this.handleRefMarkSubmit}
                >
                    {<FormattedMessage {...arrMessages.globalActionSelect} />}
                </Button>,
            );
        }

        let accordion;
        if (conditionContent && valueContent) {
            accordion = (
                <Accordion
                    className="accordion-simple bordered"
                    activeKey={valueAccodrionType}
                    onSelect={type => {
                        this.setState({valueAccodrionType: type});
                    }}
                >
                    <Card className={valueAccodrionType === 'CONDITION' ? 'open' : ''}>
                        <Card.Header>
                            <h4>{<FormattedMessage {...arrMessages.fundFilterSettingsFilterByConditionTitle} />}</h4>
                        </Card.Header>
                        <Accordion.Collapse eventKey="CONDITION">
                            <Card.Body>{conditionContent}</Card.Body>
                        </Accordion.Collapse>
                    </Card>
                    <Card className={valueAccodrionType === 'VALUE' ? 'open' : ''}>
                        <Card.Header>
                            <h4>{<FormattedMessage {...arrMessages.fundFilterSettingsFilterByValueTitle} />}</h4>
                        </Card.Header>
                        <Accordion.Collapse eventKey="VALUE">
                            <Card.Body>{valueContent}</Card.Body>
                        </Accordion.Collapse>
                    </Card>
                </Accordion>
            );
        } else {
            accordion = (
                <div>
                    {conditionContent && (
                        <div>
                            <h4>{<FormattedMessage {...arrMessages.fundFilterSettingsFilterByConditionTitle} />}</h4>
                            {conditionContent}
                        </div>
                    )}
                    {valueContent && (
                        <div>
                            <h4>{<FormattedMessage {...arrMessages.fundFilterSettingsFilterByValueTitle} />}</h4>
                            {valueContent}
                        </div>
                    )}
                </div>
            );
        }

        return (
            <div className="fund-filter-settings-container">
                <Modal.Body>
                    <div className="filters-container">
                        {specContent}
                        {accordion}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    {okButtons}
                    <Button variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.cancel} />}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
};

export default FundFilterSettings;
