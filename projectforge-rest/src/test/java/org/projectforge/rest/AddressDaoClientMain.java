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

package org.projectforge.rest;

import org.projectforge.framework.json.JsonUtils;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.model.rest.AddressObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.UserObject;
import org.projectforge.rest.converter.DateTimeFormat;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.Month;
import java.util.Collection;

public class AddressDaoClientMain {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressDaoClientMain.class);

  @SuppressWarnings("unused")
  public static void main(final String[] args) throws IOException {
    final Client client = ClientBuilder.newClient();
    final UserObject user = RestClientMain.authenticate(client);

    final PFDateTime dt = PFDateTime.now().withYear(2013).withMonth(Month.JUNE.getValue());
    final Long modifiedSince = dt.getEpochMilli();
    //modifiedSince = null; // Uncomment this for testing modifiedSince paramter.

    // http://localhost:8080/ProjectForge/rest/task/tree // userId / token
    WebTarget webResource = client.target(RestClientMain.getUrl() + RestPaths.buildOldListPath(RestPaths.ADDRESS))
            .queryParam("search", "");
    if (modifiedSince != null) {
      webResource = webResource.queryParam("modifiedSince", "" + modifiedSince);
    }
    webResource = RestClientMain.setConnectionSettings(webResource,
            new ConnectionSettings().setDateTimeFormat(DateTimeFormat.MILLIS_SINCE_1970));
    final Response response = RestClientMain.getClientResponse(webResource, user);
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      log.error("Failed : HTTP error code : " + response.getStatus());
      return;
    }
    final String json = (String) response.getEntity();
    log.info(json);
    final Collection<AddressObject> col = JsonUtils.fromJson(json, Collection.class);
    for (final AddressObject address : col) {
      log.info(address.getFirstName() + " " + address.getName());
    }
  }
}
