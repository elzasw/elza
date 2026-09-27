import {
	Menu,
	MenuTrigger,
	MenuList,
	MenuItemCheckbox,
	MenuPopover,
	MenuButton,
} from "@fluentui/react-components";
import { TableSettingsRegular } from "@fluentui/react-icons";
import type { MenuCheckedValueChangeData, MenuCheckedValueChangeEvent } from "@fluentui/react-components";
import { colDef } from "../columns";
import { useIntl } from "react-intl";
import { tableMessages } from "components/shared/lang/tableMessages";


type AipListColSelectorProps = {
	columns: string[];
	onChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
	className?: string;
	hiddenValues?: string[];
};

const AipListColSelector = ({columns, onChange, hiddenValues, ...props} : AipListColSelectorProps) => {
	const {formatMessage} = useIntl();
	const columnsDef = colDef.filter(col => !hiddenValues?.includes(col.key));

	return (
		<Menu 
			checkedValues={{ col: columns }} 
			onCheckedValueChange={onChange}
		>
			<MenuTrigger disableButtonEnhancement>
				<MenuButton icon={<TableSettingsRegular />} {...props}>
					{formatMessage(tableMessages.columns)}
				</MenuButton>
			</MenuTrigger>
			<MenuPopover>
				<MenuList>
					{columnsDef.map((column) =>
						<MenuItemCheckbox 
							name="col" 
							key={`selector-${column.field}`}
							value={formatMessage(column.message)} 
						>
							{formatMessage(column.message)}
						</MenuItemCheckbox>
					)}
				</MenuList>
			</MenuPopover>
		</Menu>
	);
}

export default AipListColSelector;