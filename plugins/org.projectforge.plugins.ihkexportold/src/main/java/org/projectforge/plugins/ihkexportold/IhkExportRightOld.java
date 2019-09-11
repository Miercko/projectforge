package org.projectforge.plugins.ihkexportold;

import org.apache.commons.lang3.ObjectUtils;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

public class IhkExportRightOld extends UserRightAccessCheck<TimesheetDO>
{
  public IhkExportRightOld(AccessChecker accessChecker)
  {
    super(accessChecker, IhkExportPluginUserRightIdOld.PLUGIN_IHKEXPORT, UserRightCategory.PLUGINS, UserRightValue.TRUE);
  }

  /**
   * @return true if the owner is equals to the logged-in user, otherwise false.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final TimesheetDO obj, final TimesheetDO oldObj,
      final OperationType operationType)
  {
    final TimesheetDO timesheet = oldObj != null ? oldObj : obj;
    if (timesheet == null) {
      return true; // General insert and select access given by default.
    }
    return (ObjectUtils.equals(user.getId(), timesheet.getUserId()) == true);
  }
}
