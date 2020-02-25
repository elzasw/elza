import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton } from '../../components/shared';
import {connect} from 'react-redux'
import {Panel, Accordion} from 'react-bootstrap'
import {indexById} from '../../stores/app/utils'
import {partyDetailFetchIfNeeded, partyAdd} from '../../actions/party/party'
// import {registryDetailFetchIfNeeded, registryAdd} from '../../actions/registry/registry'
import {routerNavigate} from '../../actions/router'
import {setInputFocus} from '../Utils'
import {setFocus, canSetFocus, focusWasSet, isFocusFor} from '../../actions/global/focus'
import {UrlFactory} from '../../actions/index.jsx';
import {selectTab} from '../../actions/global/tab'
import {modalDialogShow, modalDialogHide} from '../../actions/global/modalDialog'
import {fundFilesCreate} from '../../actions/arr/fundFiles'
import {setSettings, getOneSettings} from '../arr/ArrUtils';
import {userDetailsSaveSettings} from '../../actions/user/userDetail'
import {WebApi} from '../../actions/index.jsx';
import '../arr/SubNodeForm.less'
import {downloadFile} from "../../actions/global/download";
import {FOCUS_KEYS} from "../../constants";
import {IItemFormState} from "../../stores/app/accesspoint/itemForm";
import {ItemFormActions} from "./ItemFormActions";
import {Promise} from "es6-promise";
import {Dispatch} from "../../typings/globals";
import ItemType from "./ItemType";
import {ItemFactoryInterface} from "./ItemFactoryInterface";

interface ItemFormClassState {
    readonly unusedItemTypeIds: number[]
}

interface FromState {

}

interface DispatchProps {
    dispatch: Dispatch<FromState>;
    userDetail: any
}

interface Props {
    subNodeForm: IItemFormState;
    formActions:ItemFormActions;
    typePrefix: string;
    showNodeAddons: boolean;
    closed: boolean;
    readMode: boolean;
    descItemFactory: ItemFactoryInterface;
    descItemTypes: any;
    structureTypes: any;
    focus: any;
    rulDataTypes: any;
    calendarTypes: any;
    conformityInfo: {missings: any[], errors: any[]};
    customActions?: (string, any) => React.ReactNode;
}
interface ReactWrappedComponent<P> extends React.ReactElement<P> {
    getWrappedInstance: Function;
}

let registryDetailFetchIfNeeded, registryAdd;

