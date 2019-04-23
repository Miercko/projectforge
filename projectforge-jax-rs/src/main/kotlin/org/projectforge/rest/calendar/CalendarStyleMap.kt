/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter

/**
 * Persist the styles of the calendarIds for the user.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class CalendarStyleMap {
    /**
     * Colors for the calendarIds by calendar id.
     */
    private val styles = mutableMapOf<Int, CalendarStyle>()

    fun add(calendarId : Int, style : CalendarStyle) {
        styles.put(calendarId, style)
    }

    fun get(calendarId: Int?) : CalendarStyle? {
        return styles[calendarId]
    }

    // LEGACY STUFF:
    companion object {
        /**
         * For re-using legacy filters (from ProjetForge version up to 6, Wicket-Calendar).
         */
        internal fun copyFrom(oldFilter: TeamCalCalendarFilter?): CalendarStyleMap {
            val styleMap = CalendarStyleMap()
            if (oldFilter != null) {
                oldFilter.templateEntries?.forEach { templateEntry ->
                    templateEntry.calendarProperties?.forEach {
                        if (!styleMap.styles.containsKey(it.calId)) {
                            styleMap.styles.put(it.calId, CalendarStyle(bgColor = it.colorCode)) // Only bgColor was stored for ProjectForge earlier than 7.0.
                        }
                    }
                }
            }
            return styleMap
        }
    }
}