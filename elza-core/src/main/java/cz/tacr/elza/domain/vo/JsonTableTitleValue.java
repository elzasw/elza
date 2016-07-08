package cz.tacr.elza.domain.vo;

/**
 * Objekt s rozšiřujícími daty - informace o počtu řásků a sloupců v tabulce.
 *
 * @author Pavel Stánek
 * @since 23.06.2016
 */
public class JsonTableTitleValue extends TitleValue {
    /** Počet řádek v tabulce. */
    private int rows;

    public JsonTableTitleValue() {
    }

    public JsonTableTitleValue(final String value, final int rows) {
        super(value);
        this.rows = rows;
    }

    public int getRows() {
        return rows;
    }
}