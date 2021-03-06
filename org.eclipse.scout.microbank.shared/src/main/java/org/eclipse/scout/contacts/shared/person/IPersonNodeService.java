package org.eclipse.scout.contacts.shared.person;

import org.eclipse.scout.rt.platform.service.IService;
import org.eclipse.scout.rt.shared.TunnelToServer;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

/**
 * <h3>{@link IPersonNodeService}</h3>
 *
 * @author mzi
 */
@TunnelToServer
public interface IPersonNodeService extends IService {

  /**
   * @param filter
   * @return
   */
  PersonNodeTablePageData getPersonNodeTableData(SearchFilter filter);
}
