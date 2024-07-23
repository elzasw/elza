import { Tag, makeStyles } from "@fluentui/react-components";
import { colDef, findColDefByKey } from "../utils";
import { AipFilter } from "typings/store";
import { AipFilterCriteria } from "./forms/EnumAipFilterCriteria";
import i18n from "components/i18n";

type AipFilterTagProps = {
    filter: AipFilter
}

const AipFilterTag = ({filter}: AipFilterTagProps) => {
    const classes = useStyles();

    const getOperator = () => {
        switch(filter.criteria) {
            case AipFilterCriteria.DOES_NOT_CONTAIN: return "-";
            case AipFilterCriteria.EQUALS: return "=";
            default: return "";
        }
    }

    const getValue = () => {
        if(filter.label) return filter.label.slice(0,15) + "...";
        switch(filter.criteria) {
            case AipFilterCriteria.IS_NULL: return i18n("aip.filter.value.null");
            case AipFilterCriteria.IS_NOT_NULL: return i18n("aip.filter.value.notNull");
            case AipFilterCriteria.BETWEEN: return filter.from + " - " + filter.to;
            default: {
                if(typeof filter.value == "boolean") {
                    return filter.value ? "ANO" : "NE";
                }
                return filter.value;
            };
        } 
    }

    if (filter.invisible) {
        return null;
    }

    return (
        <Tag 
            dismissible
            dismissIcon={{ "aria-label": "remove" }}
            key={`filter-${filter.value}`}
            className={classes.tag}
            size="small"
            value={filter.id}
        >
            {getOperator()} {findColDefByKey(filter.attr).name} : {getValue()}
        </Tag>
    );
}

const useStyles = makeStyles({
	tag: {
		marginRight: "5px", 
        padding: 0,
        backgroundColor: "white", 
        borderRadius: 0, 
        border: "1px solid",
	},

});

export default AipFilterTag;