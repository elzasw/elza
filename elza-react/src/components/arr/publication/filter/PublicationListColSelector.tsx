import {
    Menu,
    MenuTrigger,
    MenuList,
    MenuItemCheckbox,
    MenuPopover,
    MenuButton,
} from "@fluentui/react-components";
import type { MenuCheckedValueChangeData, MenuCheckedValueChangeEvent } from "@fluentui/react-components";
import { ChevronDownRegular } from "@fluentui/react-icons";
import { colDef } from "../columns";
import { tableMessages } from "components/shared/lang/tableMessages";
import { useIntl } from "react-intl";

type PublicationListColSelectorProps = {
    columns: string[];
    onChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
};

const PublicationListColSelector = ({ columns, onChange }: PublicationListColSelectorProps) => {
    const { formatMessage } = useIntl();

    return (
        <Menu
            checkedValues={{ col: columns }}
            onCheckedValueChange={onChange}
        >
            <MenuTrigger disableButtonEnhancement>
                <MenuButton
                    menuIcon={<ChevronDownRegular />}
                >
                    {formatMessage(tableMessages.columns)}
                </MenuButton>
            </MenuTrigger>
            <MenuPopover>
                <MenuList>
                    {colDef.map((col) => {
                        const label = formatMessage(col.message);
                        return (
                            <MenuItemCheckbox
                                name="col"
                                key={col.key}
                                value={label}
                            >
                                {label}
                            </MenuItemCheckbox>
                        );
                    })}
                </MenuList>
            </MenuPopover>
        </Menu>
    );
};

export default PublicationListColSelector;
