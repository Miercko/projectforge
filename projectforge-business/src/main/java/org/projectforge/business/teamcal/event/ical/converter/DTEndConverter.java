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

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDateTimeUtils;

import java.util.Date;

import static org.projectforge.business.teamcal.event.ical.ICalConverterStore.TIMEZONE_REGISTRY;

public class DTEndConverter extends PropertyConverter {
  @Override
  public Property toVEvent(final TeamEventDO event) {
    net.fortuna.ical4j.model.Date date;

    if (event.getAllDay()) {
      final Date endUtc = PFDateTimeUtils.getUTCBeginOfDay(event.getEndDate());
      final PFDateTime dateTime = PFDateTime.from(endUtc); // not null
      // TODO sn should not be done
      // requires plus 1 because one day will be omitted by calendar.
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(dateTime.plusDays(1).getUtilDate());
      date = new net.fortuna.ical4j.model.Date(fortunaEndDate.getTime());
    } else {
      date = new DateTime(event.getEndDate());
      ((net.fortuna.ical4j.model.DateTime) date).setTimeZone(TIMEZONE_REGISTRY.getTimeZone(event.getTimeZone().getID()));
    }

    return new DtEnd(date);
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent) {
    final boolean isAllDay = this.isAllDay(vEvent);

    if (vEvent.getProperties().getProperties(Property.DTEND).isEmpty()) {
      return false;
    }

    if (isAllDay) {
      // TODO sn change behaviour to iCal standard
      final PFDateTime dateTime = PFDateTime.from(vEvent.getEndDate().getDate()); // not null
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(dateTime.plusDays(-1).getUtilDate());
      event.setEndDate(new Date(fortunaEndDate.getTime()));
    } else {
      event.setEndDate(ICal4JUtils.getUtilDate(vEvent.getEndDate().getDate()));
    }

    return true;
  }
}
