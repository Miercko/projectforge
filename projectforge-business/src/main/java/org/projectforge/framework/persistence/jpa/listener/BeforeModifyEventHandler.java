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

package org.projectforge.framework.persistence.jpa.listener;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.events.*;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.JpaPfGenericPersistenceService;
import org.projectforge.framework.persistence.jpa.PfEmgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The listener interface for receiving checkPartOfTenantUpdate events. The class that is interested in processing a
 * checkPartOfTenantUpdate event implements this interface, and the object created with that class is registered with a
 * component using the component's <code>addCheckPartOfTenantUpdateListener<code> method. When the
 * checkPartOfTenantUpdate event occurs, that object's appropriate method is invoked.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Component
public class BeforeModifyEventHandler implements EmgrEventHandler<EmgrInitForModEvent>
{
  @Autowired
  protected AccessChecker accessChecker;

  @Autowired
  private JpaPfGenericPersistenceService genericPersistenceService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEvent(EmgrInitForModEvent event)
  {
    DbRecord<?> rec = event.getRecord();
    if (!(rec instanceof BaseDO)) {
      return;
    }
    BaseDO baseDo = (BaseDO) rec;
    PfEmgr emgr = (PfEmgr) event.getEmgr();
    if (!emgr.isCheckAccess()) {
      return;
    }
    AUserRightId aUserRightId = rec.getClass().getAnnotation(AUserRightId.class);
    if (aUserRightId != null && !aUserRightId.checkAccess()) {
      return; // skip right check
    }
    OperationType operationType;
    if (event instanceof EmgrInitForInsertEvent) {
      operationType = OperationType.INSERT;
    } else if (event instanceof EmgrInitForUpdateEvent) {
      // see AccessCheckUpdateCopyFilterListener
      return;
    } else if (event instanceof EmgrBeforeDeleteEvent) {
      operationType = OperationType.DELETE;
    } else {
      throw new IllegalArgumentException("Unsuported event to BeforeModifyEventHandler:" + event.getClass().getName());
    }
    accessChecker.checkRestrictedOrDemoUser();
    IUserRightId rightId = genericPersistenceService.getUserRight(baseDo);
    accessChecker.hasLoggedInUserAccess(rightId, baseDo, null, operationType, true);
  }

}
