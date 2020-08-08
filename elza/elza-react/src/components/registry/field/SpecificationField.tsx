import React, {FC} from 'react';
import {connect} from "react-redux";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {FormInput} from "../../index";

interface Props {
    itemTypeId: number;
    refTables: any;
    itemSpecIds?: Array<number>;  // pokud je uvedeno, bere se v7běr z nich, jinak se bere z item type
}

const SpecificationField: FC<Props> = ({refTables, itemTypeId, itemSpecIds, ...rest}) => {
    const itemType = refTables.descItemTypes.itemsMap[itemTypeId] as RulDescItemTypeExtVO;

    let itemSpecsList = itemType.descItemSpecs
        .sort((a, b) => a.name.localeCompare(b.name));

    if (itemSpecIds) {
        itemSpecsList = itemSpecsList.filter(x => itemSpecIds.includes(x.id));
    }

    return (
        <FormInput
            as={'select'}
            {...rest}
        >
            <option key={""}></option>
            {itemSpecsList.map((spec) => {
                return <option key={spec.id} value={spec.id}>{spec.name}</option>
            })}
        </FormInput>
    );
};

const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
    }
};

export default connect(mapStateToProps)(SpecificationField);
