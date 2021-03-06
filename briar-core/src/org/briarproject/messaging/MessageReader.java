package org.briarproject.messaging;

import static org.briarproject.api.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.briarproject.api.messaging.MessagingConstants.MAX_BODY_LENGTH;
import static org.briarproject.api.messaging.MessagingConstants.MAX_CONTENT_TYPE_LENGTH;
import static org.briarproject.api.messaging.MessagingConstants.MAX_PACKET_LENGTH;
import static org.briarproject.api.messaging.MessagingConstants.MESSAGE_SALT_LENGTH;
import static org.briarproject.api.messaging.Types.MESSAGE;

import java.io.IOException;

import org.briarproject.api.Author;
import org.briarproject.api.FormatException;
import org.briarproject.api.UniqueId;
import org.briarproject.api.messaging.Group;
import org.briarproject.api.messaging.MessageId;
import org.briarproject.api.messaging.UnverifiedMessage;
import org.briarproject.api.serial.CopyingConsumer;
import org.briarproject.api.serial.CountingConsumer;
import org.briarproject.api.serial.Reader;
import org.briarproject.api.serial.StructReader;

class MessageReader implements StructReader<UnverifiedMessage> {

	private final StructReader<Group> groupReader;
	private final StructReader<Author> authorReader;

	MessageReader(StructReader<Group> groupReader,
			StructReader<Author> authorReader) {
		this.groupReader = groupReader;
		this.authorReader = authorReader;
	}

	public UnverifiedMessage readStruct(Reader r) throws IOException {
		CopyingConsumer copying = new CopyingConsumer();
		CountingConsumer counting = new CountingConsumer(MAX_PACKET_LENGTH);
		r.addConsumer(copying);
		r.addConsumer(counting);
		// Read the start of the struct
		r.readStructStart(MESSAGE);
		// Read the parent's message ID, if there is one
		MessageId parent = null;
		if(r.hasNull()) {
			r.readNull();
		} else {
			byte[] b = r.readBytes(UniqueId.LENGTH);
			if(b.length < UniqueId.LENGTH) throw new FormatException();
			parent = new MessageId(b);
		}
		// Read the group
		Group group = groupReader.readStruct(r);
		// Read the author, if there is one
		Author author = null;
		if(r.hasNull()) r.readNull();
		else author = authorReader.readStruct(r);
		// Read the content type
		String contentType = r.readString(MAX_CONTENT_TYPE_LENGTH);
		// Read the timestamp
		long timestamp = r.readInteger();
		if(timestamp < 0) throw new FormatException();
		// Read the salt
		byte[] salt = r.readBytes(MESSAGE_SALT_LENGTH);
		if(salt.length < MESSAGE_SALT_LENGTH) throw new FormatException();
		// Read the message body
		byte[] body = r.readBytes(MAX_BODY_LENGTH);
		// Record the offset of the body within the message
		int bodyStart = (int) counting.getCount() - body.length;
		// Record the length of the data covered by the author's signature
		int signedLength = (int) counting.getCount();
		// Read the author's signature, if there is one
		byte[] signature = null;
		if(author == null) r.readNull();
		else signature = r.readBytes(MAX_SIGNATURE_LENGTH);
		// Read the end of the struct
		r.readStructEnd();
		// The signature will be verified later
		r.removeConsumer(counting);
		r.removeConsumer(copying);
		byte[] raw = copying.getCopy();
		return new UnverifiedMessage(parent, group, author, contentType,
				timestamp, raw, signature, bodyStart, body.length,
				signedLength);
	}
}
