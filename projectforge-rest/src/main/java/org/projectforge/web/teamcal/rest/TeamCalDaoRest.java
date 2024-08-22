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

package org.projectforge.web.teamcal.rest;

import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.TeamCalFilter;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.framework.json.JsonUtils;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.model.rest.CalendarObject;
import org.projectforge.model.rest.RestPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * REST interface for {@link TeamCalDao}.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
@Path(RestPaths.TEAMCAL)
public class TeamCalDaoRest
{
  @Autowired
  private TeamCalDao teamCalDao;

  @Autowired
  private UserRightService userRights;

  @Autowired
  private UserXmlPreferencesDao userXmlPreferencesDao;

  /**
   * Rest-Call for {@link TeamCalDao#getList(org.projectforge.core.BaseSearchFilter)}
   */
  @GET
  @Path(RestPaths.LIST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList(@QueryParam("fullAccess") final boolean fullAccess)
  {
    final TeamCalFilter filter = new TeamCalFilter();
    if (fullAccess) {
      filter.setFullAccess(true);
      filter.setMinimalAccess(false);
      filter.setReadonlyAccess(false);
    }
    final List<TeamCalDO> list = teamCalDao.getList(filter);
    Integer[] teamCalBlackListIds = userXmlPreferencesDao
        .getDeserializedUserPreferencesByUserId(ThreadLocalUserContext.getUserId(), TeamCalDO.Companion.getTEAMCALRESTBLACKLIST(), Integer[].class);
    if(teamCalBlackListIds != null && teamCalBlackListIds.length > 0) {
      Arrays.stream(teamCalBlackListIds).forEach(calId -> list.remove(teamCalDao.getById(calId)));
    }
    final List<CalendarObject> result = new LinkedList<>();
    if (list != null && list.size() > 0) {
      for (final TeamCalDO cal : list) {
        result.add(TeamCalDOConverter.getCalendarObject(cal, userRights));
      }
    }
    final String json = JsonUtils.toJson(result);
    return Response.ok(json).build();
  }

}
