import { Menu, MenuButton, MenuItem, MenuList, MenuPopover, MenuTrigger, TagGroup, makeStyles } from "@fluentui/react-components";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { useThunkDispatch } from "utils/hooks";
import {Icon} from 'components/shared';
import "./AipFilter.scss";
import { colDef, generateUUID } from "../utils";
import { useEffect, useState } from "react";
import AipFilterTag from "./AipFilterTag";
import { AREA_AIPS, aipsFilter } from "actions/aip/aip";
import { AipFilter } from "typings/store";
import AipStringFilterForm from "./forms/AipStringFilterForm";
import AipEnumFilterForm from "./forms/AipEnumFilterForm";
import AipRefFilterForm from "./forms/AipRefFilterForm";
import AipNumericFilterForm from "./forms/AipNumericFilterForm";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { QueueItemState } from "api/QueueItemState";

const AipFilters = () => {
	const {filter} = useSelector((state: any) => storeFromArea(state, AREA_AIPS));
    const [filters, setFilters] = useState<AipFilter[]>([]);
    const dispatch = useThunkDispatch();
	const classes = useStyles();

    const handleClose = () => {
        dispatch(modalDialogHide());
    }

	useEffect(() => {
		dispatch(aipsFilter(filters, 0, filter.pageSize));
	}, [filters]);
	
	const handleCreate = (filter: AipFilter) => {
		handleClose();
		const oldFilters = filters;
		filter.id = generateUUID();
		setFilters([...oldFilters, filter]);
	}

	const handleRemove = (_e, { value }) => {
		setFilters(filters.filter((item) => item.id != value));
	}

	const getForm = (item) => {
		switch(item.type){
			case "date":
			case "number": return (
				<AipNumericFilterForm 
					item={item} 
					onClose={handleClose}
					onSubmit={handleCreate}
				/>);
			case "string": return (
				<AipStringFilterForm
					item={item} 
					onClose={handleClose}
					onSubmit={handleCreate}
				/>);
			case "ref": {
				return (
				<AipRefFilterForm
					item={item} 
					onClose={handleClose}
					onSubmit={handleCreate}
				/>)}
			case "bool" : return (
				<AipEnumFilterForm
					item={item} 
					onClose={handleClose}
					onSubmit={handleCreate}
					selectValues={[{label: "ANO", value: true}, {label: "NE", value: false}]}
				/>)
			case "enum": return (
				<AipEnumFilterForm
					item={item} 
					onClose={handleClose}
					onSubmit={handleCreate}
					selectValues={[
						{label: "Chyba", value: QueueItemState.ERROR},
						{label: "Ke stažení", value: QueueItemState.IMPORT_NEW},
						{label: "Aktualizováno/Staženo", value: QueueItemState.IMPORT_OK},
						{label: "K aktualizaci", value: QueueItemState.UPDATE}
					]}
				/>)
			default: return <></>
		}
	}

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
				<MenuTrigger disableButtonEnhancement>
					<MenuButton
						menuIcon={<Icon glyph="fa-filter"/>}     
						shape="square"
						className="filter-btn"
					>
					</MenuButton>
				</MenuTrigger>

				<MenuPopover className={classes.menuPopover}>
				<MenuList>
					{Object.keys(colDef).map(key => (
						<MenuItem  
							className={classes.menuItem}
							onClick={() => handleFilterCreate(colDef[key])}
						>
							{colDef[key].name}
						</MenuItem>
					))}
				</MenuList>
				</MenuPopover>
			</Menu>
			
			<TagGroup onDismiss={handleRemove} aria-label="Filtry" className="tag-group">
				{filters.map(filter =>(
					<AipFilterTag filter={filter} />
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