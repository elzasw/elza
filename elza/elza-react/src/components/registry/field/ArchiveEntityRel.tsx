import React, {useState} from 'react';
import classnames from "classnames";
import {Field} from 'redux-form';
import { useSelector } from "react-redux";
import { AppState } from "typings/store";
import ReduxFormFieldErrorDecorator from "../../shared/form/ReduxFormFieldErrorDecorator";
import FormInput from "../../shared/form/FormInput";
import {Area} from "../../../api/Area";
import {ArchiveEntityVO} from "../../../api/ArchiveEntityVO";
import {WebApi} from "../../../actions/WebApi";
import {debounce} from "../../../shared/utils";
import {ArchiveEntityResultListVO} from "../../../api/ArchiveEntityResultListVO";
import {FilteredResultVO} from "../../../api/FilteredResultVO";
import {ApAccessPointVO} from "../../../api/ApAccessPointVO";
import { TooltipTrigger } from "components/shared";
import RegistryListItem from "../RegistryListItem";

type OwnProps = {}

type Props = {
    onlyMainPart: boolean;
    area: Area;
    itemTypeId: number;
    itemSpecId?: number;
    scopeId?: number;
    disabled: boolean;
    name: string;
    label: string;
    modifyFilterData?: (data: any) => any;
    api?: (itemTypeId: number, itemSpecId: number, filter: any, scopeId?: number) => Promise<ArchiveEntityResultListVO | FilteredResultVO<ApAccessPointVO>>;
} & OwnProps;

export const ArchiveEntityRel = ({onlyMainPart, area, itemTypeId, itemSpecId, scopeId, disabled, name, label, modifyFilterData, api}: Props) => {

    const [items, setItems] = useState<ArchiveEntityVO[] | ApAccessPointVO[]>([]);
    const apTypeIdMap = useSelector((state:AppState) => state.refTables.apTypes.itemsMap)

    const fetchData = debounce(fulltext => {

        let filter = {
            area: area,
            onlyMainPart: onlyMainPart,
            search: fulltext
        };
        if (modifyFilterData) {
            filter = modifyFilterData(filter);
        }

        if (api) {
            return api(itemTypeId, itemSpecId!, filter, scopeId).then(result => {
                if (result.hasOwnProperty('data')) {
                    setItems((result as ArchiveEntityResultListVO).data);
                } else {
                    setItems((result as FilteredResultVO<ApAccessPointVO>).rows);
                }
            });
        }

        if(itemTypeId == undefined){
            return WebApi.findAccessPoint(filter.search,undefined,undefined,undefined,undefined,undefined,0,50 ).then((result) => {
                setItems(result.rows);
            })
        }

        return WebApi.findAccessPointForRel(0, 50, itemTypeId, itemSpecId!, filter, scopeId).then(result => {
            setItems(result.data);
        })
    }, 1000);

    const renderRecord = (props) => {
        if(!props){return <></>}
        const {item, highlighted, selected, ...otherProps} = props;

        return (
            <TooltipTrigger
                key={item.id}
                content={item.description}
                holdOnHover
                placement="horizontal"
                className="tooltip-container"
                {...otherProps}
            >
                <RegistryListItem
                    {...item}
                    typeId={item.aeTypeId || item.typeId}
                    key={'reg-' + item.id}
                    apTypeIdMap={apTypeIdMap}
                    className={classnames('item', {focus: highlighted, active: selected})}
                />
            </TooltipTrigger>
        );
    };

    return <Field
        name={name}
        label={label}
        items={items}
        disabled={disabled}
        getItemName={obj => obj && obj.name}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        renderItem={renderRecord}
        onSearchChange={fetchData}
        itemFilter={(filterText, items, props) => items}
        type="autocomplete"
    />
};
