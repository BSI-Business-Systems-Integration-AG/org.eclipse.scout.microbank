package org.eclipse.scout.contacts.server.account.model;

import java.math.BigInteger;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Block {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(Block.class);

	private String hash;
	private BigInteger number;
	private Date timestamp;

	public Block() {

	}

	public Block(String hash, BigInteger number, Date timestamp) {
		this.hash = hash;
		this.number = number;
		this.timestamp = timestamp;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public BigInteger getNumber() {
		return number;
	}

	public void setNumber(BigInteger number) {
		this.number = number;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj instanceof Block) {
			String hash = getHash();
			String thatHash = ((Block) obj).getHash();
			return hash.equals(thatHash);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getHash().hashCode();
	}
}
