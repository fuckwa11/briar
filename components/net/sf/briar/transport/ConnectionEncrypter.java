package net.sf.briar.transport;

import java.io.IOException;
import java.io.OutputStream;

/** Encrypts authenticated data to be sent over a connection. */
interface ConnectionEncrypter {

	/** Returns an output stream to which unencrypted data can be written. */
	OutputStream getOutputStream();

	/** Encrypts and writes the MAC for the current frame. */
	void writeMac(byte[] mac) throws IOException;
}