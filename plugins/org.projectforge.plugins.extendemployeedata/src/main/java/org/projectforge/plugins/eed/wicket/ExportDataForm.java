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

package org.projectforge.plugins.eed.wicket;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.plugins.eed.service.EEDHelper;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class ExportDataForm extends AbstractStandardForm<Object, ExportDataPage>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExportDataForm.class);

  @SpringBean
  private EEDHelper eedHelper;

  protected Integer selectedMonth;

  protected Integer selectedYear;

  public ExportDataForm(ExportDataPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void init()
  {
    super.init();

    //Filter
    //Fieldset for Date DropDown
    final FieldsetPanel fsMonthYear = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("plugins.eed.listcare.yearmonth"));
    //Get actual Month as preselected
    selectedMonth = PFDateTime.now().getMonthValue() + 1;
    //Month DropDown
    DropDownChoicePanel<Integer> ddcMonth = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedMonth"),
            EEDHelper.MONTH_INTEGERS));
    fsMonthYear.add(ddcMonth);
    //Get actual year for pre select
    selectedYear = PFDateTime.now().getYear();
    //Year DropDown
    DropDownChoicePanel<Integer> ddcYear = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedYear"),
            eedHelper.getDropDownYears()));
    fsMonthYear.add(ddcYear);

    final Button exportButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("export"))
    {
      private static final long serialVersionUID = -2985054827068348809L;

      @Override
      public final void onSubmit()
      {
        parentPage.exportData();
      }
    };
    WicketUtils.addTooltip(exportButton, getString("export"));
    final SingleButtonPanel exportButtonPanel = new SingleButtonPanel(actionButtons.newChildId(),
        exportButton,
        getString("export"), SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(exportButtonPanel);
  }

}
