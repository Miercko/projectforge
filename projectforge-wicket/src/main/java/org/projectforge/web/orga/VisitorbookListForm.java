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

package org.projectforge.web.orga;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.business.orga.VisitorbookFilter;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LocalDateModel;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Date;

public class VisitorbookListForm extends AbstractListForm<VisitorbookFilter, VisitorbookListPage>
{
  private static final Logger log = LoggerFactory.getLogger(VisitorbookListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  protected LocalDatePanel startDate;

  protected LocalDatePanel stopDate;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent<?>[2];

  @Override
  protected void init()
  {
    super.init();
    final VisitorbookFilter filter = getSearchFilter();
    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(final Form<?> form)
      {
        final Date from = startDate.getConvertedInput();
        final Date to = stopDate.getConvertedInput();
        if (from != null && to != null && from.after(to) == true) {
          error(getString("timePeriodPanel.startTimeAfterStopTime"));
        }
      }
    });
    {
      gridBuilder.newSplitPanel(GridSize.COL66);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod"));
      startDate = new LocalDatePanel(fs.newChildId(), new LocalDateModel(new PropertyModel<LocalDate>(filter, "startDay")), DatePanelSettings.get()
          .withSelectPeriodMode(true), true);
      fs.add(dependentFormComponents[0] = startDate);
      fs.setLabelFor(startDate);
      fs.add(new DivTextPanel(fs.newChildId(), " - "));
      stopDate = new LocalDatePanel(fs.newChildId(), new LocalDateModel(new PropertyModel<LocalDate>(filter, "stopDay")),
          DatePanelSettings.get().withSelectPeriodMode(true), true);
      fs.add(dependentFormComponents[1] = stopDate);
      {
        final SubmitLink unselectPeriodLink = new SubmitLink(IconLinkPanel.LINK_ID)
        {
          @Override
          public void onSubmit()
          {
            getSearchFilter().setStartDay(null);
            getSearchFilter().setStopDay(null);
            clearInput();
            parentPage.refresh();
          }

        };
        unselectPeriodLink.setDefaultFormProcessing(false);
        fs.add(new IconLinkPanel(fs.newChildId(), IconType.REMOVE_SIGN,
            new ResourceModel("calendar.tooltip.unselectPeriod"),
            unselectPeriodLink).setColor(CSSColor.RED));
      }
      final QuickSelectPanel quickSelectPanel = new QuickSelectPanel(fs.newChildId(), parentPage, "quickSelect",
          startDate);
      fs.add(quickSelectPanel);
      quickSelectPanel.init();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return WicketUtils.getCalendarWeeks(VisitorbookListForm.this, filter.getStartDay(), filter.getStopDay());
        }
      }));
      fs.add(new HtmlCommentPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return WicketUtils.getUTCDates(filter.getStartDay(), filter.getStopDay());
        }
      }));
    }
  }

  /**
   * @see AbstractListForm#onOptionsPanelCreate(FieldsetPanel, DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "showOnlyActiveEntries"), getString("label.onlyActiveEntries")));
  }

  public VisitorbookListForm(final VisitorbookListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected VisitorbookFilter newSearchFilterInstance()
  {
    return new VisitorbookFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
