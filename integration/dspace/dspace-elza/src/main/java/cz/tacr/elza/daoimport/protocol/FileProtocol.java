package cz.tacr.elza.daoimport.protocol;

import java.io.BufferedWriter;
import java.io.IOException;

public class FileProtocol implements Protocol {
	final BufferedWriter writer;

	public FileProtocol(final BufferedWriter writer) {
		this.writer = writer;
	}

	@Override
	public void add(String message) {
		try {
			writer.append(message);
			writer.newLine();
		} catch (IOException e) {
			throw new IllegalStateException("Chyba při zápisu protkolu. Zpráva: "+message, e);
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();		
	}
}
