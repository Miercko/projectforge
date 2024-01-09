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

package org.projectforge.business.address;

import org.hibernate.search.bridge.StringBridge;
import org.projectforge.framework.utils.LabelValueBean;

import java.util.List;


/**
 * StringBridge for hibernate search to search in instant messaging values.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HibernateSearchInstantMessagingBridge implements StringBridge
{
  @SuppressWarnings("unchecked")
  public String objectToString(Object object)
  {
    if (object == null || !(object instanceof List<?>)) {
      return "";
    }
    List<LabelValueBean<InstantMessagingType, String>> list = (List<LabelValueBean<InstantMessagingType, String>>)object;
    return AddressDO.Companion.getInstantMessagingAsString(list);
  }
}
