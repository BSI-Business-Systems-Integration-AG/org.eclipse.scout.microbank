package org.eclipse.scout.contacts.server.account;

import java.io.File;
import java.math.BigDecimal;

import org.eclipse.scout.contacts.server.account.model.Account;
import org.eclipse.scout.contacts.server.person.PersonService;
import org.eclipse.scout.contacts.shared.account.AccountFormData;
import org.eclipse.scout.contacts.shared.account.AccountTablePageData;
import org.eclipse.scout.contacts.shared.account.IAccountService;
import org.eclipse.scout.contacts.shared.account.AccountTablePageData.AccountTableRowData;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.util.IOUtility;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountService implements IAccountService {

	private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

	@Override
	public AccountTablePageData getAccountTableData(SearchFilter filter, String personId) {
		AccountTablePageData pageData = new AccountTablePageData();

		BEANS.get(EthereumService.class).getAccounts()
		.stream()
		.forEach(address -> {
			Account account = BEANS.get(EthereumService.class).getAccount(address);

			if (personId == null || personId.equals(account.getPersonId())) {
				addRow(account, personId, pageData);
			}
		});

		return pageData;
	}

	private void addRow(Account account, String personId, AccountTablePageData pageData) {
		AccountTableRowData rowData = pageData.addRow();

		// primay key
		String address = account.getAddress();
		rowData.setAddress(address);
		
		// summary column
		rowData.setSummary(calculateSummary(account, personId == null));
		
		// other values
		rowData.setPerson(account.getPersonId());
		rowData.setAccountName(account.getName());

		try {
			BigDecimal balance = BEANS.get(EthereumService.class).getBalance(address);
			rowData.setBalance(balance);
		}
		catch (Exception e) {
			LOG.error("failed to fetch balance for account " + address, e);
		}
	}

	@Override
	public AccountFormData prepareCreate(AccountFormData formData) {
		// TODO [mzi] add business logic here.
		return formData;
	}

	@Override
	public AccountFormData create(AccountFormData formData) {
		String personId = formData.getPerson().getValue();
		String name = formData.getName().getValue();
		String password = formData.getPassword().getValue();

		Account wallet = new Account(personId, name, password, createWalletPath());
		BEANS.get(EthereumService.class).save(wallet);

		return formData;
	}

	private String createWalletPath() {
		try {
			File tmpFile = File.createTempFile("tmp", ".txt");
			return tmpFile.getParent();
		}
		catch (Exception e) {
			LOG.error("Failed to create path to temp file", e);
		}

		return null;
	}

	@Override
	public AccountFormData load(AccountFormData formData) {
		String address = formData.getAddress().getValue();
		Account wallet = BEANS.get(EthereumService.class).getAccount(address);
		String fileName = wallet.getFile().getAbsolutePath();
		String fileContent = new String(IOUtility.getContent(fileName));

		formData.getPerson().setValue(wallet.getPersonId());
		formData.getName().setValue(wallet.getName());
		formData.getFilePath().setValue(fileName);
		formData.getFileContent().setValue(fileContent);

		return formData;
	}

	@Override
	public AccountFormData store(AccountFormData formData) {
		String address = formData.getAddress().getValue();
		Account wallet = BEANS.get(EthereumService.class).getAccount(address);

		wallet.setName(formData.getName().getValue());
		BEANS.get(EthereumService.class).save(wallet);

		return formData;
	}

	@Override
	public String getPerson(String address) {
		return BEANS.get(EthereumService.class).getAccount(address).getPersonId();
	}

	private String calculateSummary(Account account, boolean includeOwner) {
		if(includeOwner) {
			String person = BEANS.get(PersonService.class).getDisplayName(account.getPersonId());
			return String.format("%s (%s)", person, account.getName());
		}
		
		return account.getName();
	}
}
