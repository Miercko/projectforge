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

package org.projectforge.plugins.marketing;

import org.projectforge.business.address.AddressDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Your plugin initialization. Register all your components such as i18n files, data-access object etc.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MarketingPlugin extends AbstractPlugin {
  public static final String ADDRESS_CAMPAIGN_ID = "addressCampaign";

  public static final String ADDRESS_CAMPAIGN_VALUE_ID = "addressCampaignValues";

  public static final String RESOURCE_BUNDLE_NAME = "MarketingI18nResources";

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[]{AddressCampaignDO.class,
          AddressCampaignValueDO.class};

  @Autowired
  AddressDao addressDao;

  @Autowired
  private AddressCampaignDao addressCampaignDao;

  @Autowired
  private AddressCampaignValueDao addressCampaignValueDao;

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  @Autowired
  private PfEmgrFactory emgrFactory;

  public MarketingPlugin() {
    super("marketing", "Marketing", "Marketing plugin for address campaigns.");
  }

  @Override
  protected void initialize() {
    // Register it:
    register(ADDRESS_CAMPAIGN_ID, AddressCampaignDao.class, addressCampaignDao, "plugins.marketing.addressCampaign");
    register(ADDRESS_CAMPAIGN_VALUE_ID, AddressCampaignValueDao.class, addressCampaignValueDao,
            "plugins.marketing.addressCampaignValue")
            .setSearchable(false);

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ADDRESS_CAMPAIGN_ID, AddressCampaignListPage.class,
            AddressCampaignEditPage.class);
    pluginWicketRegistrationService.registerWeb(ADDRESS_CAMPAIGN_VALUE_ID, AddressCampaignValueListPage.class,
            AddressCampaignValueEditPage.class);

    // Register the menu entry as sub menu entry of the misc menu:
    pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.MISC, MenuItemDef.create(ADDRESS_CAMPAIGN_ID, "plugins.marketing.addressCampaign.menu"), AddressCampaignListPage.class);
    pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.MISC, MenuItemDef.create(ADDRESS_CAMPAIGN_VALUE_ID, "plugins.marketing.addressCampaignValue.menu"), AddressCampaignValueListPage.class);

    // Define the access management:
    registerRight(new AddressCampaignRight(accessChecker));
    registerRight(new AddressCampaignValueRight(accessChecker, addressDao));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);

    addressDao.register(new MarketingPluginAddressDeletionListener(addressCampaignDao));
  }
}
