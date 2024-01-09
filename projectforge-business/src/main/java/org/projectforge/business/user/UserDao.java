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

package org.projectforge.business.user;

import de.micromata.genome.jpa.Clauses;
import de.micromata.genome.jpa.CriteriaUpdate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.login.Login;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPasswordDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.utils.Crypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class UserDao extends BaseDao<PFUserDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserDao.class);

  private static final SortProperty[] DEFAULT_SORT_PROPERTIES = new SortProperty[]{new SortProperty("firstname"), new SortProperty("lastname")};

  @Autowired
  private ApplicationContext applicationContext;

  private UserPasswordDao userPasswordDao;

  public UserDao() {
    super(PFUserDO.class);
  }

  public QueryFilter getDefaultFilter() {
    final QueryFilter queryFilter = new QueryFilter(null);
    queryFilter.add(QueryFilter.eq("deleted", false));
    return queryFilter;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#createQueryFilter(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  public QueryFilter createQueryFilter(final BaseSearchFilter filter) {
    return new QueryFilter(filter);
  }

  @Override
  public SortProperty[] getDefaultSortProperties() {
    return DEFAULT_SORT_PROPERTIES;
  }

  @Override
  public List<PFUserDO> getList(final BaseSearchFilter filter) {
    final PFUserFilter myFilter;
    if (filter instanceof PFUserFilter) {
      myFilter = (PFUserFilter) filter;
    } else {
      myFilter = new PFUserFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(myFilter);
    if (myFilter.getDeactivatedUser() != null) {
      queryFilter.add(QueryFilter.eq("deactivated", myFilter.getDeactivatedUser()));
    }
    if (Login.getInstance().hasExternalUsermanagementSystem()) {
      // Check hasExternalUsermngmntSystem because otherwise the filter is may-be preset for an user and the user can't change the filter
      // (because the fields aren't visible).
      if (myFilter.getRestrictedUser() != null) {
        queryFilter.add(QueryFilter.eq("restrictedUser", myFilter.getRestrictedUser()));
      }
      if (myFilter.getLocalUser() != null) {
        queryFilter.add(QueryFilter.eq("localUser", myFilter.getLocalUser()));
      }
    }
    if (myFilter.getHrPlanning() != null) {
      queryFilter.add(QueryFilter.eq("hrPlanning", myFilter.getHrPlanning()));
    }
    List<PFUserDO> list = getList(queryFilter);
    if (myFilter.getIsAdminUser() != null) {
      final List<PFUserDO> origList = list;
      list = new LinkedList<>();
      for (final PFUserDO user : origList) {
        if (myFilter.getIsAdminUser() == accessChecker.isUserMemberOfAdminGroup(user, false)) {
          list.add(user);
        }
      }
    }
    return list;
  }

  public Collection<Integer> getAssignedGroups(final PFUserDO user) {
    return getUserGroupCache().getUserGroups(user);
  }

  public List<UserRightDO> getUserRights(final Integer userId) {
    return getUserGroupCache().getUserRights(userId);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#onChange(ExtendedBaseDO, ExtendedBaseDO)
   */
  @Override
  protected void onChange(final PFUserDO obj, final PFUserDO dbObj) {
    super.onChange(obj, dbObj);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(ExtendedBaseDO)
   */
  @Override
  protected void afterSaveOrModify(final PFUserDO obj) {
    if (!obj.isMinorChange()) {
      getUserGroupCache().setExpired();
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, OperationType, boolean)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final PFUserDO obj, final PFUserDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * @return false, if no admin user and the context user is not at minimum in one groups assigned to the given user or
   * false. Also deleted and deactivated users are only visible for admin users.
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUserSelectAccess(PFUserDO, ExtendedBaseDO, boolean) )
   * @see AccessChecker#areUsersInSameGroup(PFUserDO, PFUserDO)
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final PFUserDO obj, final boolean throwException) {
    boolean result = accessChecker.isUserMemberOfAdminGroup(user)
        || accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP);
    log.debug("UserDao hasSelectAccess. Check user member of admin, finance or controlling group: " + result);
    if (!result && obj.hasSystemAccess()) {
      result = accessChecker.areUsersInSameGroup(user, obj);
      log.debug("UserDao hasSelectAccess. Caller user: " + user.getUsername() + " Check user: " + obj.getUsername()
          + " Check user in same group: " + result);
    }
    if (throwException && !result) {
      throw new AccessException(user, AccessType.GROUP, OperationType.SELECT);
    }
    return result;
  }

  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user) {
    return accessChecker.isUserMemberOfAdminGroup(user, false);
  }

  /**
   * Update user after login success.
   *
   * @param user the user
   */
  public void updateUserAfterLoginSuccess(PFUserDO user) {
    PfEmgrFactory.get().runInTrans((emgr) -> {
      CriteriaUpdate<PFUserDO> cu = CriteriaUpdate.createUpdate(PFUserDO.class);
      cu
          .set("lastLogin", new Date())
          .set("loginFailures", 0)
          .addWhere(Clauses.equal("id", user.getId()));
      return emgr.update(cu);
    });
  }

  public void updateIncrementLoginFailure(String userName) {
    PfEmgrFactory.get().runInTrans((emgr) -> {
      CriteriaUpdate<PFUserDO> cu = CriteriaUpdate.createUpdate(PFUserDO.class);
      cu
          .setExpression("loginFailures", "loginFailures + 1")
          .addWhere(Clauses.equal("username", userName));
      return emgr.update(cu);
    });
  }

  /**
   * Does an user with the given username already exists? Works also for existing users (if username was modified).
   */
  public boolean doesUsernameAlreadyExist(final PFUserDO user) {
    Validate.notNull(user);
    PFUserDO dbUser = null;
    if (user.getId() == null) {
      // New user
      dbUser = getInternalByName(user.getUsername());
    } else {
      // user already exists. Check maybe changed username:
      dbUser = SQLHelper.ensureUniqueResult(em.createNamedQuery(PFUserDO.FIND_OTHER_USER_BY_USERNAME, PFUserDO.class)
          .setParameter("username", user.getUsername())
          .setParameter("id", user.getId()));
    }
    return dbUser != null;
  }

  public PFUserDO getInternalByName(final String username) {
    return SQLHelper.ensureUniqueResult(em.createNamedQuery(PFUserDO.FIND_BY_USERNAME, PFUserDO.class)
        .setParameter("username", username));
  }

  /**
   * User can modify own setting, this method ensures that only such properties will be updated, the user's are allowed
   * to.
   *
   * @param user
   */
  public void updateMyAccount(final PFUserDO user) {
    accessChecker.checkRestrictedOrDemoUser();
    final PFUserDO contextUser = ThreadLocalUserContext.getUser();
    Validate.isTrue(Objects.equals(user.getId(), contextUser.getId()));
    final PFUserDO dbUser = internalGetById(user.getId());
    dbUser.setTimeZone(user.getTimeZone());
    dbUser.setDateFormat(user.getDateFormat());
    dbUser.setExcelDateFormat(user.getExcelDateFormat());
    dbUser.setTimeNotation(user.getTimeNotation());
    dbUser.setLocale(user.getLocale());
    dbUser.setPersonalPhoneIdentifiers(user.getPersonalPhoneIdentifiers());
    dbUser.setSshPublicKey(user.getSshPublicKey());
    dbUser.setFirstname(user.getFirstname());
    dbUser.setLastname(user.getLastname());
    dbUser.setDescription(user.getDescription());
    dbUser.setGpgPublicKey(user.getGpgPublicKey());
    dbUser.setSshPublicKey(user.getSshPublicKey());
    final ModificationStatus result = internalUpdate(dbUser);
    if (result != ModificationStatus.NONE) {
      log.info("Object updated: " + dbUser.toString());
      copyValues(user, contextUser);
    } else {
      log.info("No modifications detected (no update needed): " + dbUser.toString());
    }
    getUserGroupCache().updateUser(contextUser);
  }

  /**
   * Gets history entries of super and adds all history entries of the AuftragsPositionDO children.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final PFUserDO obj) {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (!hasLoggedInUserHistoryAccess(obj, false)) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getRights())) {
      for (final UserRightDO right : obj.getRights()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(right);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName(right.getRightIdString() + ":" + entry.getPropertyName()); // Prepend number of positon.
          } else {
            entry.setPropertyName(String.valueOf(right.getRightIdString()));
          }
        }
        list.addAll(entries);
      }
    }
    list.sort(new Comparator<DisplayHistoryEntry>() {
      @Override
      public int compare(final DisplayHistoryEntry o1, final DisplayHistoryEntry o2) {
        return (o2.getTimestamp().compareTo(o1.getTimestamp()));
      }
    });
    return list;
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * Re-index all dependent objects only if the username, first or last name was changed.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#wantsReindexAllDependentObjects(ExtendedBaseDO, ExtendedBaseDO)
   */
  @Override
  protected boolean wantsReindexAllDependentObjects(final PFUserDO obj, final PFUserDO dbObj) {
    if (!super.wantsReindexAllDependentObjects(obj, dbObj)) {
      return false;
    }
    return !StringUtils.equals(obj.getUsername(), dbObj.getUsername())
        || !StringUtils.equals(obj.getFirstname(), dbObj.getFirstname())
        || !StringUtils.equals(obj.getLastname(), dbObj.getLastname());
  }

  @Override
  public PFUserDO newInstance() {
    return new PFUserDO();
  }

  public List<PFUserDO> findByUsername(String username) {
    return em.createNamedQuery(PFUserDO.FIND_BY_USERNAME, PFUserDO.class)
        .setParameter("username", username)
        .getResultList();
  }

  /**
   * Encrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
   * anymore.
   *
   * @param data The data to encrypt.
   * @return The encrypted data.
   */
  public String encrypt(String data) {
    final String password = getPasswordOfUser(ThreadLocalUserContext.getUserId());
    if (password == null) {
      return null;
    }
    return Crypt.encrypt(password, data);
  }

  /**
   * Decrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
   * anymore.
   *
   * @param encrypted The data to encrypt.
   * @return The decrypted data.
   */
  public String decrypt(String encrypted) {
    return decrypt(encrypted, ThreadLocalUserContext.getUserId());
  }

  /**
   * Decrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
   * anymore.
   *
   * @param encrypted The data to encrypt.
   * @param userId    Use the password of the given user (used by CookieService, because user isn't yet logged-in).
   * @return The decrypted data.
   * @see UserDao#decrypt(String)
   */
  public String decrypt(String encrypted, Integer userId) {
    final String password = getPasswordOfUser(userId);
    if (password == null) {
      return null;
    }
    return Crypt.decrypt(password, encrypted);
  }

  private String getPasswordOfUser(Integer userId) {
    if (userPasswordDao == null) {
      userPasswordDao = applicationContext.getBean(UserPasswordDao.class);
    }
    final UserPasswordDO passwordObj = userPasswordDao.internalGetByUserId(userId);
    if (passwordObj == null) {
      log.warn("Can't encrypt data. Password for user " + userId + " not found.");
      return null;
    }
    if (StringUtils.isBlank(passwordObj.getPasswordHash())) {
      log.warn("Can't encrypt data. Password of user '" + userId + " not found.");
      return null;
    }
    return passwordObj.getPasswordHash();
  }
}
