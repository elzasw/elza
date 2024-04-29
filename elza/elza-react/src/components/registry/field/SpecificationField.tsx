import React, {FC} from 'react';
import {connect} from 'react-redux';
import {RulDescItemTypeExtVO} from '../../../api/RulDescItemTypeExtVO';
import {FormInput} from '../../index';

interface Props {
    itemTypeId: number;
    itemsMap: {[key: number]: RulDescItemTypeExtVO};
    itemSpecIds?: Array<number>; // pokud je uvedeno, bere se v7běr z nich, jinak se bere z item type
}

const SpecificationField: FC<Props> = ({itemsMap, itemTypeId, itemSpecIds, ...rest}) => {
    const itemType = itemsMap[itemTypeId] as RulDescItemTypeExtVO;

    let itemSpecsList = itemType.descItemSpecs;
    if (itemSpecIds) {
        itemSpecsList = itemSpecsList.filter(x => itemSpecIds.includes(x.id));
    }

    itemSpecsList = itemSpecsList.sort((a, b) => a.viewOrder - b.viewOrder);

    return (
        <FormInput type={'select'} {...rest}>
            <option key={''}></option>
            {itemSpecsList.map(spec => {
                return (
                    <option key={spec.id} value={spec.id}>
                        {spec.name}
                    </option>
                );
            })}
        </FormInput>
    );
};

const mapStateToProps = (state: any) => {
    return {
        itemsMap: state.refTables?.descItemTypes?.itemsMap as any,
    };
};

export default connect(mapStateToProps)(SpecificationField);
