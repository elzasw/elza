import React, {useState} from 'react';
import {Field} from 'redux-form';
import ReduxFormFieldErrorDecorator from "../../shared/form/ReduxFormFieldErrorDecorator";
import FormInput from "../../shared/form/FormInput";
import {Area} from "../../../api/Area";
import {ArchiveEntityVO} from "../../../api/ArchiveEntityVO";
import {WebApi} from "../../../actions/WebApi";
import {debounce} from "../../../shared/utils";

type OwnProps = {}

type Props = {
    onlyMainPart: boolean;
    area: Area;
    itemTypeId: number;
    itemSpecId: number;
    disabled: boolean;
    name: string;
    label: string;
} & OwnProps;

export const ArchiveEntityRel = ({onlyMainPart, area, itemTypeId, itemSpecId, disabled, name, label}: Props) => {

    const [items, setItems] = useState<ArchiveEntityVO[]>([]);

    const fetchData = debounce(fulltext => {
        return WebApi.findAccessPointForRel(0, 50, itemTypeId, itemSpecId, {
            area: area,
            onlyMainPart: onlyMainPart,
            search: fulltext
        }).then(result => {
            setItems(result.data);
        })
    }, 1000);

    return <Field
        name={name}
        label={label}
        items={items}
        disabled={disabled}
        getItemName={obj => obj && obj.name}
        component={ReduxFormFieldErrorDecorator}
        renderComponent={FormInput}
        onSearchChange={fetchData}
        itemFilter={(filterText, items, props) => items}
        type="autocomplete"
    />
};
