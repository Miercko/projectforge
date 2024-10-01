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
import org.projectforge.business.fibu.*;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.jpa.PfPersistenceContext;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class RechnungDaoTest extends AbstractTestBase {
    @Autowired
    private RechnungDao rechnungDao;

    @Test
    public void getNextNumber() {
        persistenceService.runInTransaction(context ->
        {
            int dbNumber = rechnungDao.getNextNumber();
            logon(AbstractTestBase.TEST_FINANCE_USER);
            final RechnungDO rechnung1 = new RechnungDO();
            int number = rechnungDao.getNextNumber(rechnung1, context);
            rechnung1.setDatum(LocalDate.now());
            rechnung1.setFaelligkeit(LocalDate.now());
            rechnung1.setProjekt(initTestDB.addProjekt(null, 1, "foo", context));
            try {
                rechnungDao.save(rechnung1, context);
                fail("Exception with wrong number should be thrown (no number given).");
            } catch (UserException ex) {

            }
            rechnung1.setNummer(number);
            rechnung1.addPosition(createPosition(1, "50.00", "0", "test"));
            Serializable id = rechnungDao.save(rechnung1, context);
            final RechnungDO rechnung1FromDb = rechnungDao.getById(id, context);
            assertEquals(dbNumber++, rechnung1FromDb.getNummer().intValue());

            final RechnungDO rechnung2 = new RechnungDO();
            rechnung2.setDatum(LocalDate.now());
            rechnung2.setNummer(number);
            try {
                rechnungDao.save(rechnung2, context);
                fail("Exception with wrong number should be thrown (does already exists).");
            } catch (UserException ex) {

            }
            number = rechnungDao.getNextNumber(rechnung2, context);
            rechnung2.setNummer(number + 1);
            rechnung2.setFaelligkeit(LocalDate.now());
            rechnung2.setProjekt(initTestDB.addProjekt(null, 1, "foo", context));
            try {
                rechnungDao.save(rechnung2, context);
                fail("Exception with wrong number should be thrown (not continuously).");
            } catch (UserException ex) {
                // OK
            }
            rechnung2.setNummer(number);
            rechnung2.addPosition(createPosition(1, "50.00", "0", "test"));
            id = rechnungDao.save(rechnung2, context);
            final RechnungDO rechnung2FromDb = rechnungDao.getById(id, context);
            assertEquals(dbNumber++, rechnung2FromDb.getNummer().intValue());

            final RechnungDO rechnung3 = new RechnungDO();
            rechnung3.setDatum(LocalDate.now());
            rechnung3.setTyp(RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN);
            rechnung3.addPosition(createPosition(1, "50.00", "0", "test"));
            rechnung3.setFaelligkeit(LocalDate.now());
            rechnung3.setProjekt(initTestDB.addProjekt(null, 1, "foo", context));
            id = rechnungDao.save(rechnung3, context);
            final RechnungDO rechnung3FromDb = rechnungDao.getById(id, context);
            assertNull(rechnung3FromDb.getNummer());
            dbNumber++; // Needed for getNextNumber test;
            return null;
        });
    }

    @Test
    public void checkAccess() {
        persistenceService.runInTransaction(context ->
        {
            int dbNumber = rechnungDao.getNextNumber(null, context);
            logon(AbstractTestBase.TEST_FINANCE_USER);
            RechnungDO rechnung = new RechnungDO();
            int number = rechnungDao.getNextNumber(rechnung, context);
            rechnung.setDatum(LocalDate.now());
            rechnung.setFaelligkeit(LocalDate.now());
            rechnung.setProjekt(initTestDB.addProjekt(null, 1, "foo", context));
            rechnung.setNummer(number);

            rechnung.addPosition(createPosition(2, "100.50", "0.19", "test"));
            rechnung.addPosition(createPosition(1, "50.00", "0", "test"));
            assertEquals("289.19", String.valueOf(rechnung.getGrossSum().setScale(2)));
            Serializable id = rechnungDao.save(rechnung, context);
            dbNumber++;
            rechnung = rechnungDao.getById(id, context);

            logon(AbstractTestBase.TEST_CONTROLLING_USER);
            rechnungDao.getById(id, context);
            checkNoWriteAccess(id, rechnung, "Controlling", context);

            logon(AbstractTestBase.TEST_USER);
            checkNoAccess(id, rechnung, "Other", context);

            logon(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
            checkNoAccess(id, rechnung, "Project manager", context);

            logon(AbstractTestBase.TEST_ADMIN_USER);
            checkNoAccess(id, rechnung, "Admin ", context);
            return null;
        });
    }

    private void checkNoAccess(Serializable id, RechnungDO rechnung, String who, PfPersistenceContext context) {
        try {
            RechnungFilter filter = new RechnungFilter();
            rechnungDao.getList(filter, context);
            fail("AccessException expected: " + who + " users should not have select list access to invoices.");
        } catch (AccessException ex) {
            // OK
        }
        try {
            rechnungDao.getById(id, context);
            fail("AccessException expected: " + who + " users should not have select access to invoices.");
        } catch (AccessException ex) {
            // OK
        }
        checkNoHistoryAccess(id, rechnung, who);
        checkNoWriteAccess(id, rechnung, who, context);
    }

    private void checkNoHistoryAccess(Serializable id, RechnungDO rechnung, String who) {
        assertFalse(rechnungDao.hasLoggedInUserHistoryAccess(false), who + " users should not have select access to history of invoices.");
        try {
            rechnungDao.hasLoggedInUserHistoryAccess(true);
            fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
        } catch (AccessException ex) {
            // OK
        }
        assertFalse(rechnungDao.hasLoggedInUserHistoryAccess(rechnung, false), who + " users should not have select access to history of invoices.");
        try {
            rechnungDao.hasLoggedInUserHistoryAccess(rechnung, true);
            fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
        } catch (AccessException ex) {
            // OK
        }
    }

    private void checkNoWriteAccess(Serializable id, RechnungDO rechnung, String who, PfPersistenceContext context) {
        try {
            RechnungDO re = new RechnungDO();
            int number = rechnungDao.getNextNumber(re, context);
            re.setDatum(LocalDate.now());
            re.setNummer(number);
            rechnungDao.save(re, context);
            fail("AccessException expected: " + who + " users should not have save access to invoices.");
        } catch (AccessException ex) {
            // OK
        }
        try {
            rechnung.setBemerkung(who);
            rechnungDao.update(rechnung, context);
            fail("AccessException expected: " + who + " users should not have update access to invoices.");
        } catch (AccessException ex) {
            // OK
        }
    }

    private RechnungsPositionDO createPosition(final int menge, final String einzelNetto, final String vat,
                                               final String text) {
        final RechnungsPositionDO pos = new RechnungsPositionDO();
        pos.setMenge(new BigDecimal(menge));
        pos.setEinzelNetto(new BigDecimal(einzelNetto));
        pos.setVat(new BigDecimal(vat));
        pos.setText(text);
        return pos;
    }
}
