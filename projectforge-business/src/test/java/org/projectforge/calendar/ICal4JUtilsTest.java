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

package org.projectforge.calendar;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public class ICal4JUtilsTest {

  @Test
  public void recurTests() {
    final TimeZone timeZone = DateHelper.EUROPE_BERLIN;
    final Recur recur = new Recur.Builder()
        .frequency(ICal4JUtils.getCal4JFrequency(RecurrenceFrequency.WEEKLY))
        .until(getDate("2013-01-31", timeZone))
        .interval(2).build();
    final DateList dateList = recur.getDates(getDate("2013-01-01", timeZone), getDate("2012-01-02", timeZone),
        getDate("2013-03-31", timeZone), Value.TIME);
    Assertions.assertEquals(3, dateList.size());
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    df.setTimeZone(timeZone);
    Assertions.assertEquals("2013-01-01 00:00", df.format(dateList.get(0)));
    Assertions.assertEquals("2013-01-15 00:00", df.format(dateList.get(1)));
    Assertions.assertEquals("2013-01-29 00:00", df.format(dateList.get(2)));
  }

  @Test
  public void testSqlDate() {
    final net.fortuna.ical4j.model.Date date = ICal4JUtils.getICal4jDateTime(
        DateHelper.parseIsoDate("2012-12-22", DateHelper.EUROPE_BERLIN),
        DateHelper.EUROPE_BERLIN);
    Assertions.assertEquals("20121222T000000", date.toString());
  }

  @Test
  public void parseIsoDate() {
    final java.util.Date date = ICal4JUtils.parseISODateString("2013-03-21 08:47:00");
    Assertions.assertNotNull(date);
    Assertions.assertEquals("2013-03-21 08:47:00", ICal4JUtils.asISODateTimeString(date));
    Assertions.assertNull(ICal4JUtils.parseISODateString(null));
    Assertions.assertNull(ICal4JUtils.parseISODateString(""));
    Assertions.assertNull(ICal4JUtils.asISODateTimeString(null));
  }

  private net.fortuna.ical4j.model.Date getDate(final String dateString, final TimeZone timeZone) {
    final java.util.Date date = DateHelper.parseIsoDate(dateString, timeZone);
    return ICal4JUtils.getICal4jDateTime(date, timeZone);
  }

  @Test
  public void parseISODateStringsAsICal4jDates() {
    parseISODateStringsAsICal4jDates(DateHelper.EUROPE_BERLIN);
    parseISODateStringsAsICal4jDates(DateHelper.UTC);
    parseISODateStringsAsICal4jDates(TimeZone.getTimeZone("America/Los_Angeles"));
  }

  private void parseISODateStringsAsICal4jDates(final TimeZone timeZone) {
    // date and time
    final List<net.fortuna.ical4j.model.Date> dateTimes = ICal4JUtils.parseCSVDatesAsICal4jDates(
        "2013-03-21 08:47:00,20130327T090000", true,
        ICal4JUtils.getTimeZone(timeZone));
    Assertions.assertEquals(2, dateTimes.size());
    final DateFormat dtfLocal = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    dtfLocal.setTimeZone(timeZone);
    final DateFormat dtfUtc = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    dtfUtc.setTimeZone(DateHelper.UTC);
    Assertions.assertEquals(dtfUtc.format(dateTimes.get(0)), "2013-03-21 08:47");
    Assertions.assertEquals(dtfLocal.format(dateTimes.get(1)), "2013-03-27 09:00");
  }
}
