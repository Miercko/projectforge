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

package org.projectforge.business.address

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO
import javax.persistence.*

/**
 *
 * Time pending attributes of an employee.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Table(name = "t_address_attr")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "withdata", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
open class AddressAttrDO : JpaTabAttrBaseDO<AddressDO, Int> {
    constructor() : super() {}

    constructor(parent: AddressDO, propertyName: String, type: Char, value: String) : super(parent, propertyName, type, value) {}

    constructor(parent: AddressDO) : super(parent) {}

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getPk(): Int? {
        return pk
    }

    override fun createData(data: String): JpaTabAttrDataBaseDO<*, Int> {
        return AddressAttrDataDO(this, data)

    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent", referencedColumnName = "pk")
    override fun getParent(): AddressDO {
        return super.getParent()

    }

}
