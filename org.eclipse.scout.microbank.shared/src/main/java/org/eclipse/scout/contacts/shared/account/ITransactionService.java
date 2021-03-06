package org.eclipse.scout.contacts.shared.account;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.scout.rt.platform.service.IService;
import org.eclipse.scout.rt.shared.TunnelToServer;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

@TunnelToServer
public interface ITransactionService extends IService {

  TransactionTablePageData getTransactionTableData(SearchFilter filter);

  void refresh(String transactionId);

  TransactionFormData prepareCreate(TransactionFormData formData);

  TransactionFormData create(TransactionFormData formData);

  TransactionFormData load(TransactionFormData formData);

  TransactionFormData store(TransactionFormData formData);

  BigDecimal convertToEther(BigInteger weiAmount);
  
  BigDecimal convertToEther(BigDecimal weiAmount);

  void send(String transactionId);
}
