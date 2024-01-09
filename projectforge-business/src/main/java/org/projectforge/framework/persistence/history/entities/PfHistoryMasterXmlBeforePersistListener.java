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

package org.projectforge.framework.persistence.history.entities;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.jpa.StdRecord;
import de.micromata.genome.jpa.metainf.EntityMetadata;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Restore History entries with new Pks.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PfHistoryMasterXmlBeforePersistListener implements JpaXmlBeforePersistListener
{
  private static final Logger LOG = LoggerFactory.getLogger(PfHistoryMasterXmlBeforePersistListener.class);

  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx)
  {
    PfHistoryMasterDO pfm = (PfHistoryMasterDO) entity;
    setNewUser(pfm, ctx);
    setRefEntityPk(pfm, ctx);
    setNewCollectionRefPks(pfm, ctx);
    return null;
  }

  private void setRefEntityPk(PfHistoryMasterDO pfm, XmlDumpRestoreContext ctx)
  {
    String entn = pfm.getEntityName();
    if (StringUtils.isBlank(entn)) {
      LOG.warn("History entry has no entityName");
      return;
    }
    Long id = pfm.getEntityId();
    EntityMetadata entityMeta = findEntityMetaData(entn, ctx);
    if (entityMeta == null) {
      LOG.warn("EntityName is not known entity: " + entn);
      return;
    }
    Integer intid = (int) (long) id;
    Object oldp = ctx.findEntityByOldPk(intid, entityMeta.getJavaType());
    if (oldp == null) {
      LOG.info("Cannot find oldpk from entity: " + entn + ": " + intid);
      return;
    }

    Object newPk = entityMeta.getIdColumn().getGetter().get(oldp);
    Number newPkN = (Number) newPk;
    pfm.setEntityId(newPkN.longValue());
  }

  private EntityMetadata findEntityMetaData(String className, XmlDumpRestoreContext ctx)
  {
    try {
      Class<?> cls = Class.forName(className);
      return ctx.findEntityMetaData(cls);
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  private void setNewUser(PfHistoryMasterDO pfm, XmlDumpRestoreContext ctx)
  {

    String smodby = pfm.getModifiedBy();
    if (!NumberUtils.isDigits(smodby)) {
      return;
    }
    Integer olduserpk = Integer.parseInt(smodby);
    PFUserDO user = ctx.findEntityByOldPk(olduserpk, PFUserDO.class);
    if (user == null) {
      LOG.warn("Cannot find user with old pk: " + smodby);
      return;
    }
    Integer pk = user.getId();
    if (pk == null) {
      LOG.warn("User id is null: " + user.getUserDisplayName());
      return;
    }
    String spk = Integer.toString(pk);
    pfm.visit((rec) -> {
      StdRecord<?> srec = (StdRecord<?>) rec;
      srec.setModifiedBy(spk);
      srec.setCreatedBy(spk);
    });
  }

  private void setNewCollectionRefPks(PfHistoryMasterDO pfm, XmlDumpRestoreContext ctx)
  {
    for (String key : pfm.getAttributes().keySet()) {
      if (!key.endsWith(":ov") && !key.endsWith(":nv")) {
        continue;
      }
      PfHistoryAttrDO row = (PfHistoryAttrDO) pfm.getAttributeRow(key);
      translatePkRefs(row, ctx);
    }

  }

  private void translatePkRefs(PfHistoryAttrDO row, XmlDumpRestoreContext ctx)
  {
    String typeClassName = row.getPropertyTypeClass();
    EntityMetadata entitymeta = findEntityMetaData(typeClassName, ctx);
    if (entitymeta == null) {
      return;
    }
    List<Integer> ilist = parseIntList(row.getStringData());
    if (ilist == null) {
      return;
    }
    List<String> nlist = new ArrayList<>();
    for (Integer opk : ilist) {
      Object oldEnt = ctx.findEntityByOldPk(opk, entitymeta.getJavaType());
      if (oldEnt == null) {
        return;
      }
      Object newPk = entitymeta.getIdColumn().getGetter().get(oldEnt);
      if (newPk == null) {
        return;
      }
      nlist.add(newPk.toString());
    }
    String nval = StringUtils.join(nlist, ',');
    row.setStringData(nval);
  }

  private List<Integer> parseIntList(String value)
  {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    String[] values = StringUtils.split(value, ',');
    if (values == null || values.length == 0) {
      return null;
    }
    List<Integer> ret = new ArrayList<>();
    for (String sv : values) {
      try {
        int ival = Integer.parseInt(sv);
        ret.add(ival);
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return ret;
  }
}
