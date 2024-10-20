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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Employee
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.Kost1
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired

@RestController
@RequestMapping("${Rest.URL}/employee")
class EmployeePagesRest : AbstractDTOPagesRest<EmployeeDO, Employee, EmployeeDao>(EmployeeDao::class.java, "fibu.employee.title") {
    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override fun transformFromDB(obj: EmployeeDO, editMode: Boolean): Employee {
        val employee = Employee()
        employee.copyFrom(obj)
        userGroupCache.getUser(obj.userId)?.let { user ->
            employee.user = User(user)
        }
        kostCache.getKost1(obj.kost1Id)?.let { kost ->
            employee.kost1 = Kost1(kost)
        }
        return employee
    }
/*
    override fun postProcessResultSet(
        resultSet: ResultSet<EmployeeDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter
    ): ResultSet<*> {
        return super.postProcessResultSet(resultSet, request, magicFilter)
    }*/

    override fun transformForDB(dto: Employee): EmployeeDO {
        val employeeDO = EmployeeDO()
        dto.copyTo(employeeDO)
        return employeeDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(UITableColumn("user", "name"))
                        .add(lc, "status", "staffNumber", "kost1")
                        .add(lc, "position", "abteilung", "eintrittsDatum", "austrittsDatum", "comment"))
        layout.getTableColumnById("eintrittsDatum").formatter = UITableColumn.Formatter.DATE
        layout.getTableColumnById("austrittsDatum").formatter = UITableColumn.Formatter.DATE
        layout.getTableColumnById("user").formatter = UITableColumn.Formatter.USER
        layout.getTableColumnById("kost1").formatter = UITableColumn.Formatter.COST1
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Employee, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "user", "kost1", "abteilung", "position"))
                        .add(UICol()
                                .add(lc, "staffNumber", "weeklyWorkingHours", "urlaubstage", "previousyearleave",
                                        "previousyearleaveused", "eintrittsDatum", "austrittsDatum")))
                .add(UIRow()
                        .add(UICol().add(lc, "street", "zipCode", "city"))
                        .add(UICol().add(lc, "country", "state"))
                        .add(UICol().add(lc, "birthday", "gender"))
                        .add(UICol().add(lc, "accountHolder", "iban", "bic")))
                .add(UIRow()
                        .add(UICol().add(lc, "status")))
                .add(UILabel("TODO: Custom properties here"))
                .add(UIRow()
                        .add(UICol().add(lc, "comment")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override val autoCompleteSearchFields = arrayOf("user.username", "user.firstname", "user.lastname", "user.email")

    override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): MutableList<EmployeeDO> {
        return baseDao.internalGetEmployeeList(filter, showOnlyActiveEntries = true).toMutableList()
    }
}
