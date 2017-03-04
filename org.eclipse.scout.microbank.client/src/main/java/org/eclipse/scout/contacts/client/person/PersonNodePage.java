package org.eclipse.scout.contacts.client.person;

import java.util.List;

import org.eclipse.scout.contacts.events.account.AccountTablePage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPageWithNodes;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;

public class PersonNodePage extends AbstractPageWithNodes {

  private String personId;

  public String getPersonId() {
    return personId;
  }

  public void setPersonId(String personId) {
    this.personId = personId;
  }

  @Override
  protected void execCreateChildPages(List<IPage<?>> pageList) {
    super.execCreateChildPages(pageList);
    AccountTablePage page = new AccountTablePage();
    page.setPersonId(getPersonId());
    pageList.add(page);
  }

}
