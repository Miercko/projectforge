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
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.web.common.timeattr.AttrWicketComponentFactory;
import org.projectforge.web.common.timeattr.AttributePanel;
import org.projectforge.web.common.timeattr.DropDownAttrWicketComponentFactory;
import org.projectforge.web.common.timeattr.TimedAttributePanel;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.TabPanel;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

/**
 * Interface to handle with Attrs.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
public class GuiAttrSchemaServiceImpl extends AttrSchemaServiceSpringBeanImpl implements GuiAttrSchemaService
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuiAttrSchemaServiceImpl.class);

  @Autowired
  private TimeableService timeableService;

  @Override
  public ComponentWrapperPanel createWicketComponent(final String id, final AttrGroup group, final AttrDescription desc,
      final EntityWithAttributes entity)
  {
    // TODO RK cachen der factory
    Object factoryObject = desc.getWicketComponentFactoryClass();
    AttrWicketComponentFactory factory;
    if (factoryObject instanceof AttrWicketComponentFactory) {
      factory = (AttrWicketComponentFactory) factoryObject;
    } else {
      throw new UnsupportedOperationException(
          "Attr cannot load component factory: " + desc.getPropertyName() + "; "
              + desc.getWicketComponentFactoryClass());
    }

    return factory.createComponents(id, group, desc, entity);
  }

  @Override
  public <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithTimeableAttr<PK, T> & EntityWithConfigurableAttr>
  Optional<IModel<String>> getStringAttribute(final U entity, final Date date, final String groupName, final String descName)
  {
    final T attrRowForCurrentDate = timeableService.getAttrRowValidAtDate(entity, groupName, date);
    if (attrRowForCurrentDate == null) {
      return Optional.empty();
    }

    final String attribute = attrRowForCurrentDate.getStringAttribute(descName);
    if (attribute == null) {
      return Optional.empty();
    }

    final AttrDescription attrDescription = getAttrDescription(entity, groupName, descName);
    final IModel<String> valueModel = (attrDescription.getWicketComponentFactoryClass() instanceof DropDownAttrWicketComponentFactory) ?
        new ResourceModel(attribute) : // DropDownAttrWicketComponentFactory -> it's a list of i18nkeys, so we need a ResourceModel for the translation
        new Model<>(attribute);

    return Optional.of(valueModel);
  }

  @Override
  public <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T> & EntityWithAttributes> void createAttrPanels(
      final DivPanel divPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage,
      final Function<AttrGroup, T> addNewEntryFunction)
  {
    addHtmlClass(divPanel);

    final AttrSchema attrSchema = getAttrSchema(entity.getAttrSchemaName());

    if (attrSchema == null) {
      return;
    }

    for (AttrGroup group : attrSchema.getGroups()) {
      createAttrPanel(divPanel, entity, parentPage, addNewEntryFunction, group);
    }
  }

  @Override
  public <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T>> void createTimedAttrPanels(
      final DivPanel divPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage,
      final Function<AttrGroup, T> addNewEntryFunction)
  {
    addHtmlClass(divPanel);

    final AttrSchema attrSchema = getAttrSchema(entity.getAttrSchemaName());

    if (attrSchema == null) {
      return;
    }

    for (AttrGroup group : attrSchema.getGroups()) {
      createTimedAttrPanel(divPanel, entity, parentPage, addNewEntryFunction, group);
    }
  }

  @Override
  public <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T> & EntityWithAttributes> void createAttrPanels(
      final TabPanel tabPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage,
      final Function<AttrGroup, T> addNewEntryFunction)
  {
    final AttrSchema attrSchema = getAttrSchema(entity.getAttrSchemaName());

    if (attrSchema == null) {
      return;
    }

    for (AttrGroup group : attrSchema.getGroups()) {
      final GridBuilder tabContainer = tabPanel.getOrCreateTab(group.getI18nKeySubmenu());
      final DivPanel divPanel = tabContainer.getPanel();

      if (!divPanel.hasChildren()) {
        // this panel is fresh, we have to add our css class
        addHtmlClass(divPanel);
      }
      createAttrPanel(divPanel, entity, parentPage, addNewEntryFunction, group);
    }
  }

  private void addHtmlClass(DivPanel divPanel)
  {
    divPanel.getDiv().add(AttributeModifier.append("class", "mm_columnContainer"));
  }

  private <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T> & EntityWithAttributes> void createAttrPanel(
      final DivPanel divPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage,
      final Function<AttrGroup, T> addNewEntryFunction,
      final AttrGroup group)
  {
    switch (group.getType()) {
      case PERIOD:
      case INSTANT_OF_TIME:
        divPanel.add(new TimedAttributePanel<>(divPanel.newChildId(), group, entity, parentPage, addNewEntryFunction));
        break;

      case NOT_TIMEABLE:
        divPanel.add(new AttributePanel(divPanel.newChildId(), group, entity));
        break;

      default:
        log.error("The Type " + group.getType() + " is not supported.");
        break;
    }
  }

  private <PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithConfigurableAttr & EntityWithTimeableAttr<PK, T>> void createTimedAttrPanel(
      final DivPanel divPanel, final U entity, final AbstractEditPage<?, ?, ?> parentPage,
      final Function<AttrGroup, T> addNewEntryFunction, final AttrGroup group)
  {
    switch (group.getType()) {
      case PERIOD:
      case INSTANT_OF_TIME:
        divPanel.add(new TimedAttributePanel<>(divPanel.newChildId(), group, entity, parentPage, addNewEntryFunction));
        break;

      default:
        log.error("The Type " + group.getType() + " is not supported.");
        break;
    }
  }
}
