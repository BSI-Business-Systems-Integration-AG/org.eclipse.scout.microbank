package org.eclipse.scout.contacts.server.account;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.scout.contacts.server.account.model.Account;
import org.eclipse.scout.contacts.server.account.model.Block;
import org.eclipse.scout.contacts.server.account.model.Transaction;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.exceptions.MessageDecodingException;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@ApplicationScoped
public class EthereumService {

	private static final Logger LOG = LoggerFactory.getLogger(EthereumService.class);

	// switch for testrpc/infura
	private static final boolean USE_LOCAL_CLIENT = true;

	// testrpc coordinates
	private static final String CLIENT_IP = "192.168.99.100";
	private static final String CLIENT_PORT = "8545";

	// infura coordinates
	private static final String TOKEN = "3UMFlH4jlpWx6IqttMeG";
	private static final String ETHEREUM_MAIN = "https://mainnet.infura.io/" + TOKEN;
	// private static final String ETHEREUM_TEST = "https://ropsten.infura.io/" + TOKEN;

	// copied from org.web3j.protocol.core.methods.response.Numeric
	private static final String HEX_PREFIX = "0x";

	// the connection to the ethereum net
	private Web3j web3j = null;

	// TODO replace these with some real persistence
	private static Map<String, Account> accounts = new HashMap<>();
	// transactions need to be persisted as well as ethereum currently does not offer an api to list all tx for an account
	// also see https://github.com/ethereum/go-ethereum/issues/1897
	private static Map<UUID, Transaction> transactions = new HashMap<>();

