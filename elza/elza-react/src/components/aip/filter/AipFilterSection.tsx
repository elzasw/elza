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
import { AipFilter } from "typings/store";
import { generateUUID } from "../utils";
import { useState } from "react";

type AipFilterSectionProps = {
    columns: string[];
    onColsChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
    filterDisabled: boolean;
    initialFilters: AipFilter[];
    hiddenValues: string[];
}

const FULLTEXT_ID = "fulltext";

const AipFilterSection = ({columns, onColsChange, filterDisabled, initialFilters, hiddenValues}: AipFilterSectionProps) => {
    const [filters, setFilters] = useState<AipFilter[]>(initialFilters || []);

    const handleSearch = debounce((e, data) => {
        if(data.value != "") {
            handleReplace({
                id: FULLTEXT_ID,
                attr: "code",
                criteria: AipFilterCriteria.CONTAINS,
                value: data.value,
                path: "da_aip",
            });
        } else {
            handleRemove(FULLTEXT_ID)
        }
    }, 1000);

    const handleReplace = (filter: AipFilter) => {
        const oldFilters = filters.filter((item) => item.id != filter.id);
        setFilters([...oldFilters, filter])
    }

    const handleCreate = (filter: AipFilter) => {
        const oldFilters = filters;
        filter.id = generateUUID();
        setFilters([...oldFilters, filter]);
    }
    
    const handleRemove = (id) => {
        setFilters(filters.filter((item) => item.id != id));
    }

    return (
        <div className="aip-filter-section">
            <SearchBox 
                placeholder={i18n("aip.table.search")}
                className='search'
                size="small"
                onChange={handleSearch}
                disabled={filterDisabled}
                
            />
            <AipFilters 
                filterDisabled={filterDisabled} 
                hiddenValues={hiddenValues}
                filters={filters}
                createFilter={handleCreate}
                removeFilter={handleRemove}
            /> 
            <AipListColSelector 
                columns={columns} 
                onChange={onColsChange}
                className="last-item"
                hiddenValues={hiddenValues}
            />
        </div>
    );
}

export default AipFilterSection;