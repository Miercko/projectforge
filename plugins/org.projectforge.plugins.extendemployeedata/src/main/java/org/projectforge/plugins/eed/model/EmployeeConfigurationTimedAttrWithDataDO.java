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

package org.projectforge.plugins.eed.model;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

import jakarta.persistence.*;
import java.util.List;

@Entity
@DiscriminatorValue("1")
public class EmployeeConfigurationTimedAttrWithDataDO extends EmployeeConfigurationTimedAttrDO
{
  // this constructor is necessary for JPA
  public EmployeeConfigurationTimedAttrWithDataDO()
  {
    super();
  }

  public EmployeeConfigurationTimedAttrWithDataDO(final EmployeeConfigurationTimedDO parent, final String key, final char type,
      final String value)
  {
    super(parent, key, type, value);
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = EmployeeConfigurationTimedAttrDO.class,
      orphanRemoval = true, fetch = FetchType.EAGER)
  @Override
  @OrderColumn(name = "datarow")
  public List<JpaTabAttrDataBaseDO<?, Integer>> getData()
  {
    return super.getData();
  }

}
