import { Menu, MenuButton, MenuItem, MenuList, MenuPopover, MenuTrigger, TagGroup, makeStyles } from "@fluentui/react-components";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { useThunkDispatch } from "utils/hooks";
import {Icon} from 'components/shared';
import "./AipFilter.scss";
import { AipColumn, colDef } from "../columns";
import { useEffect, useState } from "react";
import AipFilterTag from "./AipFilterTag";
import { AREA_AIPS, aipsFilter } from "actions/aip/aip";
import { AipFilterEntry } from "typings/store";
import { AipFilterForm } from "./forms/AipFilterForm";
import { useIntl } from "react-intl";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import {QueueItemState} from "elza-api";

type AipFiltersProps = {
	filterDisabled: boolean;
	initialFilters?: AipFilterEntry[];
	hiddenValues?: string[];
	filters: AipFilterEntry[]
	createFilter: (filter: AipFilterEntry) => void;
	removeFilter: (id: string) => void;
}

const AipFilters = ({filterDisabled, hiddenValues, filters, createFilter, removeFilter}: AipFiltersProps) => {
	const {filter} = useSelector((state: any) => storeFromArea(state, AREA_AIPS));
	const columnsDef = colDef.filter(col => !hiddenValues?.includes(col.key));
    const dispatch = useThunkDispatch();
	const classes = useStyles();
	const {formatMessage} = useIntl();


    const handleClose = () => {
        dispatch(modalDialogHide());
    }

	useEffect(() => {
		dispatch(aipsFilter(filters, 0, filter.pageSize));
	}, [filters]);

	const handleCreate = (filter: AipFilterEntry) => {
		handleClose();
		createFilter(filter);
	}

	const handleRemove = (_e, { value }) => {
		removeFilter(value);
	}

	const getForm = (item: AipColumn) => (
		<AipFilterForm item={item} onClose={handleClose} onSubmit={handleCreate}/>
	);

    const handleFilterCreate = (item) => {
        dispatch(
            modalDialogShow(
                this,
                `Vytvořit filtr ${item.name}`,
				getForm(item),
                null,
            ),
        );
    }

    return (
		<div className="filters">
			<Menu>
				<MenuTrigger disableButtonEnhancement >
					<MenuButton
						menuIcon={<Icon glyph="fa-filter"/>}
						shape="square"
						disabled={filterDisabled}
						className="filter-btn"
					>
					</MenuButton>
				</MenuTrigger>

				<MenuPopover className={classes.menuPopover}>
				<MenuList>
					{Object.keys(columnsDef).map((key, index) => (
						<MenuItem
							key={`filter-${index}`}
							className={classes.menuItem}
							onClick={() => handleFilterCreate(columnsDef[key])}
						>
							{formatMessage(columnsDef[key].message)}
						</MenuItem>
					))}
				</MenuList>
				</MenuPopover>
			</Menu>

			<TagGroup onDismiss={filterDisabled ? undefined : handleRemove} aria-label="Filtry" className="tag-group" >
				{filters.map(filter => (
					<AipFilterTag key={filter.id} filter={filter} />
				))}
			</TagGroup>

		</div>
    );
}

const useStyles = makeStyles({
	menuItem: {
		backgroundColor: "#e3e3e3ff",
		padding: "5px"
	},
	menuPopover: {
		backgroundColor: "#e3e3e3ff",
		borderRadius: "0",
		padding: "0"
	},
  });

export default AipFilters;
