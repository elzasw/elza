/**
 * Stránka pro správu aip.
 */
import { FC, useEffect, useState } from 'react';
import AipTable from 'components/aip/AipTable';
import './AipPage.scss';
import AipPageRibbon from 'components/aip/AipPageRibbon';
import { useRouteMatch} from 'react-router';
import { useThunkDispatch } from 'utils/hooks';
import { selectAip } from "actions/aip/aip"

interface AipPageUrlParams {
    id?: string;
}

const AipPage: FC = () => {
    const [detailOpen, setDetailOpen] = useState<boolean>(false);
    const dispatch = useThunkDispatch();
    const match = useRouteMatch<AipPageUrlParams>();

    useEffect(() => {
        const id = match.params?.id;
        if (id != null) {
            dispatch(selectAip(id));
            setDetailOpen(true);
        } 
    }, [match.params.id]);

    return (
        <div>
            <AipPageRibbon />
            <AipTable detailOpen={detailOpen} setDetailOpen={setDetailOpen}/>
        </div>
    );
}

export default AipPage;
