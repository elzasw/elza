package com.lightcomp.kads.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

/**
 * Adapter converts type xs:anyURI from/to string.
 */
public abstract class AnyUriAdapter extends XmlAdapter<String, String> {
	
	private static Logger log = LoggerFactory.getLogger(AnyUriAdapter.class);
	
	private static boolean legacyDefault = false;
	
	public static boolean isLegacyDefault() {
		return legacyDefault;
	}
	
	public static void setLegacyDefault(boolean legacyDefault) {
		AnyUriAdapter.legacyDefault = legacyDefault;
		log.warn("AnyUriAdapter legacyDefault set to {}", legacyDefault);
	}

    public static void register(Marshaller m, boolean legacy) {
        m.setAdapter(AnyUriAdapter.class, legacy ? new LegacyAdapter() : new EncodingAdapter());
    }

    public static void register(Unmarshaller unm, boolean legacy) {
        unm.setAdapter(AnyUriAdapter.class, legacy ? new LegacyAdapter() : new EncodingAdapter());
    }

    public static class LegacyAdapter extends AnyUriAdapter {

        @Override
        public String unmarshal(String v) throws Exception {
            return v;
        }

        @Override
        public String marshal(String v) throws Exception {
            return v;
        }
    }

    public static class EncodingAdapter extends AnyUriAdapter {

        @Override
        public String unmarshal(String v) throws Exception {
            return uriDecodeUtf8(v);
        }

        @Override
        public String marshal(String v) throws Exception {
            return UriUtils.encodePath(v, StandardCharsets.UTF_8);
        }
    }

    /**
     * Decode URI component
     * @param uri to decode
     * @return String
     */
	public static String decodeUri(String uri) {
		return UriUtils.decode(uri, StandardCharsets.UTF_8.name());
	}

    /**
     * Dekoduje URI path, zachovava znaky s diakritikou
     * @param source string k dekodovani 
     * @param charset znakova sada
     * @return String
     * 
     * Vychazi z UriUtils ze springu
     * 
     * Vyhodi IllegalArgumentException pokud je navalidni zapis znaku s procenty
     * 
     * Pokud neni v retezci znak % je vracen nemodifikovany
     * 
     * !!! Nemelo by se pouzivat protoze obchazi standard
     */
    public static String uriDecode(String source, Charset charset) {
		int i = source.indexOf('%');
		if (i == -1) {
			return source;
		}
		int length = source.length();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		int s = 0;
		try {
			while (i != -1) {
				bos.write(source.substring(s, i).getBytes(charset));
				if (i + 2 < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
					bos.write((char) ((u << 4) + l));	
				} else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}				
				s = i + 3;
				i = source.indexOf('%', s);
			}
			if (s < length) {
				bos.write(source.substring(s).getBytes(charset));
			}
		} catch (IOException ioEx) {
			throw new IllegalArgumentException("Invalid encoded sequence \"" + source + "\"");
		}
		return new String(bos.toByteArray(), charset);
	}

    public static String uriDecodeUtf8(String source) {
    	return uriDecode(source, StandardCharsets.UTF_8);
    }

    
}