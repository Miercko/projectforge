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

import mu.KotlinLogging
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

/**
 */
@Service
class HistoryService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Loads all history entries for the given baseDO by class and id.
     */
    fun loadHistory(baseDO: BaseDO<Long>): List<PfHistoryMasterDO> {
        val result = persistenceService.namedQuery(
            PfHistoryMasterDO.SELECT_HISTORY_FOR_BASEDO,
            PfHistoryMasterDO::class.java,
            Pair("entityId", baseDO.id),
            Pair("entityName", baseDO::class.java.name),
        )
        return result
    }

    /**
     * Save method will be called automatically by the Dao services.
     */
    fun save(master: PfHistoryMasterDO, attrs: Collection<PfHistoryAttrDO>? = null): Long? {
        persistenceService.runInTransaction { context ->
            val em = context.em
            master.modifiedBy = ThreadLocalUserContext.user?.id?.toString() ?: "anon"
            master.modifiedAt = Date()
            em.persist(master)
            log.info { "Saving history: $master" }
            attrs?.forEach { attr ->
                attr.master = master
                em.persist(attr)
            }
        }
        return master.id
    }
}
