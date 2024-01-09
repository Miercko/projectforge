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

package org.projectforge.plugins.eed.service;

import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class EmployeeConfigurationServiceImpl extends CorePersistenceServiceImpl<Integer, EmployeeConfigurationDO>
    implements EmployeeConfigurationService
{
  @Autowired
  private EmployeeConfigurationDao employeeConfigurationDao;

  /**
   * There is just one row in the employee configuration table. If it does not exist, create it once.
   *
   * @return The EmployeeConfigurationDO.
   */
  @Override
  public Integer getSingleEmployeeConfigurationDOId()
  {
    return getSingleEmployeeConfigurationDO().getPk();
  }

  @Override
  public EmployeeConfigurationDO getSingleEmployeeConfigurationDO()
  {
    employeeConfigurationDao.checkLoggedInUserSelectAccess();
    final List<EmployeeConfigurationDO> employeeConfigurationDOs = employeeConfigurationDao.internalLoadAll();
    if (employeeConfigurationDOs.isEmpty()) {
      final EmployeeConfigurationDO employeeConfigurationDO = employeeConfigurationDao.newInstance();
      employeeConfigurationDao.save(employeeConfigurationDO);
      return employeeConfigurationDO;
    } else if (employeeConfigurationDOs.size() != 1) {
      throw new RuntimeException("Expected One employeeConfigurationDO, but found " + employeeConfigurationDOs.size());
    }

    return employeeConfigurationDOs.get(0);
  }

  public EmployeeConfigurationTimedDO addNewTimeAttributeRow(final EmployeeConfigurationDO employeeConfiguration,
      final String groupName)
  {
    final EmployeeConfigurationTimedDO row = new EmployeeConfigurationTimedDO();
    row.setEmployeeConfiguration(employeeConfiguration);
    row.setGroupName(groupName);
    employeeConfiguration.addTimeableAttribute(row);
    return row;
  }

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return employeeConfigurationDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(EmployeeConfigurationDO obj, boolean throwException)
  {
    return employeeConfigurationDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(EmployeeConfigurationDO obj, EmployeeConfigurationDO dbObj,
      boolean throwException)
  {
    return employeeConfigurationDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(EmployeeConfigurationDO obj, EmployeeConfigurationDO dbObj,
      boolean throwException)
  {
    return employeeConfigurationDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, EmployeeConfigurationDO obj, EmployeeConfigurationDO dbObj,
      boolean throwException)
  {
    return employeeConfigurationDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    return employeeConfigurationDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(EmployeeConfigurationDO obj)
  {
    return employeeConfigurationDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    employeeConfigurationDao.rebuildDatabaseIndex4NewestEntries();
  }

  @Override
  public void rebuildDatabaseIndex()
  {
    employeeConfigurationDao.rebuildDatabaseIndex();
  }

  @Override
  public EmployeeConfigurationDO getById(Serializable id) throws AccessException
  {
    // use the method from the DAO to have the access check
    return employeeConfigurationDao.getById(id);
  }
}
