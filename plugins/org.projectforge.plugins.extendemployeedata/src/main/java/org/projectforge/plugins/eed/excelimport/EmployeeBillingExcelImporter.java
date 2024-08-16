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

package org.projectforge.plugins.eed.excelimport;

import de.micromata.merlin.excel.importer.ImportStorage;
import de.micromata.merlin.excel.importer.ImportedSheet;
import org.projectforge.business.excel.ExcelImport;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.export.AttrColumnDescription;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.utils.MyImportedElement;
import org.projectforge.plugins.eed.ExtendEmployeeDataEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmployeeBillingExcelImporter {
    private static final Logger log = LoggerFactory.getLogger(EmployeeBillingExcelImporter.class);

    private static final String NAME_OF_EXCEL_SHEET = "employees";

    private static final int ROW_INDEX_OF_COLUMN_NAMES = 0;

    private static final String[] DIFF_PROPERTIES = {};

    private final EmployeeService employeeService;

    //private final TimeableService timeableService;

    private final ImportStorage<EmployeeDO> storage;

    private final Date dateToSelectAttrRow;

    public EmployeeBillingExcelImporter(final EmployeeService employeeService,
                                        //final TimeableService timeableService,
                                        final ImportStorage<EmployeeDO> storage, final Date dateToSelectAttrRow) {
        this.employeeService = employeeService;
        // this.timeableService = timeableService;
        this.storage = storage;
        this.dateToSelectAttrRow = dateToSelectAttrRow;
    }

    public List<AttrColumnDescription> doImport(final InputStream is) throws IOException {
        final ExcelImport<EmployeeBillingExcelRow> importer = new ExcelImport<>(is);

        // search the sheet
        for (short idx = 0; idx < importer.getWorkbook().getNumberOfSheets(); idx++) {
            importer.setActiveSheet(idx);
            final String name = importer.getWorkbook().getSheetName(idx);
            if (NAME_OF_EXCEL_SHEET.equals(name)) {
                importer.setActiveSheet(idx);
                //        final HSSFSheet sheet = importer.getWorkbook().getSheetAt(idx);
                return importEmployeeBillings(importer);
            }
        }
        log.error("Oups, no sheet named '" + NAME_OF_EXCEL_SHEET + "' found.");
        return null;
    }

    private List<AttrColumnDescription> importEmployeeBillings(final ExcelImport<EmployeeBillingExcelRow> importer) {
        final ImportedSheet<EmployeeDO> importedSheet = new ImportedSheet<>(new ImportStorage<>());
        storage.addSheet(importedSheet);
        importedSheet.setName(NAME_OF_EXCEL_SHEET);
        importer.setNameRowIndex(ROW_INDEX_OF_COLUMN_NAMES);
        importer.setStartingRowIndex(ROW_INDEX_OF_COLUMN_NAMES + 1);

        // mapping from excel column name to the bean field name
        final Map<String, String> map = new HashMap<>();
        map.put("Id", "id");
        map.put(I18nHelper.getLocalizedMessage("fibu.employee.user"), "fullName");

        ExtendEmployeeDataEnum.getAllAttrColumnDescriptions().forEach(
                desc -> map.put(I18nHelper.getLocalizedMessage(desc.getI18nKey()), desc.getCombinedName()));
        importer.setColumnMapping(map);

        final List<AttrColumnDescription> attrColumnsInSheet = getAttrColumnsUsedInSheet(importer);

        final EmployeeBillingExcelRow[] rows = importer.convertToRows(EmployeeBillingExcelRow.class);
        int rowNum = 0;
        for (final EmployeeBillingExcelRow row : rows) {
            final MyImportedElement<EmployeeDO> element = convertRowToDo(importedSheet, attrColumnsInSheet, row, rowNum);
            importedSheet.addElement(element);
        }

        return attrColumnsInSheet;
    }

    private List<AttrColumnDescription> getAttrColumnsUsedInSheet(ExcelImport<EmployeeBillingExcelRow> importer) {
        final List<String> columnNames = importer.getColumnNames();
        return ExtendEmployeeDataEnum
                .getAllAttrColumnDescriptions()
                .stream()
                .filter(desc -> columnNames.contains(I18nHelper.getLocalizedMessage(desc.getI18nKey())))
                .collect(Collectors.toList());
    }

    private MyImportedElement<EmployeeDO> convertRowToDo(final ImportedSheet<EmployeeDO> importedSheet,
                                                         final List<AttrColumnDescription> attrColumnsInSheet,
                                                         final EmployeeBillingExcelRow row,
                                                         final int rowNum) {
/*    final MyImportedElement<EmployeeDO> element = new MyImportedElementWithAttrs(importedSheet, rowNum, EmployeeDO.class, attrColumnsInSheet,
        dateToSelectAttrRow, timeableService, DIFF_PROPERTIES);
    EmployeeDO employee;
    if (row.getId() != null) {
      employee = employeeService.selectByPkDetached(row.getId());
      // validate ID and USER: make sure that full name has not changed
      if (!StringUtils.equals(row.getFullName(), employee.getUser().getFullname())) {
        element.putErrorProperty("user", row.getFullName());
      }
    } else {
      // this employee is just created to show it in the EmployeeBillingImportStoragePanel, it will never be imported to the DB
      employee = new EmployeeDO();
      element.putErrorProperty("id", row.getId());
    }
    element.setValue(employee);

    attrColumnsInSheet.forEach(
        desc -> getOrCreateAttrRowAndPutAttribute(employee, desc, row)
    );

    return element;
  }

  private void getOrCreateAttrRowAndPutAttribute(final EmployeeDO employee, final AttrColumnDescription colDesc, final EmployeeBillingExcelRow row)
  {
    EmployeeTimedDO attrRow = timeableService.getAttrRowForSameMonth(employee, colDesc.getGroupName(), dateToSelectAttrRow);

    if (attrRow == null) {
      attrRow = employeeService.addNewTimeAttributeRow(employee, colDesc.getGroupName());
      attrRow.setStartTime(dateToSelectAttrRow);
    }

    final Object fieldValue = PrivateBeanUtils.readField(row, colDesc.getCombinedName());
    attrRow.putAttribute(colDesc.getPropertyName(), fieldValue);
    */
        return null;
    }
}

