/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.contacts.client.contact;

import java.util.List;

import org.eclipse.scout.contacts.client.Icons;
import org.eclipse.scout.contacts.client.account.AccountTablePage;
import org.eclipse.scout.contacts.client.person.PersonTablePage;
import org.eclipse.scout.rt.client.ui.desktop.outline.AbstractOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.shared.TEXTS;

//tag::Outline[]
//tag::OutlineInit[]
public class ContactOutline extends AbstractOutline {

  //tag::execCreateChildPagesPerson[]
  @Override
  protected void execCreateChildPages(List<IPage<?>> pageList) {
    // pages to be shown in the navigation area of this outline
    pageList.add(new PersonTablePage()); // <1>
    // pageList.add(new OrganizationTablePage());
    pageList.add(new AccountTablePage());
  }
  //end::execCreateChildPagesPerson[]

  @Override
  protected String getConfiguredTitle() {
    return TEXTS.get("MicroBank");
  }

  @Override
  protected String getConfiguredIconId() {
    return Icons.Category;
  }
}
//end::OutlineInit[]
//end::Outline[]
