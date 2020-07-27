import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import AddFileForm from './AddFileForm';
import {connect} from 'react-redux';
import {Accordion, Card} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx';
import DescItemType from './nodeForm/DescItemType.jsx';
import {registryAdd, registryDetailFetchIfNeeded} from 'actions/registry/registry.jsx';
import {routerNavigate} from 'actions/router.jsx';
import {setInputFocus} from 'components/Utils.jsx';
import {canSetFocus, focusWasSet, isFocusFor, setFocus} from 'actions/global/focus.jsx';
import {UrlFactory} from 'actions/index.jsx';
import {selectTab} from 'actions/global/tab.jsx';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx';
import {fundFilesCreate} from 'actions/arr/fundFiles.jsx';
import {getOneSettings, setSettings} from 'components/arr/ArrUtils.jsx';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx';
import './SubNodeForm.scss';
import {downloadFile} from '../../actions/global/download';
import {FOCUS_KEYS} from '../../constants.tsx';
import {objectEqualsDiff} from '../Utils';
import {SUB_NODE_FORM_CMP} from '../../stores/app/arr/subNodeForm';

import classNames from 'classnames';
import {Button} from '../ui';
import FormDescItemGroup from './FormDescItemGroup';

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class SubNodeForm extends AbstractReactComponent {
    refNodeForm = null;

    refObjects = {};

    constructor(props) {
        super(props);

        this.bindMethods(
            'renderDescItemGroup',
            'handleChange',
            'handleChangePosition',
            'handleChangeSpec',
            'handleDescItemTypeRemove',
            'handleSwitchCalculating',
            'handleBlur',
            'handleFocus',
            'handleDescItemAdd',
            'handleDescItemRemove',
            'handleCreateParty',
            'handleDescItemTypeCopyFromPrev',
            'handleDescItemTypeLock',
            'handleDescItemTypeCopy',
            'handleCreatedParty',
            'handleCreateRecord',
            'handleCreatedRecord',
            'trySetFocus',
            'initFocus',
            'getFlatDescItemTypes',
            'handleJsonTableDownload',
            'handleDetailParty',
            'handleDetailRecord',
            'handleCreateFile',
            'handleFundFiles',
            'handleCoordinatesUpload',
            'handleJsonTableUpload',
            'handleCoordinatesDownload',
        );
    }

    state = {
        unusedItemTypeIds: [],
        unusedItemTypeInitIds: [],
    };

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        routingKey: PropTypes.oneOf(PropTypes.string, PropTypes.number),
        nodeSetting: PropTypes.object,
        rulDataTypes: PropTypes.object.isRequired,
        calendarTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        structureTypes: PropTypes.object.isRequired,
        subNodeForm: PropTypes.object.isRequired,
        closed: PropTypes.bool.isRequired,
        readMode: PropTypes.bool.isRequired,
        conformityInfo: PropTypes.object.isRequired,
        descItemCopyFromPrevEnabled: PropTypes.bool.isRequired,
        focus: PropTypes.object,
        formActions: PropTypes.object.isRequired,
        showNodeAddons: PropTypes.bool.isRequired,
        arrPerm: PropTypes.bool.isRequired,
    };

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        } else {
            const log = false;
            return (
                !objectEqualsDiff(this.props.subNodeForm, nextProps.subNodeForm, SUB_NODE_FORM_CMP, '', log) ||
                !objectEqualsDiff(
                    this.props.descItemCopyFromPrevEnabled,
                    nextProps.descItemCopyFromPrevEnabled,
                    {},
                    '',
                    log,
                ) ||
                !objectEqualsDiff(this.props.nodeSetting, nextProps.nodeSetting, undefined, {}, log) ||
                !objectEqualsDiff(this.props.readMode, nextProps.readMode, undefined, {}, log)
            );
        }
    }

    componentDidMount() {
        this.trySetFocus(this.props);

        const {subNodeForm} = this.props;
        const unusedItemTypeIds = subNodeForm.unusedItemTypeIds || [];
        this.setState({
            unusedItemTypeIds: unusedItemTypeIds,
            unusedItemTypeInitIds: unusedItemTypeIds,
        });
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);

        const prevSubNodeForm = this.props.subNodeForm;
        const nextSubNodeForm = nextProps.subNodeForm;
        if (prevSubNodeForm.unusedItemTypeIds !== nextSubNodeForm.unusedItemTypeIds) {
            const unusedItemTypeIds = nextSubNodeForm.unusedItemTypeIds;
            this.setState({
                unusedItemTypeIds: unusedItemTypeIds,
            });
        }
    }

    initFocus() {
        if (this.refNodeForm) {
            const el = ReactDOM.findDOMNode(this.refNodeForm);
            if (el) {
                setInputFocus(el, false);
            }
        }
    }

    trySetFocus(props) {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.ARR, 2, 'subNodeForm')) {
                if (focus.item) {
                    // položka
                    this.setState({}, () => {
                        const ref = this.refObjects['descItemType' + focus.item.descItemTypeId];
                        if (ref) {
                            let descItemType = ref;
                            if (ref.getWrappedInstance) {
                                descItemType = ref.getWrappedInstance();
                            }
                            descItemType.focus(focus.item);
                        }
                        focusWasSet();
                    });
                } else {
                    // obecně formulář
                    this.setState({}, () => {
                        const el = ReactDOM.findDOMNode(this.refNodeForm);
                        if (el) {
                            setInputFocus(el, false);
                        }
                        focusWasSet();
                    });
                }
            }
        }
    }

    handleShortcuts(action) {}

    descItemRef = (key, ref) => {
        this.refObjects[key] = ref;
    };

    /**
     * Renderování skupiny atributů.
     * @param descItemGroup {Object} skupina
     * @param descItemGroupIndex {Integer} index skupiny v seznamu
     * @param nodeSetting
     * @return {Object} view
     */
    renderDescItemGroup(descItemGroup, descItemGroupIndex, nodeSetting) {
        const {
            userDetail,
            readMode,
            versionId,
            arrRegion,
            descItemCopyFromPrevEnabled,
            fundId,
            arrPerm,
            typePrefix,
            subNodeForm,
            singleDescItemTypeEdit,
            singleDescItemTypeId,
            showNodeAddons,
            conformityInfo,
            calendarTypes,
            structureTypes,
            descItemFactory,
            customActions,
        } = this.props;

        const fund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
        let strictMode = false;
        if (fund) {
            strictMode = fund.activeVersion.strictMode;
            let userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
            if (userStrictMode && userStrictMode.value !== null) {
                strictMode = userStrictMode.value === 'true';
            }
        }

        return (
            <FormDescItemGroup
                descItemGroup={descItemGroup}
                descItemGroupIndex={descItemGroupIndex}
                nodeSetting={nodeSetting}
                singleDescItemTypeEdit={singleDescItemTypeEdit}
                singleDescItemTypeId={singleDescItemTypeId}
                subNodeForm={subNodeForm}
                typePrefix={typePrefix}
                arrPerm={arrPerm || (subNodeForm.data && subNodeForm.data.arrPerm)}
                fundId={fundId}
                strictMode={strictMode}
                showNodeAddons={showNodeAddons}
                descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                conformityInfo={conformityInfo}
                versionId={versionId}
                readMode={readMode}
                calendarTypes={calendarTypes}
                structureTypes={structureTypes}
                descItemFactory={descItemFactory}
                customActions={customActions}
                descItemRef={this.descItemRef}
                onCreateParty={this.handleCreateParty}
                onDetailParty={this.handleDetailParty}
                onCreateRecord={this.handleCreateRecord}
                onDetailRecord={this.handleDetailRecord}
                onCreateFile={this.handleCreateFile}
                onFundFiles={this.handleFundFiles}
                onDescItemAdd={this.handleDescItemAdd}
                onCoordinatesUpload={this.handleCoordinatesUpload}
                onJsonTableUpload={this.handleJsonTableUpload}
                onDescItemRemove={this.handleDescItemRemove}
                onCoordinatesDownload={this.handleCoordinatesDownload}
                onJsonTableDownload={this.handleJsonTableDownload}
                onChange={this.handleChange}
                onChangePosition={this.handleChangePosition}
                onChangeSpec={this.handleChangeSpec}
                onBlur={this.handleBlur}
                onFocus={this.handleFocus}
                onDescItemTypeRemove={this.handleDescItemTypeRemove}
                onSwitchCalculating={this.handleSwitchCalculating}
                onDescItemTypeLock={this.handleDescItemTypeLock}
                onDescItemTypeCopy={this.handleDescItemTypeCopy}
                onDescItemTypeCopyFromPrev={this.handleDescItemTypeCopyFromPrev}
                onDescItemNotIdentified={this.handleDescItemNotIdentified}
            />
        );
    }

    /**
     * Odebrání hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleDescItemRemove(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };

        // Focus na následující hodnotu, pokud existuje, jinak na předchozí hodnotu, pokud existuje, jinak obecně na descItemType (reálně se nastaví na první hodnotu daného atributu)
        // Focus musíme zjišťovat před DISPATCH formActions.fundSubNodeFormValueDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
        let setFocusFunc;
        const {
            subNodeForm: {formData},
        } = this.props;
        const descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex];
        if (descItemIndex + 1 < descItemType.descItems.length) {
            // následující hodnota
            let focusDescItem = descItemType.descItems[descItemIndex + 1];
            setFocusFunc = () =>
                setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                    descItemTypeId: descItemType.id,
                    descItemObjectId: focusDescItem.descItemObjectId,
                    descItemIndex: descItemIndex,
                });
        } else if (descItemIndex > 0) {
            // předchozí hodnota
            let focusDescItem = descItemType.descItems[descItemIndex - 1];
            setFocusFunc = () =>
                setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                    descItemTypeId: descItemType.id,
                    descItemObjectId: focusDescItem.descItemObjectId,
                    descItemIndex: descItemIndex - 1,
                });
        } else {
            // obecně descItemType
            setFocusFunc = () =>
                setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                    descItemTypeId: descItemType.id,
                    descItemObjectId: null,
                    descItemIndex: null,
                });
        }

        // Smazání hodnoty
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueDelete(
                this.props.versionId,
                this.props.routingKey,
                valueLocation,
            ),
        );

        // Nyní pošleme focus
        this.props.dispatch(setFocusFunc());
    }

    handleSwitchCalculating(descItemGroupIndex, descItemTypeIndex) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        };

        const {
            subNodeForm: {formData},
            versionId,
            routingKey,
        } = this.props;
        const descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex];

        const msgI18n =
            descItemType.calSt === 1 ? 'subNodeForm.calculate-auto.confirm' : 'subNodeForm.calculate-user.confirm';

        if (window.confirm(i18n(msgI18n))) {
            this.props.dispatch(
                this.props.formActions.switchOutputCalculating(versionId, descItemType.id, routingKey, valueLocation),
            );
        }
    }

    /**
     * Odebrání atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     */
    handleDescItemTypeRemove(descItemGroupIndex, descItemTypeIndex) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        };

        // Focus na následující prvek, pokud existuje, jinak na předchozí, pokud existuje, jinak na accordion
        // Focus musíme zjišťovat před DISPATCH formActions.fundSubNodeFormDescItemTypeDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
        let setFocusFunc;
        const {
            subNodeForm: {formData},
        } = this.props;
        const descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex];
        const types = this.getFlatDescItemTypes(true);
        const indexUnused =
            this.state.unusedItemTypeInitIds != null ? this.state.unusedItemTypeInitIds.indexOf(descItemType.id) : -1;
        if (indexUnused !== -1) {
            const unusedItemTypeIds = this.state.unusedItemTypeIds;
            this.setState({
                unusedItemTypeIds: [
                    ...unusedItemTypeIds.slice(0, indexUnused),
                    descItemType.id,
                    ...unusedItemTypeIds.slice(indexUnused),
                ],
            });
        }
        const index = indexById(types, descItemType.id);
        if (index + 1 < types.length) {
            let focusDescItemType = types[index + 1];
            setFocusFunc = () =>
                setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                    descItemTypeId: focusDescItemType.id,
                    descItemObjectId: null,
                });
        } else if (index > 0) {
            let focusDescItemType = types[index - 1];
            setFocusFunc = () =>
                setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                    descItemTypeId: focusDescItemType.id,
                    descItemObjectId: null,
                });
        } else {
            // nemůžeme žádný prvek najít, focus dostane accordion
            setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'accordion');
        }

        // Smazání hodnoty
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormDescItemTypeDelete(
                this.props.versionId,
                this.props.routingKey,
                valueLocation,
            ),
        );

        // Nyní pošleme focus
        this.props.dispatch(setFocusFunc());
    }

    getFlatDescItemTypes(onlyNotLocked) {
        const {
            subNodeForm: {formData},
        } = this.props;

        let nodeSetting;
        if (onlyNotLocked) {
            nodeSetting = this.props.nodeSetting;
        }

        const result = [];

        formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(type => {
                if (onlyNotLocked) {
                    if (!this.isDescItemLocked(nodeSetting, type.id)) {
                        result.push(type);
                    }
                } else {
                    result.push(type);
                }
            });
        });

        return result;
    }

    /**
     * Přidání nové hodnoty vícehodnotového atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     */
    handleDescItemAdd(descItemGroupIndex, descItemTypeIndex) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        };

        // Focus na novou hodnotu
        const {
            subNodeForm: {formData},
        } = this.props;
        const descItemType = formData.descItemGroups[descItemGroupIndex].descItemTypes[descItemTypeIndex];
        const index = descItemType.descItems.length;

        // pokud není opakovatelný, nelze přidat další položku
        if (descItemType.rep !== 1) {
            return;
        }

        const setFocusFunc = () =>
            setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                descItemTypeId: descItemType.id,
                descItemObjectId: null,
                descItemIndex: index,
            });

        // Přidání hodnoty
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueAdd(this.props.versionId, this.props.routingKey, valueLocation),
        );

        // Nyní pošleme focus
        this.props.dispatch(setFocusFunc());
    }

    /**
     * Přidání nové hodnoty coordinates pomocí uploadu
     * @param descItemTypeId {Integer} Id descItemTypeId
     * @param file {File} soubor
     */
    handleCoordinatesUpload(descItemTypeId, file) {
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueUploadCoordinates(
                this.props.versionId,
                this.props.routingKey,
                descItemTypeId,
                file,
            ),
        );
    }

    /**
     * Přidání nové hodnoty jsonTable pomocí uploadu.
     * @param descItemTypeId {Integer} Id descItemTypeId
     * @param file {File} soubor
     */
    handleJsonTableUpload(descItemTypeId, file) {
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueUploadCsv(
                this.props.versionId,
                this.props.routingKey,
                descItemTypeId,
                file,
            ),
        );
    }

    /**
     * Vytvoření nového hesla.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleCreateRecord(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const {versionId} = this.props;
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };
        this.props.dispatch(registryAdd(versionId, this.handleCreatedRecord.bind(this, valueLocation), true));
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data {Object} data z formuláře
     * @param submitType {String} typ submitu
     */
    handleCreatedRecord(valueLocation, data, submitType) {
        const {versionId, routingKey, subNodeForm} = this.props;

        // Uložení hodnoty
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueChange(versionId, routingKey, valueLocation, data, true),
        );

        // Akce po vytvoření
        if (submitType === 'storeAndViewDetail') {
            // přesměrování na detail
            this.props.dispatch(registryDetailFetchIfNeeded(data.id));
            this.props.dispatch(routerNavigate('registry'));
        } else {
            // nastavení focus zpět na prvek
            const formData = subNodeForm.formData;
            const descItemType =
                formData.descItemGroups[valueLocation.descItemGroupIndex].descItemTypes[
                    valueLocation.descItemTypeIndex
                ];
            this.props.dispatch(
                setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {
                    descItemTypeId: descItemType.id,
                    descItemObjectId: null,
                    descItemIndex: valueLocation.descItemIndex,
                }),
            );
        }
    }

    /**
     * Zobrazení detailu rejstříku.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param recordId {Integer} identifikátor rejstříku
     */
    handleDetailRecord(descItemGroupIndex, descItemTypeIndex, descItemIndex, recordId) {
        const {singleDescItemTypeEdit} = this.props;
        singleDescItemTypeEdit && this.props.dispatch(modalDialogHide());
        this.props.dispatch(registryDetailFetchIfNeeded(recordId));
        this.props.dispatch(routerNavigate('registry'));
    }

    /**
     * Vytvoření obalu.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data          obal
     */
    handleCreatedPacket(valueLocation, data) {
        const {versionId, routingKey} = this.props;

        // Uložení hodnoty
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueChange(versionId, routingKey, valueLocation, data, true),
        );
    }

    /**
     * Vytvoření nové osoby.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param partyTypeId {Integer} identifikátor typu osoby
     */
    handleCreateParty(descItemGroupIndex, descItemTypeIndex, descItemIndex, partyTypeId) {
        console.warn('%c ::party ', 'background: black; color: yellow;');
    }

    /**
     * Vytvoření souboru.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleCreateFile(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('dms.file.title.add'),
                <AddFileForm onSubmitForm={this.handleCreateFileFormSubmit.bind(this, valueLocation)} />,
            ),
        );
    }

    /**
     * Odeslání formuláře na vytvoření souboru.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data          soubor
     */
    handleCreateFileFormSubmit(valueLocation, data) {
        const {fundId} = this.props;
        return this.props.dispatch(fundFilesCreate(fundId, data, this.handleCreatedFile.bind(this, valueLocation)));
    }

    /**
     * Vytvoření souboru.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data          soubor
     */
    handleCreatedFile(valueLocation, data) {
        const {versionId, routingKey} = this.props;

        // Uložení hodnoty
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueChange(versionId, routingKey, valueLocation, data, true),
        );
    }

    /**
     * TODO @randak
     * @param type
     * @param value
     */
    enableIfDisabled(type, value) {
        const {fundId, userDetail} = this.props;
        let settings = getOneSettings(userDetail.settings, type, 'FUND', fundId);
        let data = settings.value ? JSON.parse(settings.value) : null;
        const enabled = data !== null ? data[value] : true;
        if (!enabled) {
            let newData = {};
            if (data) {
                data[value] = true;
                newData = data;
            } else {
                newData[value] = true;
            }
            settings.value = JSON.stringify(newData);
            settings = setSettings(userDetail.settings, settings.id, settings);
            //settings = setSettings(settings, centerSettings.id, centerSettings);
            console.warn(1, settings);
            this.props.dispatch(userDetailsSaveSettings(settings));
        }
    }

    /**
     * Zobrazení souborů FA.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleFundFiles(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const tab = 'files';
        this.enableIfDisabled('FUND_CENTER_PANEL', 'rightPanel');
        this.enableIfDisabled('FUND_RIGHT_PANEL', tab);

        this.props.dispatch(routerNavigate('arr'));
        this.props.dispatch(selectTab('arr-as', tab));
        this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 3, null, null));
    }

    handleCreatedParty(valueLocation, data, submitType) {
        console.warn('%c ::party ', 'background: black; color: yellow;');
    }

    /**
     * Zobrazení detailu osoby.
     *
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param partyId {Integer} identifikátor osoby
     */
    handleDetailParty(descItemGroupIndex, descItemTypeIndex, descItemIndex, partyId) {
        console.warn('%c ::party ', 'background: black; color: yellow;');
    }

    /**
     * Opuštění hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleBlur(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };

        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueBlur(this.props.versionId, this.props.routingKey, valueLocation),
        );
    }

    /**
     * Nový focus do hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     */
    handleFocus(descItemGroupIndex, descItemTypeIndex, descItemIndex) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };

        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueFocus(
                this.props.versionId,
                this.props.routingKey,
                valueLocation,
            ),
        );
    }

    /**
     * Změna hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param value {Object} nová hodnota atributu
     */
    handleChange(descItemGroupIndex, descItemTypeIndex, descItemIndex, value, error) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };

        // Updates the value in form data.
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueChange(
                this.props.versionId,
                this.props.routingKey,
                valueLocation,
                value,
                false,
            ),
        );
        // Updates the error value in descItem.
        // Only when error exists
        error &&
            this.props.dispatch(
                this.props.formActions._fundSubNodeFormValueValidateResult(
                    this.props.versionId,
                    this.props.routingKey,
                    valueLocation,
                    error,
                ),
            );
    }

    /**
     * Změna pozice hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param newDescItemIndex {Integer} nová pozice - nový index atributu
     */
    handleChangePosition(descItemGroupIndex, descItemTypeIndex, descItemIndex, newDescItemIndex) {
        // console.log(222222, descItemGroupIndex, descItemTypeIndex, descItemIndex, newDescItemIndex);
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };

        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueChangePosition(
                this.props.versionId,
                this.props.routingKey,
                valueLocation,
                newDescItemIndex,
            ),
        );
    }

    /**
     * Změna specifikace u hodnoty atributu.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemIndex {Integer} index honodty atributu v seznamu
     * @param value {Object} nová hodnota specifikace u atributu
     */
    handleChangeSpec(descItemGroupIndex, descItemTypeIndex, descItemIndex, value) {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };

        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueChangeSpec(
                this.props.versionId,
                this.props.routingKey,
                valueLocation,
                value,
            ),
        );
    }

    isDescItemLocked(nodeSetting, descItemTypeId) {
        // existuje nastavení o JP - zamykání
        if (nodeSetting && nodeSetting.descItemTypeLockIds) {
            const index = nodeSetting.descItemTypeLockIds.indexOf(descItemTypeId);

            // existuje type mezi zamknutými
            if (index >= 0) {
                return true;
            }
        }
        return false;
    }

    handleCoordinatesDownload(objectId) {
        const {versionId} = this.props;

        this.props.dispatch(downloadFile(UrlFactory.exportArrCoordinate(objectId, versionId)));
    }

    handleJsonTableDownload(objectId) {
        const {versionId, typePrefix} = this.props;
        this.props.dispatch(downloadFile(UrlFactory.exportItemCsvExport(objectId, versionId, typePrefix)));
    }

    /**
     * Akce okamžitého kopírování hodnot atributu z předcházející JP.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemTypeId {Integer} id desc item type
     */
    handleDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemTypeId) {
        this.props.onDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemTypeId);
    }

    /**
     * Přidání/odebrání zámku pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param locked {Boolean} true, pokud se má zámek povolit
     */
    handleDescItemTypeLock(descItemTypeId, locked) {
        this.props.onDescItemTypeLock(descItemTypeId, locked);
    }

    /**
     * Přidání/odebrání opakovaného pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param copy {Boolean} true, pokud se má opakované kopírování povolit
     */
    handleDescItemTypeCopy(descItemTypeId, copy) {
        this.props.onDescItemTypeCopy(descItemTypeId, copy);
    }

    handleDescItemNotIdentified = (descItemGroupIndex, descItemTypeIndex, descItemIndex, descItem) => {
        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
            descItemIndex,
        };
        this.props.dispatch(
            this.props.formActions.fundSubNodeFormValueNotIdentified(
                this.props.versionId,
                this.props.routingKey,
                valueLocation,
                descItem,
            ),
        );
    };

    handleAddUnusedItem = (itemTypeId, index) => {
        const {formActions, versionId} = this.props;
        const {unusedItemTypeIds} = this.state;

        this.props.dispatch(formActions.addCalculatedDescItem(versionId, itemTypeId, true)).then(() => {
            this.setState({
                unusedItemTypeIds: [...unusedItemTypeIds.slice(0, index), ...unusedItemTypeIds.slice(index + 1)],
            });
        });
    };

    render() {
        const {nodeSetting, subNodeForm, closed, readMode} = this.props;
        const {unusedItemTypeIds} = this.state;
        const formData = subNodeForm.formData;

        console.info('{SubNodeForm}');

        let unusedGeneratedItems; // nepoužité vygenerované PP
        if (unusedItemTypeIds && unusedItemTypeIds.length > 0) {
            unusedGeneratedItems = (
                <Accordion>
                    <Card eventKey="1">
                        <Card.Header>
                            {i18n('arr.output.title.unusedGeneratedItems', unusedItemTypeIds.length)}
                        </Card.Header>
                        <Accordion.Collapse eventKey={'1'}>
                            <Card.Body>
                                {unusedItemTypeIds.map((itemTypeId, index) => {
                                    const refType = subNodeForm.refTypesMap[itemTypeId];
                                    if (!readMode && !closed) {
                                        return (
                                            <Button
                                                className="add-link btn btn-link"
                                                variant="link"
                                                key={itemTypeId}
                                                onClick={() => this.handleAddUnusedItem(itemTypeId, index)}
                                            >
                                                <Icon glyph="fa-plus" /> {refType.name}
                                            </Button>
                                        );
                                    } else {
                                        return (
                                            <span className="space" key={itemTypeId}>
                                                {refType.name}{' '}
                                            </span>
                                        );
                                    }
                                })}
                            </Card.Body>
                        </Accordion.Collapse>
                    </Card>
                </Accordion>
            );
        }

        const descItemGroups = [];
        formData.descItemGroups.forEach((group, groupIndex) => {
            const i = this.renderDescItemGroup(group, groupIndex, nodeSetting);
            if (i !== null) {
                descItemGroups.push(i);
            }
        });

        return (
            <div className="node-form">
                {unusedGeneratedItems}
                <div ref={ref => (this.refNodeForm = ref)} className="desc-item-groups">
                    {descItemGroups}
                </div>
            </div>
        );
    }
}

export default connect(
    state => {
        const {userDetail, arrRegion} = state;

        return {
            userDetail,
            arrRegion,
        };
    },
    null,
    null,
    {forwardRef: true},
)(SubNodeForm);
