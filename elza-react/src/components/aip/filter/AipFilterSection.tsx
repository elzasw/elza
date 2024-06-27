import { MenuCheckedValueChangeData, MenuCheckedValueChangeEvent, SearchBox } from "@fluentui/react-components";
import AipFilters from "./AipFilters";
import AipListColSelector from "./AipListColSelector";
import { debounce, storeFromArea } from "shared/utils";
import { useThunkDispatch } from "utils/hooks";
import { AREA_AIPS, aipsFilter } from "actions/aip/aip";
import { AipFilterCriteria } from "./forms/EnumAipFilterCriteria";
import "./AipFilter.scss";
import { useSelector } from "react-redux";
import i18n from "components/i18n";

type AipFilterSectionProps = {
    columns: string[];
    onColsChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
}

const AipFilterSection = ({columns, onColsChange}: AipFilterSectionProps) => {
    const {filter} = useSelector((state: any) => storeFromArea(state, AREA_AIPS));
    const dispatch = useThunkDispatch();

    const handleSearch = 
        debounce((e, data) => dispatch(aipsFilter([{
                id: null,
                attr: "code",
                criteria: AipFilterCriteria.CONTAINS,
                value: data.value,
                from: null,
                to: null,
                path: "da_aip",
            }], 0, filter.pageSize))
        , 500);

    return (
        <div className="aip-filter-section">
            <SearchBox 
                placeholder={i18n("aip.table.search")}
                className='search'
                size="small"
                onChange={handleSearch}
            />
            <AipFilters /> 
            <AipListColSelector 
                columns={columns} 
                onChange={onColsChange}
                className="last-item"
            />
        </div>
    );
}

export default AipFilterSection;