	@PostConstruct
	private void init() {
		LOG.info("Poulating dummy/temp accounts ...");

		// TODO verify/update to working accounts 
		populateAccount("prs01", "UTC--2017-03-06T16-57-12.927000000Z--097d9b716af54104fb1711fad5e09b357c626e94.json", "test");
		populateAccount("prs01a", "UTC--2017-03-06T16-56-52.160000000Z--8d3f0415b7cca124a16690b67ac2a8b7525d87f7.json", "test");
		LOG.info("local wallets successfully loaded ...");

		// move some initial funds to alice's account (only when working with testrpc)
		if (USE_LOCAL_CLIENT) {
			try {
				EthCoinbase coinbase = getWeb3j().ethCoinbase().sendAsync().get();
				String from = coinbase.getAddress();
				String to = "0x8d2ec831056c620fea2fabad8bf6548fc5810cc3";
				BigInteger amount = Convert.toWei("10", Unit.ETHER).toBigInteger();

				BigInteger nonce = getNonce(from);

				org.web3j.protocol.core.methods.request.Transaction transaction =
						new org.web3j.protocol.core.methods.request.Transaction(from, nonce, Transaction.GAS_PRICE_DEFAULT, Transaction.GAS_LIMIT_DEFAULT, to, amount, null);

				EthSendTransaction txRequest = getWeb3j().ethSendTransaction(transaction).sendAsync().get();
				LOG.info(String.format("added %d weis to account of prs01. tx hash: %s", amount, txRequest.getTransactionHash()));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void populateAccount(String personId, String fileName, String password) {
		String walletName = "Primary Account";
		String walletPath = "C:\\Users\\mzi\\AppData\\Local\\Temp";
		Account wallet = Account.load(walletName, password, walletPath, fileName);
		wallet.setPersonId(personId);

		save(wallet);
	}

	public String createTransaction(String from, String to, BigInteger amountWei, BigInteger nonce, String data, BigInteger gasPrice, BigInteger gasLimit) {

		if (from == null || to == null || amountWei == null) {
			return null;
		}

		Account wallet = getAccount(from);
		if (wallet == null) {
			return null;
		}
		
		if (nonce == null) {
			nonce = getNonce(from);
		}

		if (gasPrice == null) {
			gasPrice = Transaction.GAS_PRICE_DEFAULT;
		}

		if (gasLimit == null) {
			gasLimit = Transaction.GAS_LIMIT_DEFAULT;
		}

		Transaction tx = wallet.createSignedTransaction(to, amountWei, nonce, gasPrice, gasLimit);
		save(tx);

		return tx.getId().toString();
	}

	public Set<String> getAccounts() {
		return accounts.keySet();
	}

	public Set<String> getAccounts(String personId) {
		if (personId == null) {
			return getAccounts();
		}

		return accounts.values()
				.stream()
				.filter(wallet -> personId.equals(wallet.getPersonId()))
				.map(wallet -> wallet.getAddress())
				.collect(Collectors.toSet());
	}

	public Account getAccount(String address) {
		return accounts.get(address);
	}

	public void save(Account wallet) {
		save(wallet, 0.0);
	}

	public void save(Account wallet, double addFunds) {
		LOG.info("caching wallet '" + wallet.getFileName() + "' with address '" + wallet.getAddress() + "'");

		accounts.put(wallet.getAddress(), wallet);

		if(addFunds > 0) {
			addSomeFunds(addFunds, wallet.getAddress());
		}
	}

	private String addSomeFunds(double valueInEther, String address) {
		LOG.info("adding funds: " + valueInEther + " ethers");

		try {
			EthCoinbase coinbase = web3j.ethCoinbase().sendAsync().get();
			EthGetTransactionCount transactionCount = web3j
					.ethGetTransactionCount(coinbase.getAddress(), DefaultBlockParameterName.LATEST)
					.sendAsync()
					.get();

			BigInteger value = BEANS.get(TransactionService.class).convertToWei(BigDecimal.valueOf(valueInEther));
			BigInteger nonce = transactionCount.getTransactionCount();
			BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
			BigInteger gasLimit = BigInteger.valueOf(4_300_000L);

			EthSendTransaction response = web3j
					.ethSendTransaction(
							org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
									coinbase.getAddress(), nonce, gasPrice, gasLimit, address, value))
					.sendAsync()
					.get();

			return response.getTransactionHash();
		}
		catch (Exception e) {
			LOG.error("failed to transfer " + valueInEther + " [ethers] from coinbase to address " + address, e);
			return null;
		}
	}

	public Set<String> getTransactions() {
		return transactions.keySet()
				.stream()
				.map(id -> id.toString())
				.collect(Collectors.toSet());
	}

	public Transaction getTransaction(String id) {
		return transactions.get(UUID.fromString(id));
	}

	public void save(Transaction transaction) {
		LOG.info("Caching tx from: " + transaction.getFromAddress() + " to: " + transaction.getToAddress() + " with amount " + transaction.getValue() + " and hash: " + transaction.getHash());

		transactions.put(transaction.getId(), transaction);
	}

	public Block getBlock(String blockHash) {
		try {
			EthBlock block = getWeb3j().ethGetBlockByHash(blockHash, false).sendAsync().get();
			String hash = block.getBlock().getHash();
			BigInteger number = block.getBlock().getNumber();
			BigInteger time = block.getBlock().getTimestamp();
			// block time stamp is in seconcs
			// http://ethereum.stackexchange.com/questions/7853/is-the-block-timestamp-value-in-solidity-seconds-or-milliseconds
			Date timestamp = new Date(time.longValue() * 1000);

			return new Block(hash, number, timestamp);
		} 
		catch (Exception e) {
			LOG.error("failed to fetch block for hash " + blockHash, e);
			return null;
		}
	}

	private Web3j getWeb3j() {
		if (web3j == null) {
			LOG.info("Trying to connect to Ethereum net ...");

			if (USE_LOCAL_CLIENT) {
				String clientUrl = String.format("http://%s:%s", CLIENT_IP, CLIENT_PORT);
				web3j = Web3j.build(new HttpService(clientUrl));
			}
			else {
				web3j = Web3j.build(new InfuraHttpService(ETHEREUM_MAIN));
			}

			try {
				Web3ClientVersion client = web3j.web3ClientVersion().sendAsync().get();
				LOG.info("Successfully connected to " + client.getWeb3ClientVersion());
			}
			catch (Exception e) {
				LOG.warn("Successfully connected to Ethereum client but failed to get client version", e);
			}
		}

		return web3j;
	}

	public BigDecimal getBalance(String address) {
		return getBalance(address, Unit.ETHER);
	}

	public BigDecimal getBalance(String address, Unit unit) {
		if (address == null || unit == null) {
			return null;
		}

		BigInteger balance = getBalanceWei(address);

		if (balance == null) {
			return null;
		}

		return Convert.fromWei(new BigDecimal(balance), unit);
	}

	public BigInteger getBalanceWei(String address) {
		try {
			EthGetBalance balanceResponse = getWeb3j().ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
			BigInteger balance = getBalanceFix(balanceResponse);
			return balance;

			// return balanceResponse.getBalance();
		}
		catch (Exception e) {
			throw new ProcessingException("Failed to get balance for address '" + address + "'", e);
		}
	}

	public Transaction send(Transaction tx) {
		LOG.info("Sending TX ...");
		EthSendTransaction response = null;
		
		try {
			response = getWeb3j().ethSendRawTransaction(tx.getSignedContent()).sendAsync().get();

			tx.setSent(new Date());
			tx.setHash(response.getTransactionHash());
			tx.setStatus(Transaction.PENDING);
			LOG.info("TX successfully sent. Hash: " + tx.getHash() + " Result: " + response.getResult());

			save(tx);
		}
		catch (Exception e) {
			tx.setError(response.getError());
			throw new ProcessingException("Failed to send TX " + tx.getSignedContent() + " " + response.getError().toString(), e);
		}

		return tx;
	}

	public Transaction refreshStatus(String transactionId) {

		Transaction tx = getTransaction(transactionId);
		
		if(tx.getTransactionReceipt() != null) {
			return tx;
		}
		
		LOG.info("Polling TX status...");
		EthGetTransactionReceipt txReceipt = null;

		try {
			txReceipt = getWeb3j().ethGetTransactionReceipt(tx.getHash()).sendAsync().get();
			
			tx.setTransactionReceipt(txReceipt.getResult());
			tx.setError(txReceipt.getError());
			
			if(tx.getError() == null) {
				LOG.info("Successfully polled status. Status: " + tx.getStatus() + " " + Transaction.STATUS.get(tx.getStatus()));
			}
			else {
				LOG.info("Successfully polled status. Status: " + tx.getStatus() + " " + Transaction.STATUS.get(tx.getStatus()) + " Error: " + tx.getError().getMessage() + " Data: " + tx.getError().getData());
			}
		}
		catch (Exception e) {
			throw new ProcessingException("failed to poll status for transaction " + tx.getSignedContent(), e);
		}


		save(tx);

		return tx;
	}

	private BigInteger getNonce(String address) {
		try {
			EthGetTransactionCount txCount = getWeb3j().ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();
			BigInteger nonce = txCount.getTransactionCount();

			LOG.info("Successfully got nonce: " + nonce + " for address " + address);
			return nonce;
		}
		catch (Exception e) {
			LOG.error("Failed to get nonce for address " + address);
			return null;
		}
	}

	// copied from org.web3j.protocol.core.methods.response.EthGetBalance
	private BigInteger getBalanceFix(EthGetBalance balanceResponse) {
		String balance = balanceResponse.getResult();
		return decodeQuantity(balance);
	}

	// copied from org.web3j.protocol.core.methods.response.Numeric
	private static BigInteger decodeQuantity(String value) {
		if (!isValidHexQuantity(value)) {
			throw new MessageDecodingException("Value must be in format 0x[1-9]+[0-9]* or 0x0");
		}
		try {
			return new BigInteger(value.substring(2), 16);
		}
		catch (NumberFormatException e) {
			throw new MessageDecodingException("Negative ", e);
		}
	}

	// copied from org.web3j.protocol.core.methods.response.Numeric
	private static boolean isValidHexQuantity(String value) {
		if (value == null) {
			return false;
		}

		if (value.length() < 3) {
			return false;
		}

		if (!value.startsWith(HEX_PREFIX)) {
			return false;
		}

		// If TestRpc resolves the following issue, we can reinstate this code
		// https://github.com/ethereumjs/testrpc/issues/220
		//        if (value.length() > 3 && value.charAt(2) == '0') {
		//            return false;
		//        }

		return true;
	}

}
