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
import de.micromata.genome.jpa.events.EmgrEventHandler;
import de.micromata.genome.jpa.events.EmgrInitForInsertEvent;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.springframework.stereotype.Component;

/**
 * Bookkeeping of created and lastupdate fields.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Component
public class InitForInsertEventListener implements EmgrEventHandler<EmgrInitForInsertEvent>
{
  @Override
  public void onEvent(EmgrInitForInsertEvent event)
  {
    DbRecord<?> rec = event.getRecord();
    if (!(rec instanceof ExtendedBaseDO)) {
      return;
    }
    ExtendedBaseDO extb = (ExtendedBaseDO) rec;
    extb.setCreated();
    extb.setLastUpdate();
  }

}
