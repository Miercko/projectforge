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

package org.projectforge.framework.persistence.attr.impl;

import de.micromata.genome.db.jpa.tabattr.api.*;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.components.TabPanel;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

/**
 * Interface to handle with Attrs.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
public interface GuiAttrSchemaService extends AttrSchemaService
{

  /**
   * Create an edit component for given Attribute.
   *
   * @param id     the wicket id
   * @param group  the group
   * @param desc   the desc
   * @param entity the entity
   * @return the component
   */
  ComponentWrapperPanel createWicketComponent(String id, AttrGroup group, AttrDescription desc, EntityWithAttributes entity);

  <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithTimeableAttr<PK, T> & EntityWithConfigurableAttr>
  Optional<IModel<String>> getStringAttribute(final U entity, final Date date, final String groupName, final String descName);

  /**
   * Creates TimedAttributePanels and AttributePanels depending on the AttrSchema of the given entity and adds them to the given divPanel.
   *
   * @param divPanel            The divPanel to add the (Timed-)AttributePanels.
   * @param entity              The entity with configurable (timeable) attributes.
   * @param parentPage          The parentPage to add the modal dialogs of the TimedAttributePanels.
   * @param addNewEntryFunction This function is used to add a new timeableAttrRow from within the TimedAttributePanels.
   */
  <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T> & EntityWithAttributes>
  void createAttrPanels(final DivPanel divPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage, final Function<AttrGroup, T> addNewEntryFunction);

  /**
   * Creates TimedAttributePanels and AttributePanels depending on the AttrSchema of the given entity and adds them to the given tabPanel.
   * It also creates new tabs if the i18nKeySubmenu property is set on the AttrGroup(s). If it is not set, the Panels will be added to the default tab.
   *
   * @param tabPanel            The tabPanel to add the (Timed-)AttributePanels and create new tabs.
   * @param entity              The entity with configurable (timeable) attributes.
   * @param parentPage          The parentPage to add the modal dialogs of the TimedAttributePanels.
   * @param addNewEntryFunction This function is used to add a new timeableAttrRow from within the TimedAttributePanels.
   */
  <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T> & EntityWithAttributes>
  void createAttrPanels(final TabPanel tabPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage, final Function<AttrGroup, T> addNewEntryFunction);

  /**
   * Creates TimedAttributePanels only depending on the AttrSchema of the given entity and adds them to the given divPanel.
   *
   * @param divPanel            The divPanel to add the (Timed-)AttributePanels.
   * @param entity              The entity with configurable and timeable attributes.
   * @param parentPage          The parentPage to add the modal dialogs of the TimedAttributePanels.
   * @param addNewEntryFunction This function is used to add a new timeableAttrRow from within the TimedAttributePanels.
   */
  <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T>>
  void createTimedAttrPanels(final DivPanel divPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage,
      final Function<AttrGroup, T> addNewEntryFunction);

}
