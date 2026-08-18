import { MenuCheckedValueChangeData, MenuCheckedValueChangeEvent, SearchBox } from "@fluentui/react-components";
import AipFilters from "./AipFilters";
import AipListColSelector from "./AipListColSelector";
import { debounce, storeFromArea } from "shared/utils";
import { useThunkDispatch } from "utils/hooks";
import { AREA_AIPS, aipsFilter } from "actions/aip/aip";
import "./AipFilter.scss";
import { useSelector } from "react-redux";
import { AipFilterEntry } from "typings/store";
import { generateUUID } from "utils/uuid";
import { aipColumns } from "../columns";
import { buildFilter } from "./aipFilterModel";
import { AipFieldName } from "elza-api";
import { useState } from "react";
import { useIntl } from "react-intl";
import { filterMessages } from "../messages";

type AipFilterSectionProps = {
    columns: string[];
    onColsChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
    filterDisabled: boolean;
    initialFilters: AipFilterEntry[];
    hiddenValues: string[];
}

const FULLTEXT_ID = "fulltext";

const AipFilterSection = ({columns, onColsChange, filterDisabled, initialFilters, hiddenValues}: AipFilterSectionProps) => {
    const [filters, setFilters] = useState<AipFilterEntry[]>(initialFilters || []);
    const {formatMessage} = useIntl();

    const handleSearch = debounce((e, data) => {
        if(data.value != "") {
            handleReplace({
                id: FULLTEXT_ID,
                field: AipFieldName.Code,
                filter: buildFilter(AipFieldName.Code, aipColumns[AipFieldName.Code].valueType, {
                    operation: "CONTAINS",
                    value: data.value,
                }),
            });
        } else {
            handleRemove(FULLTEXT_ID)
        }
    }, 1000);

    const handleReplace = (filter: AipFilterEntry) => {
        const oldFilters = filters.filter((item) => item.id != filter.id);
        setFilters([...oldFilters, filter])
    }

    const handleCreate = (filter: AipFilterEntry) => {
        setFilters([...filters, filter]);
    }
    
    const handleRemove = (id) => {
        setFilters(filters.filter((item) => item.id != id));
    }

    return (
        <div className="aip-filter-section">
            <SearchBox 
                placeholder={formatMessage(filterMessages.search)}
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