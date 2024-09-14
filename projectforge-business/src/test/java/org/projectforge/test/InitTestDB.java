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

package org.projectforge.test;

import org.apache.commons.lang3.Validate;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2ArtDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.user.*;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.framework.time.DateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

@Component
public class InitTestDB {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InitTestDB.class);

  @Autowired
  private UserService userService;

  @Autowired
  private AccessDao accessDao;

  @Autowired
  private DatabaseService databaseService;

  @Autowired
  private ConfigurationDao configurationDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private Kost2Dao kost2Dao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private TaskTree taskTree;

  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private Kost2ArtDao kost2ArtDao;

  @Autowired
  private EmployeeDao employeeDao;

  @Autowired
  private UserGroupCache userGroupCache;

  @Autowired
  private UserRightDao userRightDao;

  private final Map<String, GroupDO> groupMap = new HashMap<>();

  private final Map<String, PFUserDO> userMap = new HashMap<>();

  private final Map<String, TaskDO> taskMap = new HashMap<>();

  public void putUser(final PFUserDO user) {
    this.userMap.put(user.getUsername(), user);
  }

  public PFUserDO addUser(final String username) {
    return addUser(username, null);
  }

  public PFUserDO addUser(final String username, final char[] password) {
    PFUserDO user = new PFUserDO();
    user.setUsername(username);
    user.setLocale(Locale.ENGLISH);
    user.setDateFormat("dd/MM/yyyy");
    user.setEmail("devnull@localhost");
    user = addUser(user);
    if (password != null) {
      userService.encryptAndSavePassword(user, password, false);
    }
    return user;
  }

  public PFUserDO addUser(final PFUserDO user) {
    Set<UserRightDO> userRights = new HashSet<>(user.getRights());
    user.getRights().clear();
    userService.save(user);
    userRights.forEach(right -> userRightDao.internalSave(right));
    putUser(user);
    if (user.getUsername().equals(AbstractTestBase.ADMIN)) {
      AbstractTestBase.ADMIN_USER = user;
    }
    return user;
  }

  public PFUserDO getUser(final String userName) {
    return this.userMap.get(userName);
  }

  public void clearUsers() {
    this.userMap.clear();
  }

  public void putGroup(final GroupDO group) {
    this.groupMap.put(group.getName(), group);
  }

  public GroupDO addGroup(final String groupname, final String... usernames) {
    final GroupDO group = new GroupDO();
    group.setName(groupname);
    if (usernames != null) {
      final Set<PFUserDO> col = new HashSet<>();
      for (final String username : usernames) {
        col.add(getUser(username));
      }
      group.setAssignedUsers(col);
    }
    groupDao.internalSave(group);
    putGroup(group);
    userGroupCache.setExpired();
    return group;
  }

  public GroupDO getGroup(final String groupName) {
    return this.groupMap.get(groupName);
  }

  public void putTask(final TaskDO task) {
    this.taskMap.put(task.getTitle(), task);
  }

  public TaskDO addTask(final String taskName, final String parentTaskName) {
    Validate.isTrue(taskName.length() <= TaskDO.TITLE_LENGTH);
    return addTask(taskName, parentTaskName, null);
  }

  public TaskDO addTask(final String taskName, final String parentTaskName, final String shortDescription) {
    TaskDO task = new TaskDO();
    task.setTitle(taskName);
    if (parentTaskName != null) {
      task.setParentTask(getTask(parentTaskName));
    }
    if (shortDescription != null) {
      task.setShortDescription(shortDescription);
    }
    final Serializable id = taskDao.internalSave(task);
    task = taskDao.internalGetById(id);
    putTask(task);
    return task;
  }

  public TaskDO getTask(final String taskName) {
    return this.taskMap.get(taskName);
  }

  public TimesheetDO addTimesheet(final PFUserDO user, final TaskDO task, final Date startTime,
                                  final Date stopTime, final String description) {
    final TimesheetDO timesheet = new TimesheetDO();
    timesheet.setDescription(description);
    timesheet.setStartTime(startTime);
    timesheet.setStopTime(stopTime);
    timesheet.setTask(task);
    timesheet.setUser(user);
    timesheetDao.internalSave(timesheet);
    return timesheet;
  }

  public ProjektDO addProjekt(final KundeDO kunde, final Integer projektNummer, final String projektName,
                              final Long... kost2ArtIds) {
    final ProjektDO projekt = new ProjektDO();
    projekt.setNummer(projektNummer);
    projekt.setName(projektName);
    if (kunde != null) {
      projektDao.setKunde(projekt, kunde.getId());
    }
    projektDao.save(projekt);
    if (kost2ArtIds != null) {
      for (final Long id : kost2ArtIds) {
        final Kost2DO kost2 = new Kost2DO();
        kost2.setProjekt(projekt);
        kost2.setNummernkreis(5);
        if (kunde != null) {
          kost2.setBereich(kunde.getId().intValue());
        }
        kost2.setTeilbereich(projekt.getNummer());
        kost2Dao.setKost2Art(kost2, id);
        kost2Dao.save(kost2);
      }
    }
    return projekt;
  }

  public void initDatabase() {
    final PFUserDO origUser = ThreadLocalUserContext.getUser();
    final PFUserDO initUser = new PFUserDO();
    initUser.setUsername("Init-database-pseudo-user");
    initUser.setId(-1L);
    initUser.addRight(new UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE));
    try {
      ThreadLocalUserContext.setUser(initUser);
      initConfiguration();
      initUsers();
      databaseService.insertGlobalAddressbook(AbstractTestBase.ADMIN_USER);
      initGroups();
      initTaskTree();
      initAccess();
      initKost2Arts();
      initEmployees();
    } finally {
      ThreadLocalUserContext.setUser(origUser);
    }
  }

  private void initEmployees() {
    PFUserDO user = addUser(AbstractTestBase.TEST_EMPLOYEE_USER, AbstractTestBase.TEST_EMPLOYEE_USER_PASSWORD);
    EmployeeDO e = new EmployeeDO();
    e.setUser(user);
    employeeDao.internalSave(e);
  }

  private void initConfiguration() {
    configurationDao.checkAndUpdateDatabaseEntries();
    final ConfigurationDO entry = configurationDao.getEntry(ConfigurationParam.DEFAULT_TIMEZONE);
    entry.setTimeZone(DateHelper.EUROPE_BERLIN);
    configurationDao.internalUpdate(entry);
  }

  private void initUsers() {
    addUser(AbstractTestBase.ADMIN);
    addUser(AbstractTestBase.TEST_ADMIN_USER, AbstractTestBase.TEST_ADMIN_USER_PASSWORD);
    PFUserDO user = new PFUserDO();
    user.setUsername(AbstractTestBase.TEST_FINANCE_USER);
    user//
        .addRight(new UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_ACCOUNTS, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_COST_UNIT, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.PM_HR_PLANNING, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.ORGA_CONTRACTS, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE)); //
    addUser(user);
    user = new PFUserDO();
    user.setUsername(AbstractTestBase.TEST_HR_USER);
    user//
        .addRight(new UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE)); //
    addUser(user);
    user = new PFUserDO();
    user.setUsername(AbstractTestBase.TEST_FULL_ACCESS_USER);
    user//
        .addRight(new UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_ACCOUNTS, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_COST_UNIT, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.PM_HR_PLANNING, UserRightValue.READWRITE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE)); //
    user = addUser(user);
    userService.encryptAndSavePassword(user, AbstractTestBase.TEST_FULL_ACCESS_USER_PASSWORD, false);
    addUser(AbstractTestBase.TEST_USER, AbstractTestBase.TEST_USER_PASSWORD);
    addUser(AbstractTestBase.TEST_USER2);
    user = addUser(AbstractTestBase.TEST_DELETED_USER, AbstractTestBase.TEST_DELETED_USER_PASSWORD);
    userService.markAsDeleted(user);
    addUser("user1");
    addUser("user2");
    addUser("user3");
    addUser(AbstractTestBase.TEST_CONTROLLING_USER);
    addUser(AbstractTestBase.TEST_MARKETING_USER);
    addUser(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
    user = new PFUserDO();
    user.setUsername(AbstractTestBase.TEST_PROJECT_ASSISTANT_USER);
    user.addRight(new UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.PARTLYREADWRITE));
    addUser(user);
  }

  private void initGroups() {
    addGroup(AbstractTestBase.ADMIN_GROUP,
        "PFAdmin", AbstractTestBase.TEST_ADMIN_USER, AbstractTestBase.TEST_FULL_ACCESS_USER);
    addGroup(AbstractTestBase.FINANCE_GROUP,
        AbstractTestBase.TEST_FINANCE_USER, AbstractTestBase.TEST_FULL_ACCESS_USER);
    addGroup(AbstractTestBase.CONTROLLING_GROUP,
        AbstractTestBase.TEST_CONTROLLING_USER, AbstractTestBase.TEST_FULL_ACCESS_USER);
    addGroup(AbstractTestBase.HR_GROUP, AbstractTestBase.TEST_FULL_ACCESS_USER, AbstractTestBase.TEST_HR_USER);
    addGroup(AbstractTestBase.ORGA_GROUP, AbstractTestBase.TEST_FULL_ACCESS_USER);
    addGroup(AbstractTestBase.PROJECT_MANAGER,
        AbstractTestBase.TEST_PROJECT_MANAGER_USER, AbstractTestBase.TEST_FULL_ACCESS_USER);
    addGroup(AbstractTestBase.PROJECT_ASSISTANT,
        AbstractTestBase.TEST_PROJECT_ASSISTANT_USER, AbstractTestBase.TEST_FULL_ACCESS_USER);
    addGroup(AbstractTestBase.MARKETING_GROUP,
        AbstractTestBase.TEST_MARKETING_USER, AbstractTestBase.TEST_FULL_ACCESS_USER);
    addGroup(AbstractTestBase.TEST_GROUP, AbstractTestBase.TEST_USER, AbstractTestBase.TEST_USER2);
    addGroup("group1", "user1", "user2");
    addGroup("group2", "user1");
    addGroup("group3");
  }

  private void initKost2Arts() {
    addKost2Art(0L, "Akquise");
    addKost2Art(1L, "Research");
    addKost2Art(2L, "Realization");
    addKost2Art(3L, "Systemadministration");
    addKost2Art(4L, "Travel costs");
  }

  private void addKost2Art(final Long id, final String name) {
    final Kost2ArtDO kost2Art = new Kost2ArtDO();
    kost2Art.setId(id);
    kost2Art.setName("Akquise");
    kost2ArtDao.internalSave(kost2Art);
  }

  private void initTaskTree() {
    if (log.isDebugEnabled()) {
      log.debug("Setting taskTree.expired: " + taskTree);
    }
    taskTree.clear();
    if (log.isDebugEnabled()) {
      log.debug("TaskTree after reload: " + taskTree);
    }
    if (taskTree.getRootTaskNode() == null) {
      addTask("root", null);
    } else {
      putTask(taskTree.getRootTaskNode().getTask());
    }
    addTask("1", "root");
    addTask("1.1", "1");
    addTask("1.2", "1");
    addTask("1.1.1", "1.1");
    addTask("1.1.2", "1.1");
    addTask("2", "root");
    addTask("2.1", "2");
    addTask("2.2", "2");
  }

  public GroupTaskAccessDO createGroupTaskAccess(final GroupDO group, final TaskDO task) {
    Validate.notNull(group);
    Validate.notNull(task);
    final GroupTaskAccessDO access = new GroupTaskAccessDO();
    access.setGroup(group);
    access.setTask(task);
    accessDao.internalSave(access);
    return access;
  }

  public GroupTaskAccessDO createGroupTaskAccess(final GroupDO group, final TaskDO task, final AccessType accessType,
                                                 final boolean accessSelect, final boolean accessInsert, final boolean accessUpdate, final boolean accessDelete) {
    final GroupTaskAccessDO access = createGroupTaskAccess(group, task);
    final AccessEntryDO entry = access.ensureAndGetAccessEntry(accessType);
    entry.setAccess(accessSelect, accessInsert, accessUpdate, accessDelete);
    accessDao.internalUpdate(access);
    return access;
  }

  private void initAccess() {
    GroupTaskAccessDO access = createGroupTaskAccess(getGroup("group1"), getTask("1"));
    final AccessEntryDO entry = access.ensureAndGetAccessEntry(AccessType.TASKS);
    entry.setAccess(true, true, true, true);
    accessDao.internalUpdate(access);

    // Create some test tasks with test access:
    addTask("testAccess", "root");
    addTask("ta_1_siud", "testAccess", "Testuser has all access rights: select, insert, update, delete");
    addTask("ta_1_1", "ta_1_siud");
    access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_1_siud"));
    setAllAccessEntries(access, true, true, true, true);
    addTask("ta_2_siux", "testAccess", "Testuser has the access rights: select, insert, update");
    addTask("ta_2_1", "ta_2_siux");
    access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_2_siux"));
    setAllAccessEntries(access, true, true, true, false);
    addTask("ta_3_sxxx", "testAccess", "Testuser has only select rights: select");
    addTask("ta_3_1", "ta_3_sxxx");
    access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_3_sxxx"));
    setAllAccessEntries(access, true, false, false, false);
    addTask("ta_4_xxxx", "testAccess", "Testuser has no rights.");
    addTask("ta_4_1", "ta_4_xxxx");
    access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_4_xxxx"));
    setAllAccessEntries(access, false, false, false, false);
    addTask("ta_5_sxux", "testAccess", "Testuser has select and update rights.");
    addTask("ta_5_1", "ta_5_sxux");
    access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_5_sxux"));
    setAllAccessEntries(access, true, false, true, false);
  }

  private void setAllAccessEntries(final GroupTaskAccessDO access, final boolean selectAccess,
                                   final boolean insertAccess,
                                   final boolean updateAccess, final boolean deleteAccess) {
    AccessEntryDO entry = access.ensureAndGetAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT);
    entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess);
    entry = access.ensureAndGetAccessEntry(AccessType.TASKS);
    entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess);
    entry = access.ensureAndGetAccessEntry(AccessType.TIMESHEETS);
    entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess);
    entry = access.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS);
    entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess);
    accessDao.internalUpdate(access);
  }
}
