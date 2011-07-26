package net.sf.briar.api.protocol.writers;

import java.io.OutputStream;

public interface PacketWriterFactory {

	AckWriter createAckWriter(OutputStream out);

	BatchWriter createBatchWriter(OutputStream out);

	SubscriptionWriter createSubscriptionWriter(OutputStream out);

	TransportWriter createTransportWriter(OutputStream out);
}