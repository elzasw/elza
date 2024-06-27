import { Icon } from "components/shared";
import './Pagination.scss';
import { Dropdown, Option } from "@fluentui/react-components";

type PaginationProps = {
    from: number;
    totalCount: number;
    pageSize: number;
    onPageChange: (nextFrom: number, pageSize: number) => void;
    onPageSizeChange: (pageSize: number) => void;
}

/** 
 * Different pagination based on the new design
 */
const Pagination = ({from, totalCount, pageSize, onPageSizeChange, onPageChange}: PaginationProps) => {
    const options: number[] = [25, 50, 75, 100];

    const handleFirstPage = () => {
        onPageChange(0, pageSize);
    }

    const handlePrevPage = () => {
        const newFrom = from - pageSize >= 0 ? from - pageSize : 0;
        onPageChange(newFrom, pageSize);
    }

    const handleNextPage = () => {
        const newFrom = from + pageSize < totalCount ? from + pageSize : from;
        onPageChange(newFrom, pageSize);
    }

    const handleLastPage = () => {
        const newFrom = totalCount - pageSize;
        onPageChange(newFrom, pageSize);
    }

    const currentPage = Math.ceil(from / pageSize);
    const pageCount = Math.ceil(totalCount / pageSize) ;

    return (
        <div className="pagination">
             <Icon 
                onClick={handleFirstPage} 
                glyph="fa-angle-double-left fa-lg" 
                className="arrow" 
            />
            <Icon 
                onClick={handlePrevPage} 
                glyph="fa-angle-left fa-lg" 
                className="arrow" 
            />
           
            <Icon 
                onClick={handleNextPage} 
                glyph="fa-angle-right fa-lg" 
                className="arrow" 
            />
             <Icon 
                onClick={handleLastPage} 
                glyph="fa-angle-double-right fa-lg" 
                className="arrow" 
            />

            <span className="middle-text">
                {currentPage + 1} / {pageCount}
            </span>

            <Dropdown 
                size="small" 
                appearance="underline" 
                style={{minWidth:"auto"}}
                onOptionSelect={(e, data) => onPageSizeChange(Number(data.optionValue))}
                defaultValue={pageSize ? pageSize.toString() : options[0].toString()}
            >
                {options.map((option) => (
                    <Option key={option}>
                        {option.toString()}
                    </Option>
                ))}
            </Dropdown>
        </div>
    );
};

export default Pagination;