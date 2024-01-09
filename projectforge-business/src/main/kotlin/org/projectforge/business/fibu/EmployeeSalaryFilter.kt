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

package org.projectforge.business.fibu

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.time.PFDayUtils
import java.io.Serializable

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
open class EmployeeSalaryFilter
@JvmOverloads
constructor(filter: BaseSearchFilter? = null)
    : BaseSearchFilter(filter), Serializable {

    /**
     * Year of salaries to filter. null means showing all years.
     */
    var year: Int? = null

    /**
     * Month of salaries to filter. null (for month or year) means showing all months.
     * 1-January, ..., 12-December
     */
    var month: Int? = null
        set(value) {
            field = PFDayUtils.validateMonthValue(value)
        }
}
