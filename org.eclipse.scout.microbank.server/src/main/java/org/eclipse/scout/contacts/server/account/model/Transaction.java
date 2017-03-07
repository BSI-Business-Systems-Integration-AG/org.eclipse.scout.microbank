package org.eclipse.scout.contacts.server.account.model;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.scout.contacts.server.account.EthereumService;
import org.eclipse.scout.rt.platform.BEANS;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.request.RawTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class Transaction {

	/**
	 * gas price: check <a href="https://ethstats.net/">ethstats.net</a> for current values. For additional infor see <a
	 * href="http://ethereum.stackexchange.com/questions/1113/can-i-set-the-gas-price-to-whatever-i-want/1133>ethereum.stackexchange.com</a>
	 */
	public static final BigInteger GAS_PRICE_DEFAULT = BigInteger.valueOf(20_000_000_000L);
	public static final BigInteger GAS_LIMIT_DEFAULT = BigInteger.valueOf(30_000L);
	public static final BigInteger TX_FEE_DEFAULT = GAS_PRICE_DEFAULT.multiply(GAS_LIMIT_DEFAULT);

	public static final int ERROR = -2;
	public static final int REMOVED = -1;
	public static final int UNDEFINED = 0;
	public static final int OFFLINE = 1;
	public static final int PENDING = 2;
	public static final int CONFIRMED = 3;
	public static Map<Integer, String> STATUS = new HashMap<>();
	
	static {
		STATUS.put(ERROR, "Err");
		STATUS.put(REMOVED, "Del");
		STATUS.put(UNDEFINED, "?");
		STATUS.put(OFFLINE, "Off");
		STATUS.put(PENDING, "Pen");
		STATUS.put(CONFIRMED, "Conf");
	}

	/**
	 * tx id's are tricky. that's why we add an artificial id here. used for {@link equals} and {@link hashCode}.
	 */
	private UUID id;
	private String hash;
	private int status;
	private Date created;
	private Date sent;
	private Date confirmed;
	private String fromAddress;
	private String signedContent;
	private Block block;

	private RawTransaction tx;
	private TransactionReceipt receipt;
	private Error error;

	public Transaction(String to, BigInteger amountWei, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit) {
		id = UUID.randomUUID();

		tx = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, amountWei);
		setCreated(new Date());
		status = UNDEFINED;
	}

	public UUID getId() {
		return id;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getSent() {
		return sent;
	}

	public void setSent(Date sent) {
		this.sent = sent;
	}

	public Date getConfirmed() {
		return confirmed;
	}

	public void setConfirmed(Date confirmed) {
		this.confirmed = confirmed;
	}

	public RawTransaction getRawTransaction() {
		return tx;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public String getToAddress() {
		return tx.getTo();
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public String getSignedContent() {
		return signedContent;
	}

	public void setSignedContent(String signedContent) {
		this.signedContent = signedContent;
		this.status = OFFLINE;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public BigInteger getValue() {
		return tx.getValue();
	}

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		if(error != null) {
			status = ERROR;
		}
		
		this.error = error;
	}

	public TransactionReceipt getTransactionReceipt() {
		return receipt;
	}

	public void setTransactionReceipt(TransactionReceipt receipt) {
		if (receipt != null) { 
			String blockHash = receipt.getBlockHash();
			block = BEANS.get(EthereumService.class).getBlock(blockHash);

			if(block != null) {
				confirmed = block.getTimestamp();
				status = CONFIRMED;
			}
		}

		this.receipt = receipt;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj instanceof Transaction) {
			UUID thatId = ((Transaction) obj).id;
			return id.equals(thatId);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

}
