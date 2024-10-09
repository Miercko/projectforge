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

package org.projectforge.framework.persistence.history

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object HistoryOldTypeClassMapping {
    internal fun getMappedClass(className: String): String {
        return oldTypeClassMapping[className] ?: className
    }

    // Automatically generated by HistoryOldTypeClassMappingTest.main()
    private val oldTypeClassMapping = mapOf(
        "de.micromata.fibu.AuftragsArt" to "org.projectforge.business.fibu.AuftragsArt",
        "de.micromata.fibu.AuftragsPositionsArt" to "org.projectforge.business.fibu.AuftragsPositionsArt",
        "de.micromata.fibu.AuftragsPositionsStatus" to "org.projectforge.business.fibu.AuftragsPositionsStatus",
        "de.micromata.fibu.AuftragsStatus" to "org.projectforge.business.fibu.AuftragsStatus",
        "de.micromata.fibu.EmployeeStatus" to "org.projectforge.business.fibu.EmployeeStatus",
        "de.micromata.fibu.KundeStatus" to "org.projectforge.business.fibu.KundeStatus",
        "de.micromata.fibu.ProjektStatus" to "org.projectforge.business.fibu.ProjektStatus",
        "de.micromata.fibu.RechnungStatus" to "org.projectforge.business.fibu.RechnungStatus",
        "de.micromata.fibu.RechnungTyp" to "org.projectforge.business.fibu.RechnungTyp",
        "de.micromata.fibu.kost.KostentraegerStatus" to "org.projectforge.business.fibu.kost.KostentraegerStatus",
        "de.micromata.genome.db.jpa.history.entities.PropertyOpType" to "org.projectforge.framework.persistence.history.PropertyOpType",
        "de.micromata.projectforge.address.AddressStatus" to "org.projectforge.business.address.AddressStatus",
        "de.micromata.projectforge.address.ContactStatus" to "org.projectforge.business.address.ContactStatus",
        "de.micromata.projectforge.address.FormOfAddress" to "org.projectforge.business.address.FormOfAddress",
        "de.micromata.projectforge.book.BookStatus" to "org.projectforge.business.book.BookStatus",
        "de.micromata.projectforge.core.Priority" to "org.projectforge.common.i18n.Priority",
        "de.micromata.projectforge.humanresources.HRPlanningEntryStatus" to "org.projectforge.business.humanresources.HRPlanningEntryStatus",
        "de.micromata.projectforge.orga.PostType" to "org.projectforge.business.orga.PostType",
        "de.micromata.projectforge.scripting.ScriptParameterType" to "org.projectforge.business.scripting.ScriptParameterType",
        "de.micromata.projectforge.task.TaskDO" to "org.projectforge.business.task.TaskDO",
        "de.micromata.projectforge.task.TaskStatus" to "org.projectforge.common.task.TaskStatus",
        "de.micromata.projectforge.task.TimesheetBookingStatus" to "org.projectforge.common.task.TimesheetBookingStatus",
        "de.micromata.projectforge.user.PFUserDO" to "org.projectforge.framework.persistence.user.entities.PFUserDO",
        "org.projectforge.address.AddressStatus" to "org.projectforge.business.address.AddressStatus",
        "org.projectforge.address.ContactStatus" to "org.projectforge.business.address.ContactStatus",
        "org.projectforge.address.FormOfAddress" to "org.projectforge.business.address.FormOfAddress",
        "org.projectforge.book.BookStatus" to "org.projectforge.business.book.BookStatus",
        "org.projectforge.book.BookType" to "org.projectforge.business.book.BookType",
        "org.projectforge.business.fibu.Gender" to "org.projectforge.framework.persistence.user.entities.Gender",
        "org.projectforge.common.TimeNotation" to "org.projectforge.framework.time.TimeNotation",
        "org.projectforge.core.Priority" to "org.projectforge.common.i18n.Priority",
        "org.projectforge.fibu.AuftragsPositionsArt" to "org.projectforge.business.fibu.AuftragsPositionsArt",
        "org.projectforge.fibu.AuftragsPositionsStatus" to "org.projectforge.business.fibu.AuftragsPositionsStatus",
        "org.projectforge.fibu.AuftragsStatus" to "org.projectforge.business.fibu.AuftragsStatus",
        "org.projectforge.fibu.EmployeeStatus" to "org.projectforge.business.fibu.EmployeeStatus",
        "org.projectforge.fibu.KontoDO" to "org.projectforge.business.fibu.KontoDO",
        "org.projectforge.fibu.KontoStatus" to "org.projectforge.business.fibu.KontoStatus",
        "org.projectforge.fibu.KundeStatus" to "org.projectforge.business.fibu.KundeStatus",
        "org.projectforge.fibu.ModeOfPaymentType" to "org.projectforge.business.fibu.ModeOfPaymentType",
        "org.projectforge.fibu.PaymentType" to "org.projectforge.business.fibu.PaymentType",
        "org.projectforge.fibu.PeriodOfPerformanceType" to "org.projectforge.business.fibu.PeriodOfPerformanceType",
        "org.projectforge.fibu.ProjektStatus" to "org.projectforge.business.fibu.ProjektStatus",
        "org.projectforge.fibu.RechnungStatus" to "org.projectforge.business.fibu.RechnungStatus",
        "org.projectforge.fibu.RechnungTyp" to "org.projectforge.business.fibu.RechnungTyp",
        "org.projectforge.fibu.kost.KostentraegerStatus" to "org.projectforge.business.fibu.kost.KostentraegerStatus",
        "org.projectforge.fibu.kost.SHType" to "org.projectforge.business.fibu.kost.SHType",
        "org.projectforge.gantt.GanttObjectType" to "org.projectforge.business.gantt.GanttObjectType",
        "org.projectforge.gantt.GanttRelationType" to "org.projectforge.business.gantt.GanttRelationType",
        "org.projectforge.humanresources.HRPlanningEntryStatus" to "org.projectforge.business.humanresources.HRPlanningEntryStatus",
        "org.projectforge.orga.PostType" to "org.projectforge.business.orga.PostType",
        "org.projectforge.plugins.teamcal.event.ReminderActionType" to "org.projectforge.business.teamcal.event.model.ReminderActionType",
        "org.projectforge.plugins.teamcal.event.ReminderDurationUnit" to "org.projectforge.business.teamcal.event.model.ReminderDurationUnit",
        "org.projectforge.scripting.ScriptParameterType" to "org.projectforge.business.scripting.ScriptParameterType",
        "org.projectforge.task.TaskDO" to "org.projectforge.business.task.TaskDO",
        "org.projectforge.task.TaskStatus" to "org.projectforge.common.task.TaskStatus",
        "org.projectforge.task.TimesheetBookingStatus" to "org.projectforge.common.task.TimesheetBookingStatus",
        "org.projectforge.user.PFUserDO" to "org.projectforge.framework.persistence.user.entities.PFUserDO",
        "org.projectforge.user.UserRightValue" to "org.projectforge.business.user.UserRightValue",
    )

    // Automatically generated by HistoryOldTypeClassMappingTest.main()
    internal val removedClasses = arrayOf(
        "org.projectforge.business.vacation.model.VacationCalendarDO",
        "org.projectforge.framework.persistence.user.entities.TenantDO",
        "org.projectforge.gantt.GanttDependencyType",
        "org.projectforge.plugins.eed.model.EmployeeConfigurationDO",
        "org.projectforge.plugins.ffp.model.FFPAccountingDO",
        "org.projectforge.plugins.ffp.model.FFPDebtDO",
        "org.projectforge.plugins.ffp.model.FFPEventDO",
        "org.projectforge.plugins.skillmatrix.SkillDO",
        "org.projectforge.plugins.skillmatrix.SkillRating",
        "org.projectforge.plugins.skillmatrix.SkillRatingDO",
        "org.projectforge.plugins.skillmatrix.TrainingAttendeeDO",
        "org.projectforge.plugins.skillmatrix.TrainingDO",
    )
}
