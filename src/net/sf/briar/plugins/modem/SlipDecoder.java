package net.sf.briar.plugins.modem;

import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.util.logging.Logger;

class SlipDecoder implements ReadHandler {

	private static final Logger LOG =
			Logger.getLogger(SlipDecoder.class.getName());

	// https://tools.ietf.org/html/rfc1055
	private static final byte END = (byte) 192, ESC = (byte) 219;
	private static final byte TEND = (byte) 220, TESC = (byte) 221;

	private final ReadHandler readHandler;

	private byte[] buf = new byte[Data.MAX_LENGTH];
	private int decodedLength = 0;
	private boolean escape = false;

	SlipDecoder(ReadHandler readHandler) {
		this.readHandler = readHandler;
	}

	public void handleRead(byte[] b, int length) throws IOException {
		for(int i = 0; i < length; i++) {
			switch(b[i]) {
			case END:
				if(escape) {
					reset(true);
				} else {
					if(decodedLength > 0) {
						readHandler.handleRead(buf, decodedLength);
						buf = new byte[Data.MAX_LENGTH];
					}
					reset(false);
				}
				break;
			case ESC:
				if(escape) reset(true);
				else escape = true;
				break;
			case TEND:
				if(escape) {
					escape = false;
					if(decodedLength == buf.length) reset(true);
					else buf[decodedLength++] = END;
				} else {
					if(decodedLength == buf.length) reset(true);
					else buf[decodedLength++] = TEND;
				}
				break;
			case TESC:
				if(escape) {
					escape = false;
					if(decodedLength == buf.length) reset(true);
					else buf[decodedLength++] = ESC;
				} else {
					if(decodedLength == buf.length) reset(true);
					else buf[decodedLength++] = TESC;
				}
				break;
			default:
				if(escape || decodedLength == buf.length) reset(true);
				else buf[decodedLength++] = b[i];
				break;
			}
		}
	}

	private void reset(boolean error) {
		if(error) {
			if(LOG.isLoggable(FINE))
				LOG.fine("Decoding error after " + decodedLength + " bytes");
		}
		escape = false;
		decodedLength = 0;
	}
}
