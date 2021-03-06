package org.briarproject.api.system;

/**
 * Uses a platform-specific source to provide a seed for a pseudo-random
 * number generator.
 */
public interface SeedProvider {

	int SEED_BYTES = 32;

	byte[] getSeed();
}
