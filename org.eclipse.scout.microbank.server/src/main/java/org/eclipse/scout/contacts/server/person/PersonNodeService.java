package org.eclipse.scout.contacts.server.person;

import org.eclipse.scout.contacts.shared.person.IPersonNodeService;
import org.eclipse.scout.contacts.shared.person.PersonNodeTablePageData;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

public class PersonNodeService implements IPersonNodeService {

  @Override
  public PersonNodeTablePageData getPersonNodeTableData(SearchFilter filter) {
    PersonNodeTablePageData pageData = new PersonNodeTablePageData();
    // TODO [mzi] fill pageData.
    return pageData;
  }
}
