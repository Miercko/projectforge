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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressbookDO
import org.projectforge.rest.dto.Addressbook
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.test.AbstractTestBase

class AddressbookTest: AbstractTestBase() {

    @Test
    fun listToStringTest() {
        var bookDO = AddressbookDO()
        bookDO.fullAccessGroupIds = "1"
        bookDO.fullAccessUserIds = "1, 2, 3"
        bookDO.readonlyAccessGroupIds = ""
        bookDO.readonlyAccessUserIds = "1, ,3"

        val book = Addressbook()
        book.copyFrom(bookDO)

        checkLongList(book.fullAccessGroups, 1)
        checkLongList(book.fullAccessUsers, 1, 2, 3)
        assertNull(book.readonlyAccessGroups)
        checkLongList(book.readonlyAccessUsers, 1, 3)

        bookDO = AddressbookDO()
        book.copyTo(bookDO)
        assertEquals("1", bookDO.fullAccessGroupIds)
        assertEquals("1, 2, 3", bookDO.fullAccessUserIds)
        assertNull( bookDO.readonlyAccessGroupIds)
        assertEquals("1, 3", bookDO.readonlyAccessUserIds)
    }

    private fun checkLongList(longList: List<BaseDTO<*>>?, vararg expected: Long) {
        assertNotNull(longList)
        if (longList == null) {
            return
        }
        assertEquals(expected.size, longList.size)
        longList.forEachIndexed { index, element ->
            assertEquals(expected[index], element.id)
        }
    }
}