import('../../actions/registry/registry').then(imported => {
    registryDetailFetchIfNeeded = imported.registryDetailFetchIfNeeded;
    registryAdd = imported.registryAdd;
})

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class ItemFormClass extends React.Component<DispatchProps & Props, ItemFormClassState> {

    readonly state: ItemFormClassState = {
        unusedItemTypeIds: []
    };

    static PropTypes = {
        versionId: React.PropTypes.number.isRequired,
        fundId: React.PropTypes.number.isRequired,
        routingKey: React.PropTypes.string,
        nodeSetting: React.PropTypes.object,
        rulDataTypes: React.PropTypes.object.isRequired,
        calendarTypes: React.PropTypes.object.isRequired,
        descItemTypes: React.PropTypes.object.isRequired,
        structureTypes: React.PropTypes.object.isRequired,
        subNodeForm: React.PropTypes.object.isRequired,
        closed: React.PropTypes.bool.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        conformityInfo: React.PropTypes.object.isRequired,
        descItemCopyFromPrevEnabled: React.PropTypes.bool.isRequired,
        focus: React.PropTypes.object,
        formActions: React.PropTypes.object.isRequired,
        showNodeAddons: React.PropTypes.bool.isRequired,
    };

    componentDidMount() {
        this.trySetFocus(this.props);

        // Tohle neexistuje
        // const {subNodeForm} = this.props;
        // const unusedItemTypeIds = subNodeForm.unusedItemTypeIds; // Tohle neexistuje jakto
        // this.setState({
        //     unusedItemTypeIds: unusedItemTypeIds
        // });
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);

        const prevSubNodeForm = this.props.subNodeForm;
        const nextSubNodeForm = nextProps.subNodeForm;
        // if (prevSubNodeForm.unusedItemTypeIds !== nextSubNodeForm.unusedItemTypeIds) {
        //     const unusedItemTypeIds = nextSubNodeForm.unusedItemTypeIds;
        //     this.setState({
        //         unusedItemTypeIds: unusedItemTypeIds
        //     });
        // }
    }

    initFocus() {
        if (this.refs.nodeForm) {
            const el = ReactDOM.findDOMNode(this.refs.nodeForm);
            if (el) {
                setInputFocus(el, false);
            }
        }
    }

    trySetFocus(props) {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.ARR, 2, 'subNodeForm')) {
                if (focus.item) {   // položka
                    this.setState({}, () => {
                        const ref = this.refs['descItemType' + focus.item.descItemTypeId] as any as ReactWrappedComponent<{}>;
                        if (ref) {
                            const descItemType = ref.getWrappedInstance();
                            descItemType.focus(focus.item)
                        }
                        focusWasSet()
                    })
                } else {    // obecně formulář
                    this.setState({}, () => {
                        const el = ReactDOM.findDOMNode(this.refs.nodeForm);
                        if (el) {
                            setInputFocus(el, false);
                        }
                        focusWasSet()
                    })
                }
            }
        }
    }

    handleShortcuts(action) {
    }


    /**
     * Odebrání hodnoty atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index hodnoty atributu v seznamu
     */
    handleDescItemRemove(itemTypeIndex, itemIndex) {
        const valueLocation = {
            itemTypeIndex,
            itemIndex,
        };

        // Focus na následující hodnotu, pokud existuje, jinak na předchozí hodnotu, pokud existuje, jinak obecně na descItemType (reálně se nastaví na první hodnotu daného atributu)
        // Focus musíme zjišťovat před DISPATCH formActions.fundSubNodeFormValueDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
        /*let setFocusFunc;
        const {subNodeForm: {formData}} = this.props;
        const descItemType = formData!!.itemTypes[itemTypeIndex];
        if (itemIndex + 1 < descItemType.descItems.length) {    // následující hodnota
            let focusDescItem = descItemType.descItems[itemIndex + 1];
            setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: focusDescItem.descItemObjectId, itemIndex: itemIndex})
        } else if (itemIndex > 0) { // předchozí hodnota
            let focusDescItem = descItemType.descItems[itemIndex - 1];
            setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: focusDescItem.descItemObjectId, itemIndex: itemIndex - 1})
        } else {    // obecně descItemType
            setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, itemIndex: null})
        }*/

        // Smazání hodnoty
        this.props.dispatch(this.props.formActions.fundSubNodeFormValueDelete(valueLocation));

        // Nyní pošleme focus
        // this.props.dispatch(setFocusFunc())
    }

    /**
     * Odebrání atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     */
    handleDescItemTypeRemove(itemTypeIndex) {
        const valueLocation = {
            itemTypeIndex,
        };

        // Focus na následující prvek, pokud existuje, jinak na předchozí, pokud existuje, jinak na accordion
        // Focus musíme zjišťovat před DISPATCH formActions.fundSubNodeFormDescItemTypeDelete, jinak bychom neměli ve formData správná data, protože ty nejsou immutable!
        /*let setFocusFunc;
        const {subNodeForm: {formData}} = this.props;
        const descItemType = formData!!.itemTypes[itemTypeIndex];
        const types = this.getFlatDescItemTypes(true);
        const index = indexById(types, descItemType.id);
        if (index + 1 < types.length) {
            let focusDescItemType = types[index + 1];
            setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: focusDescItemType.id, descItemObjectId: null})
        } else if (index > 0) {
            let focusDescItemType = types[index - 1];
            setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: focusDescItemType.id, descItemObjectId: null})
        } else {    // nemůžeme žádný prvek najít, focus dostane accordion
            setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'accordion')
        }*/

        // Smazání hodnoty
        this.props.dispatch(this.props.formActions.fundSubNodeFormDescItemTypeDelete(valueLocation));

        // Nyní pošleme focus
        // this.props.dispatch(setFocusFunc())
    }

    /**
     * Přidání nové hodnoty vícehodnotového atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     */
    handleDescItemAdd(itemTypeIndex) {
        const valueLocation = {
            itemTypeIndex,
        };

        // Focus na novou hodnotu
        const {subNodeForm: {formData}} = this.props;
        const itemType = formData!!.itemTypes[itemTypeIndex];
        const index = itemType.items.length;

        // pokud není opakovatelný, nelze přidat další položku
        if (itemType.rep !== 1) {
            return;
        }

        const setFocusFunc = () => setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: itemType.id, descItemObjectId: null, itemIndex: index});

        // Přidání hodnoty
        this.props.dispatch(this.props.formActions.fundSubNodeFormValueAdd(valueLocation));

        // Nyní pošleme focus
        this.props.dispatch(setFocusFunc())
    }

    /**
     * Vytvoření nového hesla.
     *
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     */
    handleCreateRecord(itemTypeIndex, itemIndex) {
        const valueLocation = {
            itemTypeIndex,
            itemIndex
        };
        this.props.dispatch(registryAdd(null, null, this.handleCreatedRecord.bind(this, valueLocation), '', true));
    }

    /**
     * Vytvoření hesla po vyplnění formuláře.
     *
     * @param valueLocation pozice hodnoty atributu
     * @param data {Object} data z formuláře
     * @param submitType {String} typ submitu
     */
    handleCreatedRecord(valueLocation, data, submitType) {
        const {subNodeForm} = this.props;

        // Uložení hodnoty
        this.props.dispatch(this.props.formActions.fundSubNodeFormValueChange(valueLocation, data, true));

        // Akce po vytvoření
        if (submitType === 'storeAndViewDetail') {  // přesměrování na detail
            // this.props.dispatch(registryDetailFetchIfNeeded(data.id));
            // this.props.dispatch(routerNavigate('registry'));
        } else {    // nastavení focus zpět na prvek
            const formData = subNodeForm.formData;
            const descItemType = formData!!.itemTypes[valueLocation.itemTypeIndex];
            this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, itemIndex: valueLocation.itemIndex}))
        }
    }

    /**
     * Zobrazení detailu rejstříku.
     *
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     * @param recordId {number} identifikátor rejstříku
     */
    handleDetailRecord(itemTypeIndex, itemIndex, recordId) {
        // this.props.dispatch(registryDetailFetchIfNeeded(recordId));
        // this.props.dispatch(routerNavigate('registry'));
    }


    /**
     * Vytvoření nové osoby.
     *
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     * @param partyTypeId {number} identifikátor typu osoby
     */
    handleCreateParty(itemTypeIndex, itemIndex, partyTypeId) {
        const valueLocation = {
            itemTypeIndex,
            itemIndex
        };
        this.props.dispatch(partyAdd(partyTypeId, null, this.handleCreatedParty.bind(this, valueLocation), true));
    }




    handleCreatedParty(valueLocation, data, submitType) {
        const {subNodeForm} = this.props;

        // Uložení hodnoty
        this.props.dispatch(this.props.formActions.fundSubNodeFormValueChange(valueLocation, data, true));

        // Akce po vytvoření
        if (submitType === 'storeAndViewDetail') {  // přesměrování na detail
            this.props.dispatch(partyDetailFetchIfNeeded(data.id, null));
            this.props.dispatch(routerNavigate('party'));
        } else {    // nastavení focus zpět na prvek
            const formData = subNodeForm.formData;
            const descItemType = formData!!.itemTypes[valueLocation.itemTypeIndex];
            this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'subNodeForm', {descItemTypeId: descItemType.id, descItemObjectId: null, itemIndex: valueLocation.itemIndex}))
        }
    }

    /**
     * Zobrazení detailu osoby.
     *
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     * @param partyId {number} identifikátor osoby
     */
    handleDetailParty(itemTypeIndex, itemIndex, partyId) {
        this.props.dispatch(partyDetailFetchIfNeeded(partyId, null));
        this.props.dispatch(routerNavigate('party'));
    }

    /**
     * Opuštění hodnoty atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     */
    handleBlur(itemTypeIndex, itemIndex) {
        const valueLocation = {
            itemTypeIndex,
            itemIndex
        };

        this.props.dispatch(this.props.formActions.fundSubNodeFormValueBlur(valueLocation));
    }

    /**
     * Nový focus do hodnoty atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     */
    handleFocus(itemTypeIndex, itemIndex) {
        const valueLocation = {
            itemTypeIndex,
            itemIndex
        };

        this.props.dispatch(this.props.formActions.fundSubNodeFormValueFocus(valueLocation));
    }

    /**
     * Změna hodnoty atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     * @param value {Object} nová hodnota atributu
     */
    handleChange(itemTypeIndex, itemIndex, value, error) {
        const valueLocation = {
            itemTypeIndex,
            itemIndex
        };

        // Updates the value in form data.
        this.props.dispatch(this.props.formActions.fundSubNodeFormValueChange(valueLocation, value, false));
        // Updates the error value in descItem.
        // Only when error exists
        error && this.props.dispatch(this.props.formActions._fundSubNodeFormValueValidateResult(valueLocation, error));
    }

    /**
     * Změna pozice hodnoty atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     * @param newDescItemIndex {number} nová pozice - nový index atributu
     */
    handleChangePosition(itemTypeIndex, itemIndex, newDescItemIndex) {
        console.log(222222, itemTypeIndex, itemIndex, newDescItemIndex)
        const valueLocation = {
            itemTypeIndex,
            itemIndex
        };

        this.props.dispatch(this.props.formActions.fundSubNodeFormValueChangePosition(valueLocation, newDescItemIndex));
    }

    /**
     * Změna specifikace u hodnoty atributu.
     * @param itemTypeIndex {number} index atributu v seznamu
     * @param itemIndex {number} index honodty atributu v seznamu
     * @param value {Object} nová hodnota specifikace u atributu
     */
    handleChangeSpec(itemTypeIndex, itemIndex, value) {
        const valueLocation = {
            itemTypeIndex,
            itemIndex
        };

        this.props.dispatch(this.props.formActions.fundSubNodeFormValueChangeSpec(valueLocation, value));
    }


    /**
     * Renderování atributu.
     * @param itemType {Object} atribut
     * @param itemTypeIndex {number} index atributu v seznamu
     * @return {Object} view
     */
    renderDescItemType(itemType, itemTypeIndex) {
        const {subNodeForm, rulDataTypes, structureTypes, calendarTypes, closed,
            showNodeAddons, conformityInfo, readMode, userDetail, typePrefix} = this.props;

        const refType = subNodeForm!!.refTypesMap!!.get(itemType.id)!!;
        const infoType = subNodeForm!!.infoTypesMap!!.get(itemType.id)!!;
        const rulDataType = refType.dataType;

        let locked = false;

        if (infoType.cal === 1) {
            locked = locked || !infoType.calSt;
        }

        let copy = false;

        let strictMode = false;

        let notIdentified = false;

        const items = itemType.items;

        items.forEach(descItem => {
            if (!itemType.rep && descItem.undefined) {
                notIdentified = true;
            }
        });

        return <ItemType key={itemType.id}
             typePrefix={typePrefix}
            ref={'descItemType' + itemType.id}
            descItemType={itemType}
            refType={refType}
            infoType={infoType}
            rulDataType={rulDataType}
            calendarTypes={calendarTypes}
            structureTypes={structureTypes}
            onCreateParty={this.handleCreateParty.bind(this, itemTypeIndex)}
            onDetailParty={this.handleDetailParty.bind(this, itemTypeIndex)}
            onCreateRecord={this.handleCreateRecord.bind(this, itemTypeIndex)}
            onDetailRecord={this.handleDetailRecord.bind(this, itemTypeIndex)}
            onDescItemAdd={this.handleDescItemAdd.bind(this, itemTypeIndex)}
            onDescItemRemove={this.handleDescItemRemove.bind(this, itemTypeIndex)}
            onChange={this.handleChange.bind(this, itemTypeIndex)}
            onChangePosition={this.handleChangePosition.bind(this, itemTypeIndex)}
            onChangeSpec={this.handleChangeSpec.bind(this, itemTypeIndex)}
            onBlur={this.handleBlur.bind(this, itemTypeIndex)}
            onFocus={this.handleFocus.bind(this, itemTypeIndex)}
            onDescItemTypeRemove={this.handleDescItemTypeRemove.bind(this, itemTypeIndex)}
            onDescItemTypeLock={() => {}}
            onDescItemTypeCopy={() => {}}
            onDescItemTypeCopyFromPrev={() => {}}
            showNodeAddons={showNodeAddons}
            locked={locked}
            closed={closed}
            copy={copy}
            conformityInfo={conformityInfo}
            descItemCopyFromPrevEnabled={false}
            readMode={readMode}
            strictMode={strictMode}
            notIdentified={notIdentified}
            onDescItemNotIdentified={(itemIndex, descItem) => this.handleDescItemNotIdentified(itemTypeIndex, itemIndex, descItem)}
            customActions={this.props.customActions && this.props.customActions(rulDataType.code, infoType)}
            descItemFactory={this.props.descItemFactory}
        />
    }

    handleDescItemNotIdentified = (itemTypeIndex, itemIndex, descItem) => {
        const valueLocation = {
            itemTypeIndex,
            itemIndex,
        };
        this.props.dispatch(this.props.formActions.fundSubNodeFormValueNotIdentified(valueLocation, descItem));
    };

    handleAddUnusedItem = (itemTypeId, index) => {
        const {formActions} = this.props;
        const {unusedItemTypeIds} = this.state;

        const x = this.props.dispatch(formActions.addCalculatedDescItem(itemTypeId, true)) as any as Promise<any>;
        x.then(()=>{
            this.setState({
                unusedItemTypeIds: [
                    ...unusedItemTypeIds.slice(0, index),
                    ...unusedItemTypeIds.slice(index + 1)
                ]
            });
        });
    };

    render() {
        const {subNodeForm, closed, readMode} = this.props;
        const {unusedItemTypeIds} = this.state;
        const formData = subNodeForm.formData;

        let unusedGeneratedItems;    // nepoužité vygenerované PP
        if (unusedItemTypeIds && unusedItemTypeIds.length > 0) {
            // Accordion as React.Component;
            unusedGeneratedItems = <Accordion>
                <Panel header={i18n("arr.output.title.unusedGeneratedItems", unusedItemTypeIds.length)} eventKey="1">
                    {unusedItemTypeIds.map((itemTypeId, index) => {
                        const refType = subNodeForm!!.refTypesMap!![itemTypeId];
                        if (!readMode && !closed) {
                            return <a className="add-link btn btn-link" key={itemTypeId} onClick={() => this.handleAddUnusedItem(itemTypeId, index)}><Icon glyph="fa-plus" /> {refType.name}</a>
                        } else {
                            return <span className="space" key={itemTypeId}>{refType.name} </span>
                        }
                    })}
                </Panel>
            </Accordion>
        }

        const nodes : React.ReactNode[] = [];
        if (formData!!.itemTypes) {
            formData!!.itemTypes.forEach((itemType, itemTypeIndex) => {
                const i = this.renderDescItemType(itemType, itemTypeIndex);
                if (i !== null) {
                    nodes.push(i)
                }
            });
        }

        return (
            <div className='node-form'>
                {unusedGeneratedItems}
                <div ref='nodeForm' className='desc-item-groups'>
                    <div className='desc-item-group'>
                        <div className='desc-item-types'>
                            {nodes}
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}


export const ItemForm = connect((state: {userDetail, arrRegion}, props: Props) => {
    const {userDetail, arrRegion} = state;

    return {
        userDetail,
        arrRegion
    }
}, null, null, { withRef: true })(ItemFormClass as any);
