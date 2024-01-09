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
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.plugins.eed.service.EEDHelper;
import org.projectforge.web.core.importstorage.AbstractImportForm;
import org.projectforge.web.core.importstorage.ImportFilter;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

import java.util.Date;
import java.util.GregorianCalendar;

public class EmployeeBillingImportForm extends AbstractImportForm<ImportFilter, EmployeeBillingImportPage, EmployeeBillingImportStoragePanel>
{
  @SpringBean
  private EEDHelper eedHelper;

  FileUploadField fileUploadField;

  private int selectedMonth = PFDateTime.now().getMonthValue() + 1;

  private int selectedYear = PFDateTime.now().getYear();

  private DropDownChoicePanel<Integer> dropDownMonth;

  private DropDownChoicePanel<Integer> dropDownYear;

  public EmployeeBillingImportForm(final EmployeeBillingImportPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();

    gridBuilder.newGridPanel();

    // Date DropDowns
    final FieldsetPanel fsMonthYear = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("plugins.eed.listcare.yearmonth"));

    dropDownMonth = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedMonth"), EEDHelper.MONTH_INTEGERS)
    );
    fsMonthYear.add(dropDownMonth);

    dropDownYear = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedYear"), eedHelper.getDropDownYears()));
    fsMonthYear.add(dropDownYear);

    // upload buttons
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), "*.xls");
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID)
      {
        @Override
        public final void onSubmit()
        {
          final Date dateToSelectAttrRow = new GregorianCalendar(selectedYear, selectedMonth - 1, 1, 0, 0).getTime();
          storagePanel.setDateToSelectAttrRow(dateToSelectAttrRow);
          final boolean success = parentPage.doImport(dateToSelectAttrRow);
          if (success) {
            setDateDropDownsEnabled(false);
          }
        }
      }, getString("upload"), SingleButtonPanel.NORMAL).setTooltip(getString("common.import.upload.tooltip")));
      addClearButton(fs);
    }

    addImportFilterRadio(gridBuilder);

    // preview of the imported data
    gridBuilder.newGridPanel();
    final DivPanel panel = gridBuilder.getPanel();
    storagePanel = new EmployeeBillingImportStoragePanel(panel.newChildId(), parentPage, importFilter);
    final Date dateToSelectAttrRow = new GregorianCalendar(selectedYear, selectedMonth - 1, 1, 0, 0).getTime();
    storagePanel.setDateToSelectAttrRow(dateToSelectAttrRow);
    panel.add(storagePanel);
  }

  void setDateDropDownsEnabled(boolean enabled)
  {
    dropDownMonth.setEnabled(enabled);
    dropDownYear.setEnabled(enabled);
  }
}
