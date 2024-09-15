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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class HistoryServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var historyService: HistoryService

    @Test
    fun testNonHistorizableProperties() {
        ensureSetup()
    }

    fun ensureSetup() {
        if (pk > 1) {
            return // Already done.
        }
        val user = getUser(TEST_USER)
        add(
            user, value = "1052256", propertyName = "assignedGroups", operationType = EntityOpType.Insert,
        )
    }

    private fun add(
        entity: BaseDO<Long>,
        value: String?,
        oldValue: String? = null,
        propertyName: String,
        operationType: EntityOpType
    ) {
        val master = HistoryServiceUtils.createMaster(entity, operationType)

        val attr1 = HistoryServiceUtils.createAttr(GroupDO::class, propertyName = "$propertyName:nv", value = value)
        val attr2 = HistoryServiceUtils.createAttr(oldPropertyClass, "$propertyName:op", value = operationType.name)
        val attr3 = HistoryServiceUtils.createAttr(GroupDO::class, "$propertyName:ov", value = oldValue)
        val attrs = mutableListOf(attr1, attr2, attr3)

        pk = historyService.save(master, attrs)!!

        Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", master.entityName)
        Assertions.assertEquals(entity.id, master.entityId)
        Assertions.assertEquals("anon", master.createdBy)
        val createdAt = master.createdAt!!.time
        Assertions.assertTrue(
            Math.abs(System.currentTimeMillis() - createdAt) < 10000,
            "createdAt should be near to now (10s)",
        )

        Assertions.assertEquals(master.id, attr1.master!!.id)
        Assertions.assertEquals("V", attr1.type)
        Assertions.assertEquals("V", attr2.type)
        if (oldValue == null) {
            Assertions.assertEquals("N", attr3.type)
        } else {
            Assertions.assertEquals("V", attr3.type)
        }


        // 0        | 42353387 | 2024-04-11 07:24:18.658 | 2         | 2024-04-11 07:24:18.658 | 2          |             0 | 1052256 | assignedGroups:nv | V    | org.projectforge.framework.persistence.user.entities.GroupDO |  42353386 |
        // 0        | 42353388 | 2024-04-11 07:24:18.658 | 2         | 2024-04-11 07:24:18.658 | 2          |             0 | Update  | assignedGroups:op | V    | de.micromata.genome.db.jpa.history.entities.PropertyOpType   |  42353386 |
        // 0        | 42353389 | 2024-04-11 07:24:18.658 | 2         | 2024-04-11 07:24:18.658 | 2          |             0 |         | assignedGroups:ov | N    | org.projectforge.framework.persistence.user.entities.GroupDO |  42353386 |
    }

    companion object {
        private var pk = 1L
        private val oldPropertyClass = "de.micromata.genome.db.jpa.history.entities.PropertyOpType"
    }
}
