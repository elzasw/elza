import { Tag, makeStyles } from "@fluentui/react-components";
import { findColDefByField } from "../utils";
import { AipFilterEntry } from "typings/store";
import { DateValueFilter, NumberValueFilter } from "elza-api";
import i18n from "components/i18n";
import { useIntl } from "react-intl";

interface Props {
    filter: AipFilterEntry;
}

export type AipFilterTagProps = Props;

const useStyles = makeStyles({
    tag: {
        marginRight: "5px",
        padding: 0,
        backgroundColor: "white",
        borderRadius: 0,
        border: "1px solid",
    },
});

function operator(entry: AipFilterEntry): string {
    const operation = (entry.filter as {operation?: string}).operation;
    switch (operation) {
        case "NOT_CONTAINS": return "-";
        case "EQ": return "=";
        default: return "";
    }
}

function value(entry: AipFilterEntry): string {
    const filter = entry.filter as {operation?: string; value?: unknown};
    switch (filter.operation) {
        case "IS_NULL": return i18n("aip.filter.value.null");
        case "NOT_NULL": return i18n("aip.filter.value.notNull");
        case "BETWEEN": {
            const range = entry.filter as NumberValueFilter | DateValueFilter;
            return `${range.from} - ${range.to}`;
        }
        default:
            if (entry.label) {
                return entry.label.length > 15 ? entry.label.slice(0, 15) + "..." : entry.label;
            }
            return String(filter.value);
    }
}

export function AipFilterTag({filter}: Props) {
    const classes = useStyles();
    const {formatMessage} = useIntl();

    if (filter.invisible) {
        return null;
    }

    return (
        <Tag
            dismissible
            dismissIcon={{"aria-label": "remove"}}
            className={classes.tag}
            size="small"
            value={filter.id}
        >
            {operator(filter)} {formatMessage(findColDefByField(filter.field).message)} : {value(filter)}
        </Tag>
    );
}

export default AipFilterTag;
