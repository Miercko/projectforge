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

package org.projectforge.business.teamcal.event.ical;

import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import org.projectforge.business.teamcal.event.ical.converter.*;

import java.util.*;

public class ICalConverterStore
{
  public static final TimeZoneRegistry TIMEZONE_REGISTRY = TimeZoneRegistryFactory.getInstance().createRegistry();

  public static final String VEVENT_DTSTART = "VEVENT_DTSTART";
  public static final String VEVENT_DTEND = "VEVENT_DTEND";
  public static final String VEVENT_SUMMARY = "VEVENT_SUMMARY";
  public static final String VEVENT_UID = "VEVENT_UID";
  public static final String VEVENT_LOCATION = "VEVENT_LOCATION";
  public static final String VEVENT_CREATED = "VEVENT_CREATED";
  public static final String VEVENT_DTSTAMP = "VEVENT_DTSTAMP";
  public static final String VEVENT_LAST_MODIFIED = "VEVENT_LAST_MODIFIED";
  public static final String VEVENT_SEQUENCE = "VEVENT_SEQUENCE";
  public static final String VEVENT_ORGANIZER = "VEVENT_ORGANIZER";
  public static final String VEVENT_ORGANIZER_EDITABLE = "VEVENT_ORGANIZER_EDITABLE";
  public static final String VEVENT_TRANSP = "VEVENT_TRANSP";
  public static final String VEVENT_ALARM = "VEVENT_VALARM";
  public static final String VEVENT_DESCRIPTION = "VEVENT_DESCRIPTION";
  public static final String VEVENT_ATTENDEES = "VEVENT_ATTENDEE";
  public static final String VEVENT_RRULE = "VEVENT_RRULE";
  public static final String VEVENT_RECURRENCE_ID = "VEVENT_RECURRENCE_ID";
  public static final String VEVENT_EX_DATE = "VEVENT_EX_DATE";

  public static final List<String> FULL_LIST = new ArrayList<>(
      Arrays.asList(VEVENT_DTSTART, VEVENT_DTEND, VEVENT_SUMMARY, VEVENT_UID, VEVENT_CREATED, VEVENT_LOCATION, VEVENT_DTSTAMP, VEVENT_LAST_MODIFIED,
          VEVENT_SEQUENCE, VEVENT_ORGANIZER, VEVENT_TRANSP, VEVENT_ALARM, VEVENT_DESCRIPTION, VEVENT_ATTENDEES, VEVENT_RRULE, VEVENT_RECURRENCE_ID,
          VEVENT_EX_DATE));

  private static ICalConverterStore ourInstance = new ICalConverterStore();

  public static ICalConverterStore getInstance()
  {
    return ourInstance;
  }

  private Map<String, VEventComponentConverter> vEventConverters;

  private ICalConverterStore()
  {
    this.vEventConverters = new HashMap<>();

    this.registerVEventConverters();
  }

  public void registerVEventConverter(final String name, final VEventComponentConverter converter)
  {
    if (this.vEventConverters.containsKey(name)) {
      throw new IllegalArgumentException(String.format("A converter with name '%s' already exisits", name));
    }

    this.vEventConverters.put(name, converter);
  }

  public VEventComponentConverter getVEventConverter(final String name)
  {
    return this.vEventConverters.get(name);
  }

  private void registerVEventConverters()
  {
    this.registerVEventConverter(VEVENT_DTSTART, new DTStartConverter());
    this.registerVEventConverter(VEVENT_DTEND, new DTEndConverter());
    this.registerVEventConverter(VEVENT_SUMMARY, new SummaryConverter());
    this.registerVEventConverter(VEVENT_UID, new UidConverter());
    this.registerVEventConverter(VEVENT_LOCATION, new LocationConverter());
    this.registerVEventConverter(VEVENT_CREATED, new CreatedConverter());
    this.registerVEventConverter(VEVENT_DTSTAMP, new DTStampConverter());
    this.registerVEventConverter(VEVENT_LAST_MODIFIED, new LastModifiedConverter());
    this.registerVEventConverter(VEVENT_SEQUENCE, new SequenceConverter());
    this.registerVEventConverter(VEVENT_ORGANIZER, new OrganizerConverter(false));
    this.registerVEventConverter(VEVENT_ORGANIZER_EDITABLE, new OrganizerConverter(true));
    this.registerVEventConverter(VEVENT_TRANSP, new TransparencyConverter());
    this.registerVEventConverter(VEVENT_ALARM, new AlarmConverter());
    this.registerVEventConverter(VEVENT_DESCRIPTION, new DescriptionConverter());
    this.registerVEventConverter(VEVENT_ATTENDEES, new AttendeeConverter());
    this.registerVEventConverter(VEVENT_RRULE, new RRuleConverter());
    this.registerVEventConverter(VEVENT_RECURRENCE_ID, new RecurrenceIdConverter());
    this.registerVEventConverter(VEVENT_EX_DATE, new ExDateConverter());
  }
}
