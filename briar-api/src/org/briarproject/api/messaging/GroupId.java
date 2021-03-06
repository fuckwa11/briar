package org.briarproject.api.messaging;

import java.util.Arrays;

import org.briarproject.api.UniqueId;

/**
 * Type-safe wrapper for a byte array that uniquely identifies a {@link Group}.
 */
public class GroupId extends UniqueId {

	public GroupId(byte[] id) {
		super(id);
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof GroupId)
			return Arrays.equals(id, ((GroupId) o).id);
		return false;
	}
}
