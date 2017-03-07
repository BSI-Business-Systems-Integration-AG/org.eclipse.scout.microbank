package org.eclipse.scout.contacts.server.account;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.scout.contacts.server.account.model.Block;
import org.eclipse.scout.contacts.server.account.model.Transaction;
import org.eclipse.scout.contacts.shared.account.ITransactionService;
import org.eclipse.scout.contacts.shared.account.TransactionFormData;
import org.eclipse.scout.contacts.shared.account.TransactionTablePageData;
import org.eclipse.scout.contacts.shared.account.TransactionTablePageData.TransactionTableRowData;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.methods.request.RawTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class TransactionService implements ITransactionService {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionService.class);
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy (HH:mm)");

	@Override
	public TransactionTablePageData getTransactionTableData(SearchFilter filter) {
		TransactionTablePageData pageData = new TransactionTablePageData();

		BEANS.get(EthereumService.class).getTransactions()
		.stream()
		.forEach(txId -> {
			Transaction tx = BEANS.get(EthereumService.class).getTransaction(txId);
			addRow(txId, tx, pageData);
		});

		return pageData;
	}

	private void addRow(String txId, Transaction tx, TransactionTablePageData pageData) {
		TransactionTableRowData rowData = pageData.addRow();
		
		// primary key
		rowData.setId(txId);
		
		// node title
		rowData.setSummary(calculateSummary(tx));
		
		// other attributes
		rowData.setFrom(tx.getFromAddress());
		rowData.setTo(tx.getToAddress());
		BigDecimal valueEther = Convert.fromWei(new BigDecimal(tx.getValue()), Unit.ETHER);
		rowData.setValue(valueEther);
		rowData.setStatus(tx.getStatus());
		rowData.setHash(tx.getHash());
		TransactionReceipt receipt = tx.getTransactionReceipt();
		if (receipt != null) {
			try {
				rowData.setBlock(receipt.getBlockNumber().longValue());
			}
			catch (Exception e) {
				LOG.info("failed to fetch tx block number", e);
			}
		}
	}

	@Override
	public void refresh(String transactionId) {
		BEANS.get(EthereumService.class).refreshStatus(transactionId);
	}

	@Override
	public TransactionFormData prepareCreate(TransactionFormData formData) {
		formData.getStatus().setValue(Transaction.OFFLINE);
		formData.getGasPrice().setValue(Transaction.GAS_PRICE_DEFAULT);
		formData.getGasLimit().setValue(Transaction.GAS_LIMIT_DEFAULT);
		formData.getTxFee().setValue(convertToEther(new BigDecimal(Transaction.TX_FEE_DEFAULT)));

		return formData;
	}

	@Override
	public TransactionFormData create(TransactionFormData formData) {
		return createNew(formData);
	}

	@Override
	public TransactionFormData load(TransactionFormData formData) {
		String txId = formData.getId();
		Transaction tx = BEANS.get(EthereumService.class).getTransaction(txId);

		if (tx != null) {
			String from = tx.getFromAddress();
			String to = tx.getToAddress();

			formData.getFrom().setValue(from);
			formData.getTo().setValue(to);
			formData.getAmount().setValue(convertToEther(new BigDecimal(tx.getValue())));
			formData.getStatus().setValue(tx.getStatus());
			formData.getCreated().setValue(tx.getCreated());
			formData.getSent().setValue(tx.getSent());

			BigInteger gasPrice = tx.getRawTransaction().getGasPrice();

			RawTransaction txRaw = tx.getRawTransaction();
			if (txRaw != null) {
				BigInteger gasLimit = tx.getRawTransaction().getGasLimit();
				BigInteger nonce = tx.getRawTransaction().getNonce();
				formData.getNonce().setValue(nonce);
				formData.getGasPrice().setValue(gasPrice);
				formData.getGasLimit().setValue(gasLimit);
				formData.getTxFee().setValue(convertToEther(gasPrice.multiply(gasLimit)));
			}

			TransactionReceipt txReceipt = tx.getTransactionReceipt();
			if (txReceipt != null) {
				BigInteger gasUsed = txReceipt.getGasUsed();
				formData.getTxHash().setValue(txReceipt.getTransactionHash());
				formData.getGasUsed().setValue(gasUsed);
				formData.getTxFee().setValue(convertToEther(gasPrice.multiply(gasUsed)));

				Block block = BEANS.get(EthereumService.class).getBlock(txReceipt.getBlockHash());
				formData.getBlockHash().setValue(block.getHash());
				formData.getBlock().setValue(block.getNumber().toString());
				formData.getBlockTimestamp().setValue(block.getTimestamp());
			}
		}

		return formData;
	}

	@Override
	public TransactionFormData store(TransactionFormData formData) {

		// only update offline transactions
		int status = formData.getStatus().getValue();
		if (status != Transaction.OFFLINE) {
			LOG.warn("can only replace tx with offline state. tx.state=" + status + " tx.id=" + formData.getId());
			return formData;
		}

		formData = createNew(formData);

		Transaction tx = BEANS.get(EthereumService.class).getTransaction(formData.getId());
		tx.setStatus(Transaction.REMOVED);
		BEANS.get(EthereumService.class).save(tx);

		return formData;
	}

	@Override
	public void send(String transactionId) {
		EthereumService service = BEANS.get(EthereumService.class);
		Transaction tx = service.getTransaction(transactionId);
		service.send(tx);
	}

	private TransactionFormData createNew(TransactionFormData formData) {
		String from = formData.getFrom().getValue();
		String to = formData.getTo().getValue();
		BigInteger amountWei = convertToWei(formData.getAmount().getValue());
		BigInteger nonce = formData.getNonce().getValue();
		String data = null;
		BigInteger gasPrice = formData.getGasPrice().getValue();
		BigInteger gasLimit = formData.getGasLimit().getValue();

		BEANS.get(EthereumService.class).createTransaction(from, to, amountWei, nonce, data , gasPrice, gasLimit);

		return formData;
	}

	@Override
	public BigDecimal convertToEther(BigInteger weiAmount) {
		return convertToEther(new BigDecimal(weiAmount));
	}

	@Override
	public BigDecimal convertToEther(BigDecimal weiAmount) {
		return Convert.fromWei(weiAmount, Unit.ETHER);
	}

	public BigInteger convertToWei(BigDecimal etherAmount) {
		return Convert.toWei(etherAmount, Unit.ETHER).toBigInteger();
	}

	private String calculateSummary(Transaction tx) {
		String date = DATE_FORMAT.format(tx.getCreated());
		
		if(tx.getBlock() != null) {
			date = DATE_FORMAT.format(tx.getBlock().getTimestamp());
		}
		else if(tx.getSent() != null) {
			date = DATE_FORMAT.format(tx.getSent());
		}
		
		return String.format("%s ETH %s %s", date, convertToEther(tx.getValue()), Transaction.STATUS.get(tx.getStatus()));
	}
}
