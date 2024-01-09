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

package org.projectforge.business.teamcal.event.ical.converter;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RecurrenceId;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.time.DateHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class RecurrenceIdConverter extends PropertyConverter
{
  private SimpleDateFormat format;
  private SimpleDateFormat formatInclZ;

  public RecurrenceIdConverter()
  {
    format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    formatInclZ = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
  }

  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    // TODO
    return null;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    RecurrenceId recurrenceId = vEvent.getRecurrenceId();

    if (recurrenceId == null) {
      return false;
    }

    try {
      synchronized (format) {
        if (recurrenceId.getTimeZone() != null) {
          TimeZone timezone = TimeZone.getTimeZone(recurrenceId.getTimeZone().getID());
          format.setTimeZone(timezone != null ? timezone : DateHelper.UTC);
          formatInclZ.setTimeZone(timezone != null ? timezone : DateHelper.UTC);

          Date date = null;
          try {
            date = format.parse(recurrenceId.getValue());
          } catch (ParseException e) {
            date = formatInclZ.parse(recurrenceId.getValue());
          }

          if (date != null) {
            format.setTimeZone(DateHelper.UTC);
            event.setRecurrenceReferenceId(format.format(date));
          }
        } else {
          format.setTimeZone(DateHelper.UTC);
          event.setRecurrenceReferenceId(format.format(recurrenceId.getDate()));
        }
      }
    } catch (Exception e) {
      return false;
    }

    return true;
  }
}
