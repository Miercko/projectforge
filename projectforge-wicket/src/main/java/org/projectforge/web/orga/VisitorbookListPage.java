/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.orga;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.orga.VisitorbookDO;
import org.projectforge.business.orga.VisitorbookService;
import org.projectforge.business.orga.VisitorbookTimedDO;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.MyListPageSortableDataProvider;
import org.projectforge.web.wicket.flowlayout.TextPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

/**
 * Created by blumenstein on 17.11.16.
 */
@ListPage(editPage = VisitorbookEditPage.class)
public class VisitorbookListPage extends AbstractListPage<VisitorbookListForm, VisitorbookService, VisitorbookDO> implements
    IListPageColumnsCreator<VisitorbookDO>
{
  private static final Logger log = LoggerFactory.getLogger(VisitorbookListPage.class);

  private static final long serialVersionUID = -8406451234003792763L;

  @SpringBean
  private VisitorbookService visitorbookService;

  @SpringBean
  private GuiAttrSchemaService guiAttrSchemaService;

  @SpringBean
  private TimeableService timeableService;

  public VisitorbookListPage(final PageParameters parameters)
  {
    super(parameters, "orga.visitorbook");
  }

  public VisitorbookListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "orga.visitorbook");
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<VisitorbookDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<VisitorbookDO, String>> columns = new ArrayList<>();

    final CellItemListener<VisitorbookDO> cellItemListener = new CellItemListener<VisitorbookDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<VisitorbookDO>> item, final String componentId,
          final IModel<VisitorbookDO> rowModel)
      {
        final VisitorbookDO visitorbook = rowModel.getObject();
        appendCssClasses(item, visitorbook.getId(), visitorbook.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.number"),
        getSortable("id", sortable),
        "id", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VisitorbookDO>> item, final String componentId,
          final IModel<VisitorbookDO> rowModel)
      {
        final VisitorbookDO visitor = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, VisitorbookEditPage.class, visitor.getId(),
              returnToPage, visitor.getPk().toString()));
        } else {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, visitor.getId(), visitor.getPk().toString()));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.lastname"),
        getSortable("lastname", sortable),
        "lastname", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.firstname"),
        getSortable("firstname", sortable),
        "firstname", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.company"),
        getSortable("company", sortable),
        "company", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.visitortype"),
        getSortable("visitortype", sortable),
        "visitortype", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.arrive"),
        getSortable("arrive", true), "arrive", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VisitorbookDO>> item, final String componentId,
          final IModel<VisitorbookDO> rowModel)
      {
        final VisitorbookDO visitor = rowModel.getObject();
        String value = "";
        List<VisitorbookTimedDO> timeableAttributes = timeableService.getTimeableAttrRowsForGroupName(visitor, "timeofvisit");
        if (timeableAttributes != null && timeableAttributes.size() > 0) {
          List<VisitorbookTimedDO> sortedList = timeableService.sortTimeableAttrRowsByDateDescending(timeableAttributes);
          VisitorbookTimedDO newestEntry = sortedList.get(0);
          SimpleDateFormat sdfParser = new SimpleDateFormat("dd.MM.yyyy");
          String date = sdfParser.format(newestEntry.getStartTime());
          String time = newestEntry.getAttribute("arrive") != null ? newestEntry.getAttribute("arrive", String.class) : "";
          value = date + " " + time;
        }
        item.add(new TextPanel(componentId, value));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.depart"),
        getSortable("depart", true), "depart", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VisitorbookDO>> item, final String componentId, final IModel<VisitorbookDO> rowModel)
      {
        final VisitorbookDO visitor = rowModel.getObject();
        String value = "";
        List<VisitorbookTimedDO> timeableAttributes = timeableService.getTimeableAttrRowsForGroupName(visitor, "timeofvisit");
        if (timeableAttributes != null && timeableAttributes.size() > 0) {
          List<VisitorbookTimedDO> sortedList = timeableService.sortTimeableAttrRowsByDateDescending(timeableAttributes);
          VisitorbookTimedDO newestEntry = sortedList.get(0);
          SimpleDateFormat sdfParser = new SimpleDateFormat("dd.MM.yyyy");
          String date = sdfParser.format(newestEntry.getStartTime());
          String time = newestEntry.getAttribute("depart") != null ? newestEntry.getAttribute("depart", String.class) : "";
          value = date + " " + time;
        }
        item.add(new TextPanel(componentId, value));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.contactPerson"),
        getSortable("contactPerson", true), "contactPerson", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VisitorbookDO>> item, final String componentId, final IModel<VisitorbookDO> rowModel)
      {
        final VisitorbookDO visitor = rowModel.getObject();
        String value = "";
        if (visitor.getContactPersons() != null && visitor.getContactPersons().size() > 0) {
          for (EmployeeDO contact : visitor.getContactPersons()) {
            value = value + contact.getUser().getFullname() + "; ";
          }
        }
        item.add(new TextPanel(componentId, value));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });

    return columns;
  }

  @Override
  protected void init()
  {
    final List<IColumn<VisitorbookDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "id", SortOrder.DESCENDING);
    form.add(dataTable);
    //addExcelExport(getString("orga.visitorbook.title.heading"), "visitors");
  }

  @Override
  protected ISortableDataProvider<VisitorbookDO, String> createSortableDataProvider(final SortParam<String> sortParam,
      final SortParam<String> secondSortParam)
  {
    if (listPageSortableDataProvider == null) {
      listPageSortableDataProvider = new MyListPageSortableDataProvider<VisitorbookDO>(sortParam, secondSortParam, this)
      {
        @Override
        protected Comparator<VisitorbookDO> getComparator(final SortParam<String> sortParam, final SortParam<String> secondSortParam)
        {
          final String sortProperty = sortParam != null ? sortParam.getProperty() : null;
          if ("arrive".equals(sortProperty) || "depart".equals(sortProperty)) {
            VisitorbookTimeComparator comparator = new VisitorbookTimeComparator();

            comparator.property = sortProperty;
            comparator.dsc = sortParam != null ? (sortParam.isAscending() == false) : false;

            return comparator;
          }

          return super.getComparator(sortParam, secondSortParam);
        }
      };
    }
    return listPageSortableDataProvider;
  }

  @Override
  protected VisitorbookListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new VisitorbookListForm(this);
  }

  @Override
  public VisitorbookService getBaseDao()
  {
    return visitorbookService;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if (property.startsWith("quickSelect.") == true) { // month".equals(property) == true) {
      final Date date = (Date) selectedValue;
      form.getSearchFilter().setStartTime(date);
      final DateHolder dateHolder = new DateHolder(date);
      if (property.endsWith(".month") == true) {
        dateHolder.setEndOfMonth();
      } else if (property.endsWith(".week") == true) {
        dateHolder.setEndOfWeek();
      } else {
        log.error("Property '" + property + "' not supported for selection.");
      }
      form.getSearchFilter().setStopTime(dateHolder.getDate());
      form.startDate.markModelAsChanged();
      form.stopDate.markModelAsChanged();
      refresh();
    } else {
      super.select(property, selectedValue);
    }
  }

  private class VisitorbookTimeComparator implements Comparator<VisitorbookDO>
  {
    private String property;
    private boolean dsc;

    @Override
    public int compare(final VisitorbookDO o1, final VisitorbookDO o2)
    {
      final VisitorbookTimedDO value1 = getNewestEntry(o1);
      final VisitorbookTimedDO value2 = getNewestEntry(o2);

      if (value1 == null) {
        if (value2 == null)
          return 0;
        else
          return (dsc) ? -1 : 1;
      }
      if (value2 == null) {
        return (dsc) ? 1 : -1;
      }

      if (value1.getStartTime().before(value2.getStartTime())) {
        return (dsc) ? -1 : 1;
      } else if (value2.getStartTime().before(value1.getStartTime())) {
        return (dsc) ? 1 : -1;
      }

      Integer time1 = getTimeOf(value1);
      Integer time2 = getTimeOf(value2);

      if (time1 == null) {
        if (time2 == null)
          return 0;
        else
          return (dsc) ? -1 : 1;
      }
      if (time2 == null) {
        return (dsc) ? 1 : -1;
      }

      if (time1 < time2) {
        return (dsc) ? -1 : 1;
      } else {
        return (dsc) ? 1 : -1;
      }
    }

    private VisitorbookTimedDO getNewestEntry(final VisitorbookDO visitor)
    {
      List<VisitorbookTimedDO> timeableAttributes = timeableService.getTimeableAttrRowsForGroupName(visitor, "timeofvisit");
      if (timeableAttributes != null && timeableAttributes.size() > 0) {
        List<VisitorbookTimedDO> sortedList = timeableService.sortTimeableAttrRowsByDateDescending(timeableAttributes);
        return sortedList.get(0);
      }

      return null;
    }

    private Integer getTimeOf(final VisitorbookTimedDO timedDO)
    {
      String time1 = timedDO.getAttribute(property) != null ? timedDO.getAttribute(property, String.class) : "";

      if (time1 == null) {
        return null;
      }

      time1 = time1.replace(":", "").trim();

      try {
        return Integer.valueOf(time1);
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }

}

