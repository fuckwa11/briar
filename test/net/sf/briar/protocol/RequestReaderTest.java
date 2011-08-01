package net.sf.briar.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.BitSet;

import junit.framework.TestCase;
import net.sf.briar.api.protocol.Request;
import net.sf.briar.api.protocol.Tags;
import net.sf.briar.api.serial.FormatException;
import net.sf.briar.api.serial.Reader;
import net.sf.briar.api.serial.ReaderFactory;
import net.sf.briar.api.serial.Writer;
import net.sf.briar.api.serial.WriterFactory;
import net.sf.briar.serial.SerialModule;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class RequestReaderTest extends TestCase {

	private final ReaderFactory readerFactory;
	private final WriterFactory writerFactory;
	private final Mockery context;

	public RequestReaderTest() throws Exception {
		super();
		Injector i = Guice.createInjector(new SerialModule());
		readerFactory = i.getInstance(ReaderFactory.class);
		writerFactory = i.getInstance(WriterFactory.class);
		context = new Mockery();
	}

	@Test
	public void testFormatExceptionIfRequestIsTooLarge() throws Exception {
		RequestFactory requestFactory = context.mock(RequestFactory.class);
		RequestReader requestReader = new RequestReader(requestFactory);

		byte[] b = createRequest(true);
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		Reader reader = readerFactory.createReader(in);
		reader.addObjectReader(Tags.REQUEST, requestReader);

		try {
			reader.readUserDefined(Tags.REQUEST, Request.class);
			assertTrue(false);
		} catch(FormatException expected) {}
		context.assertIsSatisfied();
	}

	@Test
	public void testNoFormatExceptionIfRequestIsMaximumSize() throws Exception {
		final RequestFactory requestFactory =
			context.mock(RequestFactory.class);
		RequestReader requestReader = new RequestReader(requestFactory);
		final Request request = context.mock(Request.class);
		context.checking(new Expectations() {{
			oneOf(requestFactory).createRequest(with(any(BitSet.class)));
			will(returnValue(request));
		}});

		byte[] b = createRequest(false);
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		Reader reader = readerFactory.createReader(in);
		reader.addObjectReader(Tags.REQUEST, requestReader);

		assertEquals(request, reader.readUserDefined(Tags.REQUEST,
				Request.class));
		context.assertIsSatisfied();
	}

	@Test
	public void testBitmapDecoding() throws Exception {
		// Test sizes from 0 to 1000 bits
		for(int i = 0; i < 1000; i++) {
			// Create a BitSet of size i with one in ten bits set (on average)
			BitSet requested = new BitSet(i);
			for(int j = 0; j < i; j++) if(Math.random() < 0.1) requested.set(j);
			// Encode the BitSet as a bitmap
			int bytes = i % 8 == 0 ? i / 8 : i / 8 + 1;
			byte[] bitmap = new byte[bytes];
			for(int j = 0; j < i; j++) {
				if(requested.get(j)) {
					int offset = j / 8;
					byte bit = (byte) (128 >> j % 8);
					bitmap[offset] |= bit;
				}
			}
			// Create a serialised request containing the bitmap
			byte[] b = createRequest(bitmap);
			// Deserialise the request
			ByteArrayInputStream in = new ByteArrayInputStream(b);
			Reader reader = readerFactory.createReader(in);
			RequestReader requestReader =
				new RequestReader(new RequestFactoryImpl());
			reader.addObjectReader(Tags.REQUEST, requestReader);
			Request r = reader.readUserDefined(Tags.REQUEST, Request.class);
			BitSet decoded = r.getBitmap();
			// Check that the decoded BitSet matches the original - we can't
			// use equals() because of padding, but the first i bits should
			// match and the cardinalities should be equal, indicating that no
			// padding bits are set
			for(int j = 0; j < i; j++) {
				assertEquals(requested.get(j), decoded.get(j));
			}
			assertEquals(requested.cardinality(), decoded.cardinality());
		}
	}

	private byte[] createRequest(boolean tooBig) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer w = writerFactory.createWriter(out);
		w.writeUserDefinedTag(Tags.REQUEST);
		// Allow one byte for the REQUEST tag, one byte for the BYTES tag, and
		// five bytes for the length as an int32
		if(tooBig) w.writeBytes(new byte[Request.MAX_SIZE - 6]);
		else w.writeBytes(new byte[Request.MAX_SIZE - 7]);
		assertEquals(tooBig, out.size() > Request.MAX_SIZE);
		return out.toByteArray();
	}

	private byte[] createRequest(byte[] bitmap) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer w = writerFactory.createWriter(out);
		w.writeUserDefinedTag(Tags.REQUEST);
		w.writeBytes(bitmap);
		return out.toByteArray();
	}
}