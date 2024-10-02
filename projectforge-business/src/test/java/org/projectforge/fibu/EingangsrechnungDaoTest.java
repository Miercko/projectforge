/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.fibu;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EingangsrechnungDao;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.RechnungFilter;
import org.projectforge.framework.access.AccessException;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EingangsrechnungDaoTest extends AbstractTestBase {
  @Autowired
  private EingangsrechnungDao eingangsrechnungDao;

  @Test
  public void checkAccess() {
    logon(AbstractTestBase.TEST_FINANCE_USER);
    EingangsrechnungDO eingangsrechnung = new EingangsrechnungDO();
    eingangsrechnung.setDatum(LocalDate.now());
    eingangsrechnung.addPosition(new EingangsrechnungsPositionDO());
    eingangsrechnung.setFaelligkeit(LocalDate.now());
    Serializable id = eingangsrechnungDao.saveInTrans(eingangsrechnung);
    eingangsrechnung = eingangsrechnungDao.getById(id);

    logon(AbstractTestBase.TEST_CONTROLLING_USER);
    eingangsrechnungDao.getById(id);
    checkNoWriteAccess(id, eingangsrechnung, "Controlling");

    logon(AbstractTestBase.TEST_USER);
    checkNoAccess(id, eingangsrechnung, "Other");

    logon(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
    checkNoAccess(id, eingangsrechnung, "Project manager");

    logon(AbstractTestBase.TEST_ADMIN_USER);
    checkNoAccess(id, eingangsrechnung, "Admin ");
  }

  private void checkNoAccess(Serializable id, EingangsrechnungDO eingangsrechnung, String who) {
    try {
      RechnungFilter filter = new RechnungFilter();
      eingangsrechnungDao.getList(filter);
      fail("AccessException expected: " + who + " users should not have select list access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
    try {
      eingangsrechnungDao.getById(id);
      fail("AccessException expected: " + who + " users should not have select access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
    checkNoHistoryAccess(id, eingangsrechnung, who);
    checkNoWriteAccess(id, eingangsrechnung, who);
  }

  private void checkNoHistoryAccess(Serializable id, EingangsrechnungDO eingangsrechnung, String who) {
    assertEquals(eingangsrechnungDao.hasLoggedInUserHistoryAccess(false), false,
            who + " users should not have select access to history of invoices.");
    try {
      eingangsrechnungDao.hasLoggedInUserHistoryAccess(true);
      fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
    } catch (AccessException ex) {
      // OK
    }
    assertEquals(eingangsrechnungDao.hasLoggedInUserHistoryAccess(eingangsrechnung, false), false,
            who + " users should not have select access to history of invoices.");
    try {
      eingangsrechnungDao.hasLoggedInUserHistoryAccess(eingangsrechnung, true);
      fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
    } catch (AccessException ex) {
      // OK
    }
  }

  private void checkNoWriteAccess(Serializable id, EingangsrechnungDO eingangsrechnung, String who) {
    try {
      EingangsrechnungDO re = new EingangsrechnungDO();
      re.setDatum(LocalDate.now());
      eingangsrechnungDao.saveInTrans(re);
      fail("AccessException expected: " + who + " users should not have save access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
    try {
      eingangsrechnung.setBemerkung(who);
      eingangsrechnungDao.updateInTrans(eingangsrechnung);
      fail("AccessException expected: " + who + " users should not have update access to invoices.");
    } catch (AccessException ex) {
      // OK
    }
  }

  public void setEingangsrechnungDao(EingangsrechnungDao eingangsrechnungDao) {
    this.eingangsrechnungDao = eingangsrechnungDao;
  }
}
