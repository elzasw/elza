package cz.tacr.elza.utils;

/**
 * Nástroje pro ověřovací schéma
 * @author Sergey Iryupin (sergey.iryupin)
 * @since 09.09.2020
 */
public class SchemaUtils {

	/**
	 * Získat cestu k souboru podle URL schématu
	 * @param url schématu
	 * @return cesta k souboru
	 */
	public static final String SchemaUrlToFile(String url) {
		switch (url) {
		case "http://ead3.archivists.org/schema/":
			return "/schema/ead3.xsd";
		default:
			return null;
		}
	}

}
