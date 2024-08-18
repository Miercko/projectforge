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

package org.projectforge.framework.configuration;

import org.apache.commons.lang3.Validate;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Configuration values persistet in the data base. Please access the configuration parameters via
 * {@link Configuration}.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class ConfigurationDao extends BaseDao<ConfigurationDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigurationDao.class);

  @Autowired
  private ApplicationContext applicationContext;

  /**
   * Force reload of the Configuration cache.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(ExtendedBaseDO)
   * @see Configuration#setExpired()
   */
  @Override
  public void afterSaveOrModify(final ConfigurationDO obj) {
    Configuration.getInstance().setExpired();
  }

  /**
   * Checks and creates missing data base entries. Updates also out-dated descriptions.
   */
  public void checkAndUpdateDatabaseEntries() {
    final List<ConfigurationDO> list = internalLoadAll();
    final Set<String> params = new HashSet<>();
    for (final ConfigurationParam param : ConfigurationParam.values()) {
      checkAndUpdateDatabaseEntry(param, list, params);
    }
    for (final ConfigurationDO entry : list) {
      if (!params.contains(entry.getParameter())) {
        log.error("Unknown configuration entry. Mark as deleted: " + entry.getParameter());
        internalMarkAsDeleted(entry);
      }
    }
  }

  public ConfigurationDO getEntry(final IConfigurationParam param) {
    Validate.notNull(param);
    return SQLHelper.ensureUniqueResult(em
        .createNamedQuery(ConfigurationDO.FIND_BY_PARAMETER, ConfigurationDO.class)
        .setParameter("parameter", param.getKey()));
  }

  public Object getValue(final IConfigurationParam parameter) {
    return getValue(parameter, getEntry(parameter));
  }

  public Object getValue(final IConfigurationParam parameter, final ConfigurationDO configurationDO) {
    if (parameter.getType().isIn(ConfigurationType.STRING, ConfigurationType.TEXT)) {
      if (configurationDO == null) {
        return parameter.getDefaultStringValue();
      }
      final String result = configurationDO.getStringValue();
      if (result != null) {
        return result;
      } else {
        return parameter.getDefaultStringValue();
      }
    } else if (parameter.getType().isIn(ConfigurationType.FLOAT, ConfigurationType.PERCENT)) {
      if (configurationDO == null) {
        return BigDecimal.ZERO;
      }
      return configurationDO.getFloatValue();
    } else if (parameter.getType() == ConfigurationType.INTEGER) {
      if (configurationDO == null) {
        return 0;
      }
      return configurationDO.getIntValue();
    } else if (parameter.getType() == ConfigurationType.BOOLEAN) {
      if (configurationDO == null) {
        return null;
      }
      return configurationDO.getBooleanValue();
    } else if (parameter.getType() == ConfigurationType.CALENDAR) {
      if (configurationDO == null) {
        return null;
      }
      final Integer calendarId = configurationDO.getCalendarId();
      return calendarId;
    } else if (parameter.getType() == ConfigurationType.TIME_ZONE) {
      String timezoneId = configurationDO != null ? configurationDO.getTimeZoneId() : null;
      if (timezoneId == null) {
        timezoneId = parameter.getDefaultStringValue();
      }
      if (timezoneId != null) {
        return TimeZone.getTimeZone(timezoneId);
      }
      return null;
    }
    throw new UnsupportedOperationException("Type unsupported: " + parameter.getType());
  }

  public ConfigurationDao() {
    super(ConfigurationDO.class);
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final ConfigurationDO obj, final ConfigurationDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  @Override
  public ConfigurationDO newInstance() {
    throw new UnsupportedOperationException();
  }

  private void checkAndUpdateDatabaseEntry(final IConfigurationParam param, final List<ConfigurationDO> list, final Set<String> params) {
    params.add(param.getKey());

    // find the entry and update it
    for (final ConfigurationDO configuration : list) {
      if (param.getKey().equals(configuration.getParameter())) {
        boolean modified = false;
        if (configuration.getConfigurationType() != param.getType()) {
          log.info("Updating configuration type of configuration entry: " + param);
          configuration.internalSetConfigurationType(param.getType());
          modified = true;
        }
        if (configuration.getDeleted()) {
          log.info("Restore deleted configuration entry: " + param);
          configuration.setDeleted(false);
          modified = true;
        }
        if (modified) {
          internalUpdate(configuration);
        }
        return;
      }
    }

    // Entry does not exist: Create entry:
    log.info("Entry does not exist. Creating parameter '" + param.getKey() + "'.");
    final ConfigurationDO configuration = new ConfigurationDO();
    configuration.setParameter(param.getKey());
    configuration.setConfigurationType(param.getType());
    if (param.getType().isIn(ConfigurationType.STRING, ConfigurationType.TEXT)) {
      configuration.setValue(param.getDefaultStringValue());
    }
    if (param.getType().isIn(ConfigurationType.INTEGER)) {
      configuration.setIntValue(param.getDefaultIntValue());
    }
    if (param.getType().isIn(ConfigurationType.BOOLEAN)) {
      configuration.setStringValue(String.valueOf(param.getDefaultBooleanValue()));
    }
    internalSave(configuration);
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }
}
