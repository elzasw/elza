import i18n from "components/i18n";
import { FC } from "react";
import "./AipFileDetail.scss";

const AipFileDetail: FC = () => {
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

    return (
        <div className="aip-file-detail">
            <h4>{i18n("aip.explorer.detail.title")}</h4>
            <div className="aip-file-detail-body">
                <DetailRow label={i18n("aip.explorer.detail.name")} value="asdasdg"/>
                <DetailRow label={i18n("aip.explorer.detail.checksum")} value="asdasdg"/>
                <DetailRow label={i18n("aip.explorer.detail.format")} value="asd"/>
                <DetailRow label={i18n("aip.explorer.detail.as")} value="nasd"/>
            </div>

            <h4>{i18n("aip.explorer.detail.relationRep")}</h4>
            <span><b>{i18n("aip.explorer.detail.parent")}</b> asdasd</span>

            <h4>{i18n("aip.explorer.detail.relationLog")}</h4>
            <span><b>{i18n("aip.explorer.detail.parent")}</b> asdakjsdfnsd</span>

        </div>
    );
}

export default AipFileDetail;