package cz.tacr.elza.domain;

/**
 * Rozšíření {@link RulItemSpec}.
 * 
 */
// This object could be removed?
public class RulItemSpecExt extends RulItemSpec {

	public RulItemSpecExt(RulItemSpec src) {
		super(src);
	}

    /**
     * Set maximum allowed type
     * 
     * Function will lower existing settings
     * 
     * @param type
     */
    public void setTypeMax(final Type type) {
        switch (type) {
        case REQUIRED:
            break;
        case RECOMMENDED:
            if (getType() == Type.REQUIRED) {
                setType(Type.RECOMMENDED);
            }
            break;
        case POSSIBLE:
            if (getType() == Type.RECOMMENDED || getType() == Type.REQUIRED) {
                setType(Type.POSSIBLE);
            }
            break;
        case IMPOSSIBLE:
            if (getType() == Type.POSSIBLE || getType() == Type.RECOMMENDED || getType() == Type.REQUIRED) {
                setType(Type.IMPOSSIBLE);
            }
            break;
        }
    }
}
