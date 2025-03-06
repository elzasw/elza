import { useEffect, useState } from 'react';
import UsageForm from './UsageForm';
import * as types from 'actions/constants/ActionTypes';
import { WebApi } from '../../actions/WebApi';
import HorizontalLoader from '../shared/loading/HorizontalLoader';

interface Props {
    detail: { id: number }; // TODO - Otypovat registryDetail ve store
}

interface UsageOccurence {
    id: number;
    type: string;
}

interface UsageNode {
    id: number;
    title: string;
    occurences: UsageOccurence[];
}

interface UsageFund {
    id: number;
    name: string;
    nodeCount: number;
    nodes: UsageNode[];
}

interface UsageData {
    funds: UsageFund[];
}

export default function RegistryUsageForm({ detail }: Props) {
    const [data, setData] = useState<UsageData>();

    useEffect(() => {
        (async () => {
            const _data: UsageData = await WebApi.findRegistryUsage(detail.id)
            setData(_data)
        })()
    }, [detail.id])

    return !data
        ? <HorizontalLoader />
        : <UsageForm
            detail={detail}
            treeArea={types.FUND_TREE_AREA_USAGE}
            type="registry"
            data={data}
        />;
}
