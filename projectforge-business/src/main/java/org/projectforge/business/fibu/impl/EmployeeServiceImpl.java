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

package org.projectforge.business.fibu.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.EntityCopyStatus;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Standard implementation of the Employee service interface.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KundeDao.class);

  private static final BigDecimal FULL_TIME_WEEKLY_WORKING_HOURS = new BigDecimal(40);

  private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal(12);

  @Autowired
  private UserDao userDao;

  @Autowired
  private Kost1Dao kost1Dao;

  @Autowired
  private EmployeeDao employeeDao;

  @Autowired
  private VacationService vacationService;

  @Autowired
  private TimesheetDao timesheetDao;

  public List<EmployeeDO> getList(BaseSearchFilter filter) {
    return employeeDao.getList(filter);
  }

  @Override
  public void setPfUser(EmployeeDO employee, Integer userId) {
    final PFUserDO user = userDao.getOrLoad(userId);
    employee.setUser(user);
  }

  @Override
  public EmployeeDO getEmployeeByUserId(Integer userId) {
    return employeeDao.findByUserId(userId);
  }

  @Override
  public void setKost1(EmployeeDO employee, final Integer kost1Id) {
    final Kost1DO kost1 = kost1Dao.getOrLoad(kost1Id);
    employee.setKost1(kost1);
  }

  public boolean hasLoggedInUserInsertAccess() {
    return employeeDao.hasLoggedInUserInsertAccess();
  }

  public boolean hasLoggedInUserInsertAccess(EmployeeDO obj, boolean throwException) {
    return employeeDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  public boolean hasLoggedInUserUpdateAccess(EmployeeDO obj, EmployeeDO dbObj, boolean throwException) {
    return employeeDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  public boolean hasLoggedInUserDeleteAccess(EmployeeDO obj, EmployeeDO dbObj, boolean throwException) {
    return employeeDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  public boolean hasDeleteAccess(PFUserDO user, EmployeeDO obj, EmployeeDO dbObj, boolean throwException) {
    return employeeDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  public EmployeeDO getById(Serializable id) throws AccessException {
    return employeeDao.getById(id);
  }

  public List<String> getAutocompletion(String property, String searchString) {
    return employeeDao.getAutocompletion(property, searchString);
  }

  public List<DisplayHistoryEntry> getDisplayHistoryEntries(EmployeeDO obj) {
    return employeeDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public boolean isEmployeeActive(final EmployeeDO employee) {
    if (employee.getAustrittsDatum() == null) {
      return true;
    }
    final PFDateTime now = PFDateTime.now();
    final PFDateTime austrittsdatum = PFDateTime.from(employee.getAustrittsDatum()); // not null
    return now.isBefore(austrittsdatum);
  }

  @Override
  public BigDecimal getMonthlySalary(EmployeeDO employee, PFDateTime selectedDate) {
    log.error("****** Not yet migrated.");
/*    final EmployeeTimedDO attribute = timeableService.getAttrRowValidAtDate(employee, "annuity", selectedDate.getUtilDate());
    final BigDecimal annualSalary = attribute != null ? attribute.getAttribute("annuity", BigDecimal.class) : null;
    final BigDecimal weeklyWorkingHours = employee.getWeeklyWorkingHours();

    if (annualSalary != null && weeklyWorkingHours != null && BigDecimal.ZERO.compareTo(weeklyWorkingHours) < 0) {
      // do the multiplication before the division to minimize rounding problems
      // we need a rounding mode to avoid ArithmeticExceptions when the exact result cannot be represented in the result
      return annualSalary
              .multiply(weeklyWorkingHours)
              .divide(MONTHS_PER_YEAR, BigDecimal.ROUND_HALF_UP)
              .divide(FULL_TIME_WEEKLY_WORKING_HOURS, BigDecimal.ROUND_HALF_UP);
    }
*/
    return null;
  }

  @Override
  public List<EmployeeDO> findAllActive(final boolean checkAccess) {
    final Collection<EmployeeDO> employeeList;
    if (checkAccess) {
      employeeList = employeeDao.getList(new EmployeeFilter());
    } else {
      employeeList = employeeDao.internalLoadAll();
    }
    return employeeList.stream()
            .filter(this::isEmployeeActive)
            .collect(Collectors.toList());
  }

  @Override
  public EmployeeDO getEmployeeByStaffnumber(String staffnumber) {
    return employeeDao.getEmployeeByStaffnumber(staffnumber);
  }

  @Override
  public List<EmployeeDO> getAll(boolean checkAccess) {
    return checkAccess ? employeeDao.getList(new EmployeeFilter()) : employeeDao.internalLoadAll();
  }

  @Override
  public EmployeeStatus getEmployeeStatus(final EmployeeDO employee) {
    log.error("****** Not yet migrated.");
/*
    final EmployeeTimedDO attrRow = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, new Date());
    if (attrRow != null && !StringUtils.isEmpty(attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME))) {
      return EmployeeStatus.findByi18nKey(attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME));
    }*/
    return null;
  }

  @Override
  public BigDecimal getAnnualLeaveDays(EmployeeDO employee) {
    return getAnnualLeaveDays(employee, LocalDate.now());
  }

  @Override
  public BigDecimal getAnnualLeaveDays(EmployeeDO employee, LocalDate validAtDate) {
    if (employee == null || validAtDate == null) { // Should only occur in CallAllPagesTest (Wicket).
      return null;
    }
    log.error("****** Not yet migrated.");
    return BigDecimal.ZERO;
/*
    Date date = PFDateTime.from(validAtDate).getUtilDate(); // not null
    final EmployeeTimedDO attrRow = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_GROUP_NAME, date);
    if (attrRow != null) {
      final String str = attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_PROP_NAME);
      if (NumberUtils.isCreatable(str)) {
        return NumberUtils.createBigDecimal(str);
      }
    }
    return BigDecimal.ZERO;*/
  }

  /**
   * @param employee
   * @param validfrom       The day of year is ignored. The year is important and used.
   * @param annualLeaveDays
   *//*
  @Override
  public EmployeeTimedDO addNewAnnualLeaveDays(final EmployeeDO employee, final LocalDate validfrom, final BigDecimal annualLeaveDays) {
    final EmployeeTimedDO newAttrRow = addNewTimeAttributeRow(employee, InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_GROUP_NAME);
    newAttrRow.setStartTime(PFDay.from(validfrom).getUtilDate());
    newAttrRow.putAttribute(InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_PROP_NAME, annualLeaveDays);
    return newAttrRow;
  }*/

  @Override
  public MonthlyEmployeeReport getReportOfMonth(final int year, final Integer month, final PFUserDO user) {
    MonthlyEmployeeReport monthlyEmployeeReport = new MonthlyEmployeeReport(this, vacationService, user, year, month);
    monthlyEmployeeReport.init();
    TimesheetFilter filter = new TimesheetFilter();
    filter.setDeleted(false);
    filter.setStartTime(monthlyEmployeeReport.getFromDate());
    filter.setStopTime(monthlyEmployeeReport.getToDate());
    filter.setUserId(user.getId());
    List<TimesheetDO> list = timesheetDao.getList(filter);
    PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    if (CollectionUtils.isNotEmpty(list)) {
      for (TimesheetDO sheet : list) {
        monthlyEmployeeReport.addTimesheet(sheet, timesheetDao.hasUserSelectAccess(loggedInUser, sheet, false));
      }
    }
    monthlyEmployeeReport.calculate();
    return monthlyEmployeeReport;
  }

  @Override
  public boolean isFulltimeEmployee(final EmployeeDO employee, final PFDateTime selectedDate) {
    final Date startOfMonth = selectedDate.getUtilDate();
    final PFDateTime dt = selectedDate.plusMonths(1).minusDays(1);
    final Date endOfMonth = dt.getUtilDate();
    log.error("****** Not yet migrated.");
    return true;

/*
    final List<EmployeeTimedDO> attrRows = timeableService
            .getAttrRowsWithinDateRange(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, startOfMonth, endOfMonth);

    final EmployeeTimedDO rowValidAtBeginOfMonth = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, selectedDate.getUtilDate());

    if (rowValidAtBeginOfMonth != null) {
      attrRows.add(rowValidAtBeginOfMonth);
    }

    return attrRows
            .stream()
            .map(row -> row.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME))
            .filter(Objects::nonNull)
            .anyMatch(s -> EmployeeStatus.FEST_ANGESTELLTER.getI18nKey().equals(s) || EmployeeStatus.BEFRISTET_ANGESTELLTER.getI18nKey().equals(s));
            */
  }

}
