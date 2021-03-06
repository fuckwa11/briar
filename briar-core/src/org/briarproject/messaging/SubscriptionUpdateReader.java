package org.briarproject.messaging;

import static org.briarproject.api.messaging.MessagingConstants.MAX_PACKET_LENGTH;
import static org.briarproject.api.messaging.MessagingConstants.MAX_SUBSCRIPTIONS;
import static org.briarproject.api.messaging.Types.SUBSCRIPTION_UPDATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.briarproject.api.FormatException;
import org.briarproject.api.messaging.Group;
import org.briarproject.api.messaging.GroupId;
import org.briarproject.api.messaging.SubscriptionUpdate;
import org.briarproject.api.serial.Consumer;
import org.briarproject.api.serial.CountingConsumer;
import org.briarproject.api.serial.Reader;
import org.briarproject.api.serial.StructReader;

class SubscriptionUpdateReader implements StructReader<SubscriptionUpdate> {

	private final StructReader<Group> groupReader;

	SubscriptionUpdateReader(StructReader<Group> groupReader) {
		this.groupReader = groupReader;
	}

	public SubscriptionUpdate readStruct(Reader r) throws IOException {
		// Set up the reader
		Consumer counting = new CountingConsumer(MAX_PACKET_LENGTH);
		r.addConsumer(counting);
		// Read the start of the struct
		r.readStructStart(SUBSCRIPTION_UPDATE);
		// Read the subscriptions, rejecting duplicates
		List<Group> groups = new ArrayList<Group>();
		Set<GroupId> ids = new HashSet<GroupId>();
		r.readListStart();
		for(int i = 0; i < MAX_SUBSCRIPTIONS && !r.hasListEnd(); i++) {
			Group g = groupReader.readStruct(r);
			if(!ids.add(g.getId())) throw new FormatException(); // Duplicate
			groups.add(g);
		}
		r.readListEnd();
		// Read the version number
		long version = r.readInteger();
		if(version < 0) throw new FormatException();
		// Read the end of the struct
		r.readStructEnd();
		// Reset the reader
		r.removeConsumer(counting);
		// Build and return the subscription update
		groups = Collections.unmodifiableList(groups);
		return new SubscriptionUpdate(groups, version);
	}
}
