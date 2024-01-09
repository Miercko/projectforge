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

import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.ExDate;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.time.DateHelper;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ExDateConverter extends PropertyConverter
{

  @Override
  public boolean toVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    if (!event.hasRecurrence() || event.getRecurrenceExDate() == null) {
      return false;
    }

    final List<Date> exDates = ICal4JUtils.parseCSVDatesAsICal4jDates(event.getRecurrenceExDate(), (!event.getAllDay()), ICal4JUtils.getUTCTimeZone());

    if (CollectionUtils.isEmpty(exDates)) {
      return false;
    }

    for (final Date date : exDates) {
      final DateList dateList;
      if (event.getAllDay()) {
        dateList = new DateList(Value.DATE);
      } else {
        dateList = new DateList();
        dateList.setUtc(true);
      }

      dateList.add(date);
      ExDate exDate;
      exDate = new ExDate(dateList);
      vEvent.getProperties().add(exDate);
    }

    return true;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    PropertyList<? extends Property> exDateProperties = vEvent.getProperties(Property.EXDATE);

    if (exDateProperties != null) {
      final boolean isAllDay = this.isAllDay(vEvent);

      List<String> exDateList = new ArrayList<>();
      exDateProperties.forEach(exDateProp -> {
        // find timezone of exdate
        final Parameter tzidParam = exDateProp.getParameter(Parameter.TZID);
        TimeZone timezone = null;
        if (tzidParam != null && tzidParam.getValue() != null) {
          timezone = TimeZone.getTimeZone(tzidParam.getValue());
        }

        if (timezone == null) {
          // ical4j uses the configured default timezone while parsing the ics file
          timezone = TimeZone.getDefault();
        }

        // parse ExDate with inherent timezone
        java.util.Date exDate = ICal4JUtils.parseICalDateString(exDateProp.getValue(), timezone);

        // add ExDate in UTC to list
        exDateList.add(ICal4JUtils.asICalDateString(exDate, DateHelper.UTC, isAllDay));
      });

      if (exDateList.isEmpty()) {
        event.setRecurrenceExDate(null);
      } else {
        event.setRecurrenceExDate(String.join(",", exDateList));
      }
    } else {
      event.setRecurrenceExDate(null);
    }

    return true;
  }
}
