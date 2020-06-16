export enum SettingsType {
    /**
     * Používá klient pro čtení režimu fondu (read-only vs write)
     */
    FUND_READ_MODE = 'FUND_READ_MODE',

    /**
     * Používá klient pro čtení stavu pravého panelu
     */
    FUND_RIGHT_PANEL = 'FUND_RIGHT_PANEL',

    /**
     * Používá klient pro čtení režimu zobrazení (potomci, předci)
     */
    FUND_CENTER_PANEL = 'FUND_CENTER_PANEL',

    /**
     * nastavení strictního módu pro uživatele (přepíše nastavení pravidel)
     */
    FUND_STRICT_MODE = 'FUND_STRICT_MODE',

    /**
     * uživatelské šablony JP
     */
    FUND_TEMPLATES = 'FUND_TEMPLATES',

    /**
     * oblíbené specifikace u typu atributu
     */
    FAVORITE_ITEM_SPECS = 'FAVORITE_ITEM_SPECS',

    /**
     * Připnutí sekcí osob
     */
    PARTY_PIN = 'PARTY_PIN',

    /**
     * Zobrazení popisků archivních souborů.
     */
    FUND_VIEW = 'FUND_VIEW',

    /**
     * Zobrazení skupin typů atributů v archivním souboru.
     */
    TYPE_GROUPS = 'TYPE_GROUPS',

    /**
     * Zobrazení skupin typů atributů v archivním souboru.
     */
    STRUCTURE_TYPES = 'STRUCTURE_TYPES',

    /**
     * Settings for given structured type
     *
     * Optional settings are defined for
     * given structured type and eventually for fund.
     *
     * Settings is saved as 'STRUCT_TYPE_<CODE>`
     */
    STRUCT_TYPE_ = 'STRUCT_TYPE_',

    /**
     * Výchozí nastavení pro rejstříky.
     */
    RECORD = 'RECORD',

    /**
     * Nastavení sloupců / atributů pro zobrazení v gridu
     */
    GRID_VIEW = 'GRID_VIEW',

    /**
     * Nastavení barev a ikon připomínek.
     */
    FUND_ISSUES = 'FUND_ISSUES',

    /**
     * Nastavení pořadí částí v detailu přístupového bodu.
     */
    PARTS_ORDER = 'PARTS_ORDER',

    /**
     * Nastavení atributů dle části v detailu přístupového bodu.
     */
    ITEM_TYPES = 'ITEM_TYPES',
}
