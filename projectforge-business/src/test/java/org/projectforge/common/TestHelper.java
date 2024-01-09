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

package org.projectforge.common;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.Locale;

public class TestHelper
{
  public static void setContextUser(final Locale locale)
  {
    final PFUserDO user = new PFUserDO();
    user.setLocale(locale);
    ThreadLocalUserContext.setUser(user);
  }

  public static void setDeclaredField(final Object obj, final String fieldName, final Object value)
  {
    Field field;
    try {
      field = obj.getClass().getDeclaredField(fieldName);
    } catch (SecurityException | NoSuchFieldException ex) {
      throw new RuntimeException(ex);
    }
    final Field[] fields = new Field[] { field };
    AccessibleObject.setAccessible(fields, true);
    try {
      field.set(obj, value);
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void setDeclaredStaticField(final Class<?> clazz, final String fieldName, final Object value)
  {
    Field field;
    try {
      field = clazz.getDeclaredField(fieldName);
    } catch (SecurityException | NoSuchFieldException ex) {
      throw new RuntimeException(ex);
    }
    final Field[] fields = new Field[] { field };
    AccessibleObject.setAccessible(fields, true);
    try {
      field.set(null, value);
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Object getDeclaredFieldValue(final Object obj, final String fieldName)
  {
    Field field;
    try {
      field = obj.getClass().getDeclaredField(fieldName);
    } catch (SecurityException | NoSuchFieldException ex) {
      throw new RuntimeException(ex);
    }
    final Field[] fields = new Field[] { field };
    AccessibleObject.setAccessible(fields, true);
    try {
      return field.get(obj);
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }
}
