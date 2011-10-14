package net.sf.briar.transport.batch;

import net.sf.briar.api.ContactId;
import net.sf.briar.api.TransportId;
import net.sf.briar.api.db.DatabaseComponent;
import net.sf.briar.api.protocol.ProtocolReaderFactory;
import net.sf.briar.api.protocol.writers.ProtocolWriterFactory;
import net.sf.briar.api.transport.BatchConnectionFactory;
import net.sf.briar.api.transport.BatchTransportReader;
import net.sf.briar.api.transport.BatchTransportWriter;
import net.sf.briar.api.transport.ConnectionReaderFactory;
import net.sf.briar.api.transport.ConnectionWriterFactory;

import com.google.inject.Inject;

class BatchConnectionFactoryImpl implements BatchConnectionFactory {

	private final ConnectionReaderFactory connReaderFactory;
	private final ConnectionWriterFactory connWriterFactory;
	private final DatabaseComponent db;
	private final ProtocolReaderFactory protoReaderFactory;
	private final ProtocolWriterFactory protoWriterFactory;

	@Inject
	BatchConnectionFactoryImpl(ConnectionReaderFactory connReaderFactory,
			ConnectionWriterFactory connWriterFactory, DatabaseComponent db,
			ProtocolReaderFactory protoReaderFactory,
			ProtocolWriterFactory protoWriterFactory) {
		this.connReaderFactory = connReaderFactory;
		this.connWriterFactory = connWriterFactory;
		this.db = db;
		this.protoReaderFactory = protoReaderFactory;
		this.protoWriterFactory = protoWriterFactory;
	}

	public void createIncomingConnection(ContactId c,
			BatchTransportReader r, byte[] encryptedIv) {
		final IncomingBatchConnection conn = new IncomingBatchConnection(
				connReaderFactory, db, protoReaderFactory, c, r, encryptedIv);
		Runnable read = new Runnable() {
			public void run() {
				conn.read();
			}
		};
		new Thread(read).start();
	}

	public void createOutgoingConnection(TransportId t, ContactId c,
			BatchTransportWriter w) {
		final OutgoingBatchConnection conn = new OutgoingBatchConnection(
				connWriterFactory, db, protoWriterFactory, t, c, w);
		Runnable write = new Runnable() {
			public void run() {
				conn.write();
			}
		};
		new Thread(write).start();
	}
}