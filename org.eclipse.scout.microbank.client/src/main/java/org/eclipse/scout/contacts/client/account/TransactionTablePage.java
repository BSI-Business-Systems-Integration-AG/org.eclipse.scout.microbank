package org.eclipse.scout.contacts.client.account;

import java.util.Set;

import org.eclipse.scout.contacts.client.account.TransactionTablePage.Table;
import org.eclipse.scout.contacts.shared.account.ITransactionService;
import org.eclipse.scout.contacts.shared.account.TransactionStatusLookupCall;
import org.eclipse.scout.contacts.shared.account.TransactionTablePageData;
import org.eclipse.scout.rt.client.dto.Data;
import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenuType;
import org.eclipse.scout.rt.client.ui.action.menu.TableMenuType;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractBigDecimalColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractLongColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractSmartColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPageWithTable;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Order;
import org.eclipse.scout.rt.platform.util.CollectionUtility;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;

@Data(TransactionTablePageData.class)
public class TransactionTablePage extends AbstractPageWithTable<Table> {

	private String address;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	protected String getConfiguredTitle() {
		return TEXTS.get("TransactionTablePage");
	}

	@Override
	protected void execLoadData(SearchFilter filter) {
		importPageData(BEANS.get(ITransactionService.class).getTransactionTableData(filter));
	}

	/**
	 * see link below for tx attributes
	 * https://etherscan.io/tx/0xf5b0c97b91d197e06d62831cee8930603a127dec47e721bd69cf4e8010b06864
	 */
	public class Table extends AbstractTable {

		public BlockColumn getBlockColumn() {
			return getColumnSet().getColumnByClass(BlockColumn.class);
		}

		public FromColumn getFromColumn() {
			return getColumnSet().getColumnByClass(FromColumn.class);
		}

		public ValueColumn getValueColumn() {
			return getColumnSet().getColumnByClass(ValueColumn.class);
		}

		public IdColumn getIdColumn() {
			return getColumnSet().getColumnByClass(IdColumn.class);
		}

		public SummaryColumn getNodeTitleColumn() {
			return getColumnSet().getColumnByClass(SummaryColumn.class);
		}

		public StatusColumn getStatusColumn() {
			return getColumnSet().getColumnByClass(StatusColumn.class);
		}

		public ToColumn getToColumn() {
			return getColumnSet().getColumnByClass(ToColumn.class);
		}

		public HashColumn getHashColumn() {
			return getColumnSet().getColumnByClass(HashColumn.class);
		}

		@Order(1000)
		public class ViewMenu extends AbstractMenu {
			@Override
			protected String getConfiguredText() {
				return TEXTS.get("View");
			}

			@Override
			protected String getConfiguredIconId() {
				// get unicode from http://fontawesome.io/icon/binoculars/
				return "font:awesomeIcons \uf1e5";
			}

			@Override
			protected Set<? extends IMenuType> getConfiguredMenuTypes() {
				return CollectionUtility.hashSet(TableMenuType.SingleSelection);
			}

			@Override
			protected void execAction() {
				TransactionForm form = new TransactionForm();
				String txId = getTable().getIdColumn().getSelectedValue();
				form.setId(txId);
				form.startView();
			}
		}

		@Order(2000)
		public class SendMenu extends AbstractMenu {
			@Override
			protected String getConfiguredText() {
				return TEXTS.get("Send");
			}

			@Override
			protected String getConfiguredIconId() {
				// get unicode from http://fontawesome.io/icon/paper-plane/
				return "font:awesomeIcons \uf1d8";
			}

			@Override
			protected Set<? extends IMenuType> getConfiguredMenuTypes() {
				return CollectionUtility.hashSet(TableMenuType.SingleSelection);
			}

			@Override
			protected void execOwnerValueChanged(Object newOwnerValue) {
				int status = getTable().getStatusColumn().getSelectedValue();
				setEnabled(status == TransactionStatusLookupCall.OFFLINE);
			}

			@Override
			protected void execAction() {
				String txId = getTable().getIdColumn().getSelectedValue();
				BEANS.get(ITransactionService.class).send(txId);
				reloadPage();
			}
		}

		@Order(3000)
		public class DeleteMenu extends AbstractMenu {
			@Override
			protected String getConfiguredText() {
				return TEXTS.get("DeleteMenu");
			}

			@Override
			protected String getConfiguredIconId() {
				// get unicode from http://fontawesome.io/icon/trash/
				return "font:awesomeIcons \uf1f8";
			}

			@Override
			protected Set<? extends IMenuType> getConfiguredMenuTypes() {
				return CollectionUtility.hashSet(TableMenuType.SingleSelection);
			}

			@Override
			protected void execOwnerValueChanged(Object newOwnerValue) {
				int status = getTable().getStatusColumn().getSelectedValue();
				setEnabled(status == TransactionStatusLookupCall.OFFLINE);
			}

			@Override
			protected void execAction() {
				TransactionForm form = new TransactionForm();
				String txId = getTable().getIdColumn().getSelectedValue();
				form.setId(txId);
				form.startModify();

				form.waitFor();
				if (form.isFormStored()) {
					reloadPage();
				}
			}
		}

		@Order(4000)
		public class RefreshMenu extends AbstractMenu {
			@Override
			protected String getConfiguredText() {
				return TEXTS.get("Refresh");
			}

			@Override
			protected String getConfiguredIconId() {
				// get unicode from http://fontawesome.io/icon/refresh/
				return "font:awesomeIcons \uf021";
			}

			@Override
			protected Set<? extends IMenuType> getConfiguredMenuTypes() {
				return CollectionUtility.hashSet(TableMenuType.SingleSelection, TableMenuType.MultiSelection);
			}

			@Override
			protected void execAction() {
				ITransactionService service = BEANS.get(ITransactionService.class);
				getSelectedRows()
				.stream()
				.forEach(row -> {
					String txId = (String) row.getKeyValues().get(0);
					service.refresh(txId);
				});

				reloadPage();
			}
		}

		@Order(0)
		public class IdColumn extends AbstractStringColumn {
			@Override
			protected boolean getConfiguredPrimaryKey() {
				return true;
			}

			@Override
			protected boolean getConfiguredDisplayable() {
				return false;
			}
		}

		@Order(0)
		public class SummaryColumn extends AbstractStringColumn {
			@Override
			protected boolean getConfiguredSummary() {
				return true;
			}

			@Override
			protected boolean getConfiguredDisplayable() {
				return false;
			}
		}

		@Order(1000)
		public class BlockColumn extends AbstractLongColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("BlockHeight");
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(2000)
		public class HashColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("TXHash");
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(5000)
		public class FromColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("From");
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(6000)
		public class ToColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("To");
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(7000)
		public class ValueColumn extends AbstractBigDecimalColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("Value");
			}

			@Override
			protected int getConfiguredMaxFractionDigits() {
				return 8;
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(8000)
		public class StatusColumn extends AbstractSmartColumn<Integer> {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("Status");
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}

			@Override
			protected Class<? extends ILookupCall<Integer>> getConfiguredLookupCall() {
				return TransactionStatusLookupCall.class;
			}
		}
	}
}
