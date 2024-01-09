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

package org.projectforge.framework.persistence.database;

import org.junit.jupiter.api.Test;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseServiceTestFork extends AbstractTestBase
{
  static final char[] DEFAULT_ADMIN_PASSWORD = "manage".toCharArray();

  @Autowired
  private DatabaseService databaseService;

  @Autowired
  private PfJpaXmlDumpService pfJpaXmlDumpService;

  @Autowired
  private UserGroupCache userGroupCache;

  @Override
  protected void initDb()
  {
    init(false);
  }

  @Test
  public void initializeEmptyDatabase()
  {
    userGroupCache.setExpired();
    assertFalse(databaseService.databaseTablesWithEntriesExist());
    final PFUserDO admin = new PFUserDO();
    admin.setUsername(DatabaseService.DEFAULT_ADMIN_USER);
    admin.setId(1);
    userService.encryptAndSavePassword(admin, DEFAULT_ADMIN_PASSWORD);
    ThreadLocalUserContext.setUser(admin);
    pfJpaXmlDumpService.createTestDatabase();
    databaseService.updateAdminUser(admin, null);
    databaseService.afterCreatedTestDb(true);
    final PFUserDO user = userService.authenticateUser(DatabaseService.DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASSWORD);
    assertNotNull(user);
    assertEquals(DatabaseService.DEFAULT_ADMIN_USER, user.getUsername());
    final Collection<Integer> col = userGroupCache.getUserGroups(user);
    assertEquals(6, col.size());
    assertTrue(userGroupCache.isUserMemberOfAdminGroup(user.getId()));
    assertTrue(userGroupCache.isUserMemberOfFinanceGroup(user.getId()));

    boolean exception = false;
    try {
      databaseService.initializeDefaultData(admin, null);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      exception = true;
      // Everything fine.
    }
    assertTrue(exception);
  }
}
