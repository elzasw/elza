package cz.tacr.elza.common;

public class TextUtils {

	/**
	 *  Nahrazení znaků pro posun řádku:
	 *  \r\n → \n
     *  \r   → \n
	 *
	 * @param original
	 * @return
	 */
	public static String normalizeText(String original) {
		return original.replaceAll("\\r\\n?", "\n");
	}
}
