import {
	Menu,
	MenuTrigger,
	MenuList,
	MenuItemCheckbox,
	MenuPopover,
	MenuButton,
	makeStyles,
} from "@fluentui/react-components";
import type { MenuCheckedValueChangeData, MenuCheckedValueChangeEvent } from "@fluentui/react-components";
import { colDef } from "../utils";
import { Icon, i18n } from "components/shared";
import "../AipDetail.scss";
import classNames from 'classnames';

type AipListColSelectorProps = {
	columns: string[];
	onChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
	className?: string;
};

const AipListColSelector = ({columns, onChange, ...props} : AipListColSelectorProps) => {
	const classes = useStyles();

	return (
		<Menu 
			checkedValues={{ col: columns }} 
			onCheckedValueChange={onChange}
		>
			<MenuTrigger disableButtonEnhancement>
				<MenuButton 
					menuIcon={null}
					shape="square"
					{...props}
				>
					<span>{i18n("aip.filter.columns")}  <Icon glyph="fa-caret-down"/></span>
				</MenuButton>
			</MenuTrigger>
			<MenuPopover className={classNames(classes.bg, classes.menuPopover)}>
				<MenuList >
					{Object.keys(colDef).map((key) => 
						<MenuItemCheckbox 
							name="col" 
							key={`selector${key}`}
							value={colDef[key].name} 
							className={classNames(classes.bg, classes.menuItem)}
						>
								{colDef[key].name}
						</MenuItemCheckbox>
					)}
				</MenuList>
			</MenuPopover>
		</Menu>
	);
}

const useStyles = makeStyles({
	bg: {
		backgroundColor: "#e3e3e3ff", 
	},
	menuPopover: {
		borderRadius: 0, 
		padding: 0
	},
	menuItem: {
		padding: "5px"
	}
});

export default AipListColSelector;