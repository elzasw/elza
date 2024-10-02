
import "./DetailRow.scss";

const DetailRow = ({label, value}: {label: string, value?: any}) => (
    <div className="item-row">
        <div className="label col">
            <b>{label}</b>
        </div>
        {value && <div className="value col">
            {value}
        </div>
        }
    </div>
);

export default DetailRow;
