/**
 * Stránka pro správu aip.
 */
import { FC } from 'react';
import AipTable from 'components/aip/AipTable';
import './AipPage.scss';
import AipPageRibbon from 'components/aip/AipPageRibbon';

export const AipPage: FC = () => (
    <div>
        <AipPageRibbon />
        <AipTable />
    </div>
);

export default AipPage;
