/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.model

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.*

/**
 * You may add manual correction entries to the leave account for an employee, e. g. for special leave days or for adding
 * or substracting leave days from previous employer.
 *
 * @author Kai Reinhard
 */
@Entity
@Indexed
@Table(name = "t_employee_leave_account_entry",
        indexes = [javax.persistence.Index(name = "idx_fk_t_leave_account_employee_id", columnList = "employee_id"),
            javax.persistence.Index(name = "idx_fk_t_leave_account_tenant_id", columnList = "tenant_id")])
open class LeaveAccountEntryDO : DefaultBaseDO() {

    /**
     * The employee.
     */
    @PropertyInfo(i18nKey = "vacation.employee")
    @IndexedEmbedded(includePaths = ["user.firstname", "user.lastname"])
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "employee_id", nullable = false)
    open var employee: EmployeeDO? = null

    @PropertyInfo(i18nKey = "vacation.startdate")
    @get:Column(name = "date", nullable = false)
    open var date: LocalDate? = null

    /**
     * For setting an accounting balance for a date, meaning, that the employee has the given amount of leave days
     * from this day for this year, independent of any remaining leave or previous vacation entries.
     * Any calculation starts from this date as a reset.
     * You may overwrite any remainingLeave manually.
     */
    @PropertyInfo(i18nKey = "vacation.accountingBalance")
    @get:Column(name = "accounting_balance", nullable = false)
    open var accountingBalance: Boolean? = null


    @PropertyInfo(i18nKey = "vacation.Days")
    @get:Column(nullable = true)
    open var amount: BigDecimal? = null

    @PropertyInfo(i18nKey = "description")
    @get:Column(nullable = true)
    open var description: String? = null
}
