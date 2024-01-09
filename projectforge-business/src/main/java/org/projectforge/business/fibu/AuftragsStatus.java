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

package org.projectforge.business.fibu;

import org.projectforge.common.i18n.I18nEnum;

/**
 * ERSETZT: Angebot wurde durch ein überarbeitetes oder neues Angebot ersetzt. LOI: Es liegt eine Absichtserklärung vor (reicht nur bei
 * langjährigen Kunden, um mit den Arbeiten zu beginnen). GROB_KALKULATION: Es wird lediglich eine Schätzung oder eine Grobkalkulation dem
 * Kunden kommuniziert.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum AuftragsStatus implements I18nEnum
{
  IN_ERSTELLUNG("in_erstellung"),
  POTENZIAL("potenzial"),
  GELEGT("gelegt"),
  LOI("loi"),
  BEAUFTRAGT("beauftragt"),
  ABGESCHLOSSEN("abgeschlossen"),
  ABGELEHNT("abgelehnt"),
  ERSETZT("ersetzt"),
  ESKALATION("eskalation");

  private String key;

  /**
   * @return The key suffix will be used e. g. for i18n.
   */
  public String getKey()
  {
    return key;
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  public String getI18nKey()
  {
    return "fibu.auftrag.status." + key;
  }

  AuftragsStatus(String key)
  {
    this.key = key;
  }

  /**
   * Converts this to equivalent enum AuftragsPositionsStatus.
   * @return
   */
  public AuftragsPositionsStatus asAuftragsPositionStatus() {
    switch(this) {
      case IN_ERSTELLUNG:
        return AuftragsPositionsStatus.IN_ERSTELLUNG;
      case POTENZIAL:
        return AuftragsPositionsStatus.POTENZIAL;
      case GELEGT:
        return AuftragsPositionsStatus.GELEGT;
      case LOI:
        return AuftragsPositionsStatus.LOI;
      case BEAUFTRAGT:
        return AuftragsPositionsStatus.BEAUFTRAGT;
      case ABGESCHLOSSEN:
        return AuftragsPositionsStatus.ABGESCHLOSSEN;
      case ABGELEHNT:
        return AuftragsPositionsStatus.ABGELEHNT;
      case ERSETZT:
        return AuftragsPositionsStatus.ERSETZT;
      case ESKALATION:
        return AuftragsPositionsStatus.ESKALATION;
    }
    return null;
  }

  public boolean isIn(AuftragsStatus... status)
  {
    for (AuftragsStatus st : status) {
      if (this == st) {
        return true;
      }
    }
    return false;
  }
}
