import { Tag, makeStyles } from "@fluentui/react-components";
import { findColDefByField } from "../columns";
import { filterMessages } from "../messages";
import { AipFilterEntry } from "typings/store";
import { DateValueFilter, NumberValueFilter } from "elza-api";
import { IntlShape, useIntl } from "react-intl";

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

function value(entry: AipFilterEntry, intl: IntlShape): string {
    const filter = entry.filter as {operation?: string; value?: unknown};
    switch (filter.operation) {
        case "IS_NULL": return intl.formatMessage(filterMessages.valueNull);
        case "NOT_NULL": return intl.formatMessage(filterMessages.valueNotNull);
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
    const intl = useIntl();
    const {formatMessage} = intl;

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
            {operator(filter)} {formatMessage(findColDefByField(filter.field).message)} : {value(filter, intl)}
        </Tag>
    );
}

export default AipFilterTag;
