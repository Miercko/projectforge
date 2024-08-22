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

package org.projectforge.plugins.eed.service

import org.springframework.stereotype.Service
import java.time.Year

@Service
open class EEDHelper {
    companion object {
        @JvmField
        val MONTH_INTEGERS: List<Int> = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
    }

    fun getDropDownYears(): List<Int> {
        val currentYear = Year.now().value
        return listOf(currentYear, currentYear - 1, currentYear - 2)
    }

    /*

  @Autowired
  private EmployeeDao employeeDao;

  public List<Integer> getDropDownYears()
  {
    // do not cache the years because this is a long lasting service and the years could change in the meantime
    final List<Integer> years = timeableService.getAvailableStartTimeYears(employeeDao.internalLoadAll());
    final Integer actualYear = PFDateTime.now().getYear();
    if (!years.contains(actualYear)) {
      years.add(actualYear);
    }
    if (!years.contains(actualYear + 1)) {
      years.add(actualYear + 1);
    }
    return years
        .stream()
        .sorted()
        .collect(Collectors.toList());
  }

   */
}
