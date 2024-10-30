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

import jakarta.persistence.Transient
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import java.io.Serializable
import java.math.BigDecimal

class RechnungPosInfo(position: AbstractRechnungsPositionDO) : Serializable {
    var id = position.id
    var deleted = position.deleted
    var menge = position.menge
    var einzelNetto = position.einzelNetto
    var vat = position.vat
    var netSum = BigDecimal.ZERO
    var grossSum = BigDecimal.ZERO
    var vatAmount = BigDecimal.ZERO
    var kostZuweisungNetSum = BigDecimal.ZERO
    var kostZuweisungGrossSum = BigDecimal.ZERO
    var kostZuweisungNetFehlbetrag = BigDecimal.ZERO
    var kostZuweisungen: List<KostZuweisungInfo>? = null
}
