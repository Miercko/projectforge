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

package org.projectforge.business.fibu.kost;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.test.AbstractTestBase;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KostZuweisungenCopyHelperTest extends AbstractTestBase
{
  @Test
  public void copy()
  {
    final RechnungsPositionDO srcPos = new RechnungsPositionDO();
    final RechnungsPositionDO destPos = new RechnungsPositionDO();

    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(0, destPos.getKostZuweisungen().size());

    KostZuweisungDO kostZuweisung = new KostZuweisungDO();
    kostZuweisung.setNetto(BigDecimal.ONE);
    kostZuweisung.setComment("1");
    kostZuweisung.setId(4711); // simulate non deletable
    srcPos.addKostZuweisung(kostZuweisung);
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(1, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));

    kostZuweisung = new KostZuweisungDO();
    kostZuweisung.setNetto(BigDecimal.ONE);
    kostZuweisung.setComment("1");

    destPos.addKostZuweisung(kostZuweisung);
    assertEquals(2, destPos.getKostZuweisungen().size());

    // srcPos "overwrites" dstPos
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(1, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));

    srcPos.getKostZuweisung(0).setNetto(BigDecimal.TEN);
    srcPos.getKostZuweisung(0).setComment("10");
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(1, destPos.getKostZuweisungen().size());
    assertEquals(BigDecimal.TEN, destPos.getKostZuweisung(0).getNetto());
    assertEquals("10", destPos.getKostZuweisung(0).getComment());

    kostZuweisung = new KostZuweisungDO();
    kostZuweisung.setNetto(BigDecimal.ONE);
    kostZuweisung.setComment("2");

    srcPos.addKostZuweisung(kostZuweisung);

    kostZuweisung = new KostZuweisungDO();
    kostZuweisung.setNetto(BigDecimal.ONE);
    kostZuweisung.setComment("3");

    srcPos.addKostZuweisung(kostZuweisung);

    kostZuweisung = new KostZuweisungDO();
    kostZuweisung.setNetto(BigDecimal.ONE);
    kostZuweisung.setComment("4");

    srcPos.addKostZuweisung(kostZuweisung);

    kostZuweisung = new KostZuweisungDO();
    kostZuweisung.setNetto(BigDecimal.ONE);
    kostZuweisung.setComment("5");

    srcPos.addKostZuweisung(kostZuweisung);
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(5, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));
    assertEquals(srcPos.getKostZuweisungen().get(1), destPos.getKostZuweisungen().get(1));
    assertEquals(srcPos.getKostZuweisungen().get(2), destPos.getKostZuweisungen().get(2));
    assertEquals(srcPos.getKostZuweisungen().get(3), destPos.getKostZuweisungen().get(3));
    assertEquals(srcPos.getKostZuweisungen().get(4), destPos.getKostZuweisungen().get(4));

    srcPos.deleteKostZuweisung(3);
    srcPos.deleteKostZuweisung(2);
    srcPos.deleteKostZuweisung(1);
    srcPos.deleteKostZuweisung(0); // is not deletable, see above
    KostZuweisungenCopyHelper.copy(srcPos.getKostZuweisungen(), destPos);
    assertEquals(2, destPos.getKostZuweisungen().size());
    assertEquals(srcPos.getKostZuweisungen().get(0), destPos.getKostZuweisungen().get(0));
    assertEquals(srcPos.getKostZuweisungen().get(1), destPos.getKostZuweisungen().get(1));
    assertEquals(srcPos.getKostZuweisungen().get(0).getNetto(), destPos.getKostZuweisungen().get(0).getNetto());
    assertEquals(srcPos.getKostZuweisungen().get(1).getNetto(), destPos.getKostZuweisungen().get(1).getNetto());
    assertEquals(4, srcPos.getKostZuweisungen().get(1).getIndex());
  }
}
