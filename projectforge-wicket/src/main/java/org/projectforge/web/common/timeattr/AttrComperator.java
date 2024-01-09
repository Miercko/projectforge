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

package org.projectforge.web.common.timeattr;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import org.apache.wicket.model.IModel;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

/**
 * Compares an attribute determined by a given group name and description name.
 * The group and description names must be given by the sortProperty in the following format: "attr:group name:description name"
 */
public class AttrComperator<PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithTimeableAttr<PK, T> & EntityWithConfigurableAttr>
    implements Comparator<U>
{
  private final GuiAttrSchemaService guiAttrSchemaService = (GuiAttrSchemaService) ApplicationContextProvider.getApplicationContext()
      .getBean("guiAttrSchemaService");

  private final String groupName;

  private final String descName;

  private final boolean ascending;

  public AttrComperator(final String sortProperty, final boolean ascending)
  {
    final String[] strings = sortProperty.split(":");
    this.groupName = strings[1];
    this.descName = strings[2];
    this.ascending = ascending;
  }

  @Override
  public int compare(U o1, U o2)
  {
    final Date now = new Date();
    final Optional<IModel<String>> stringValue1 = guiAttrSchemaService.getStringAttribute(o1, now, groupName, descName);
    final Optional<IModel<String>> stringValue2 = guiAttrSchemaService.getStringAttribute(o2, now, groupName, descName);

    if (stringValue1.isPresent() && stringValue2.isPresent()) {
      final String sv1 = stringValue1.get().getObject();
      final String sv2 = stringValue2.get().getObject();
      return ascending ? sv1.compareTo(sv2) : sv2.compareTo(sv1);
    }

    if (stringValue1.isPresent()) {
      return ascending ? 1 : -1;
    }

    if (stringValue2.isPresent()) {
      return ascending ? -1 : 1;
    }

    return 0;
  }
}
