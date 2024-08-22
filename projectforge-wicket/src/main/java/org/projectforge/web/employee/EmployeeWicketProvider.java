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

package org.projectforge.web.employee;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.NumberHelper;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class EmployeeWicketProvider extends ChoiceProvider<EmployeeDO>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeWicketProvider.class);

  private static final long serialVersionUID = 6228672123966093257L;

  private transient EmployeeDao employeeDao;

  private int pageSize = 20;

  private boolean withOwnUser = false;

  public EmployeeWicketProvider(EmployeeDao employeeDao)
  {
    this.employeeDao = employeeDao;
  }

  public EmployeeWicketProvider(EmployeeDao employeeDao, boolean withOwnUser)
  {
    this.employeeDao = employeeDao;
    this.withOwnUser = withOwnUser;
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public EmployeeWicketProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  @Override
  public String getDisplayValue(final EmployeeDO choice)
  {
    return choice.getUser().getFullname();
  }

  @Override
  public String getIdValue(final EmployeeDO choice)
  {
    return String.valueOf(choice.getId());
  }

  @Override
  public void query(String term, final int page, final Response<EmployeeDO> response)
  {
    boolean hasMore = false;
    Collection<EmployeeDO> result = new ArrayList<>();
    Collection<EmployeeDO> resultList = new ArrayList<>();
    if (withOwnUser) {
      resultList = employeeDao.findAllActive(false);
    } else {
      resultList = employeeDao.findAllActive(false).stream()
          .filter(emp -> emp.getUser().getId().equals(ThreadLocalUserContext.getUserId()) == false)
          .collect(Collectors.toList());
    }
    for (EmployeeDO emp : resultList) {
      if (StringUtils.isBlank(term) == false) {
        if (emp.getUser().getFullname().toLowerCase().contains(term.toLowerCase())) {
          result.add(emp);
        }
      } else {
        result.add(emp);
      }
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  @Override
  public Collection<EmployeeDO> toChoices(final Collection<String> ids)
  {
    final List<EmployeeDO> list = new ArrayList<>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer employeedId = NumberHelper.parseInteger(str);
      if (employeedId == null) {
        continue;
      }
      EmployeeDO employee = employeeDao.selectByPkDetached(employeedId);
      if (employee != null) {
        list.add(employee);
      }
    }
    return list;
  }

}
