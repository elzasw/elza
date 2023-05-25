package cz.tacr.elza.common.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FilterInputStreamWithException extends FilterInputStream {
	
	protected Exception except;

	public FilterInputStreamWithException(InputStream in) {
		super(in);
	}
	
	private void checkException() throws IOException {
        if (except != null) {
            //Exception thrown by the writer will bubble up to InputStream reader
            throw new IOException("IOException in writer: "+except.getMessage(), except);
        }		
	}

	public void setException(Exception e) {
		except = e;
	}
	
	@Override
	public int read() throws IOException {
		int i = super.read();
		checkException();
		return i;
	}
	
	@Override
	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);		
	}
	
	@Override
	public int read(byte b[], int off, int len) throws IOException {
		int i = super.read(b, off, len);
		checkException();
		return i;
	}
	
	@Override
    public void close() throws IOException {
    	  try {
    		  checkException();
          } finally {
              super.close();
          }    	
    }	
}
