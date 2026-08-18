import {
	Menu,
	MenuTrigger,
	MenuList,
	MenuItemCheckbox,
	MenuPopover,
	MenuButton,
	makeStyles,
	mergeClasses
} from "@fluentui/react-components";
import type { MenuCheckedValueChangeData, MenuCheckedValueChangeEvent } from "@fluentui/react-components";
import { colDef } from "../utils";
import { useIntl } from "react-intl";
import { Icon, i18n } from "components/shared";
import "../AipDetail.scss";


type AipListColSelectorProps = {
	columns: string[];
	onChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
	className?: string;
	hiddenValues?: string[];
};

const AipListColSelector = ({columns, onChange, hiddenValues, ...props} : AipListColSelectorProps) => {
	const classes = useStyles();
	const {formatMessage} = useIntl();
	const columnsDef = colDef.filter(col => !hiddenValues?.includes(col.key));

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
			<MenuPopover className={mergeClasses(classes.bg, classes.menuPopover)}>
				<MenuList>
					{Object.keys(columnsDef).map((key) => 
						<MenuItemCheckbox 
							name="col" 
							key={`selector${key}`}
							value={formatMessage(columnsDef[key].message)} 
							className={mergeClasses(classes.bg, classes.menuItem)}
						>
							{formatMessage(columnsDef[key].message)}
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