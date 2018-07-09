package com.chen;

import java.nio.charset.Charset;
import java.security.*;

class BucketChooser {
	private int numBucket;
	private final int[] cutoffs;
	private static final long INT_RANGE = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE;
	private MessageDigest mDigest;

	public static BucketChooser create(int numBucket) {
		return new BucketChooser(numBucket);
	}
	
	private BucketChooser(int numBucket) {
		this.cutoffs = constructCutoffArray(numBucket);
		this.numBucket = numBucket;
		this.mDigest = createMessageDigest();
	}

	private int hash(final String cookieID) {
		byte[] bytes = cookieID.getBytes(Charset.forName("utf8"));
		mDigest.update(bytes);
		final byte[] digest = mDigest.digest();
		return convertToInt(digest);
	}

	private static int convertToInt(final byte[] digest) {
		final int offset = 12; // arbitrary choice; changing this would
								// reshuffle all groups
		return (0xff & digest[offset + 0]) << 24 | 
				(0xff & digest[offset + 1]) << 16 | 
				(0xff & digest[offset + 2]) << 8
				| (0xff & digest[offset + 3]);
	}

	private static MessageDigest createMessageDigest() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException("Impossible no MD5", e);
		}
	}

	private static int[] constructCutoffArray(int numBucket) {
		final int[] cutoffs = new int[numBucket - 1];

		double bucketTotal = 0.0;
		for (int i = 0; i < cutoffs.length; ++i) {
			bucketTotal += 1.0 / numBucket;
			cutoffs[i] = (int) (Integer.MIN_VALUE + bucketTotal * INT_RANGE);
		}

		return cutoffs;
	}

	/*
	 * thread safe
	 */
	public synchronized int chooseBucket(final String cookieID) {
		final int value = hash(cookieID);
		int i;
		for (i = 0; i < this.numBucket - 1 && value > this.cutoffs[i]; ++i) {
			/* intentionally empty */ }
		return i;
	}

}