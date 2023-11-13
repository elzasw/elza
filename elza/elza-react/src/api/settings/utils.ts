import { UISettingsVO } from 'api/UISettingsVO';
import { getOneSettings } from '../../components/arr/ArrUtils';
import { MenuOption, MenuOptions } from './MenuOption';

export const isMenuItemHidden = (userSettings: UISettingsVO[], option: MenuOptions) => {
    const menuSettings = getOneSettings(userSettings, 'MENU');
    const menuSettingsValue = menuSettings.value ? JSON.parse(menuSettings.value) : null;
    const menuOptions = menuSettingsValue?.options || [];
    const menuItem = menuOptions.find((item: MenuOption) => item.name === option);
    if (menuItem) {
        return menuItem.value === 'true' ? true : false;
    }
    return false;
};
