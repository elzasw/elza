import { ReactNode } from 'react';

import './DetailRow.scss';

interface Props {
    label: ReactNode;
    value?: ReactNode;
}

export function DetailRow({ label, value }: Props) {
    return (
        <div className="aip-detail-item-row">
            <div className="label col">
                <b>{label}</b>
            </div>
            {value != null && (
                <div className="value col">
                    {value}
                </div>
            )}
        </div>
    );
}

export type DetailRowProps = Props;
