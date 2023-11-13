import {EntityType} from "./EntityType";
import {SettingsType} from "./settings/SettingsType";

export interface UISettingsVO {
    id: number;
    settingsType: SettingsType;
    entityType: EntityType;
    entityId: number;
    value: string;
}
