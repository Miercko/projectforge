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

package org.projectforge.framework.persistence.api;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;

import java.util.Date;

public class SearchFilter extends BaseSearchFilter
{
  private static final long serialVersionUID = 5850672386075331163L;

  private TaskDO task;

  private PFUserDO modifiedByUser;

  public SearchFilter()
  {
    this.maxRows = 3;
  }

  public void updateUseModificationFilterFlag() {
    this.useModificationFilter = this.modifiedByUserId != null || this.startTimeOfModification != null || this.stopTimeOfModification != null;
  }


  @Override
  public SearchFilter setStartTimeOfModification(final Date startTimeOfLastModification)
  {
    if (startTimeOfLastModification == null) {
      super.setStartTimeOfModification(null);
      return this;
    }
    final PFDateTime dateTime = PFDateTime.from(startTimeOfLastModification).withPrecision(DatePrecision.MILLISECOND).getBeginOfDay();
    super.setStartTimeOfModification(dateTime.getUtilDate());
    return this;
  }

  @Override
  public SearchFilter setStopTimeOfModification(final Date stopTimeOfLastModification)
  {
    if (stopTimeOfLastModification == null) {
      super.setStopTimeOfModification(null);
      return this;
    }
    final PFDateTime dateTime = PFDateTime.from(stopTimeOfLastModification).withPrecision(DatePrecision.MILLISECOND)
        .getEndOfDay();
    super.setStopTimeOfModification(dateTime.getUtilDate());
    return this;
  }

  public PFUserDO getModifiedByUser()
  {
    return modifiedByUser;
  }

  public void setModifiedByUser(final PFUserDO modifiedByUser)
  {
    this.modifiedByUser = modifiedByUser;
    this.modifiedByUserId = modifiedByUser != null ? modifiedByUser.getId() : null;
  }

  public TaskDO getTask()
  {
    return task;
  }

  public Integer getTaskId()
  {
    return task != null ? task.getId() : null;
  }

  public void setTask(final TaskDO task)
  {
    this.task = task;
  }

  /**
   * @return true, if no field for search is set (ignores task and searchHistory).
   */
  public boolean isEmpty()
  {
    return StringUtils.isEmpty(searchString)
        && modifiedByUserId == null
        && startTimeOfModification == null
        && stopTimeOfModification == null;
  }

  @Override
  public SearchFilter reset()
  {
    super.reset();
    this.searchString = "";
    this.useModificationFilter = false;
    this.modifiedByUserId = null;
    this.startTimeOfModification = null;
    this.stopTimeOfModification = null;
    this.task = null;
    return this;
  }
}
