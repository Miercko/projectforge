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

package org.projectforge.carddav

import jakarta.servlet.http.HttpServletResponse
import org.projectforge.carddav.CardDavXmlUtils.EXTRACT_ADDRESS_ID_REGEX
import org.projectforge.carddav.model.Contact
import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.*

internal object CardDavUtils {
    const val D = "d"        // xmlns:d="DAV:"
    const val CARD = "card"  // xmlns:card="urn:ietf:params:xml:ns:carddav"
    const val CS = "cs"      // xmlns:cs="http://calendarserver.org/ns/"
    const val ME = "me"      // xmlns:me="http://me.com/_namespace/"

    fun getVcfFileName(contact: Contact): String {
        // If you change this, don't forget to change the regex in CardDavXmlUtils.extractAddressIds.
        return "ProjectForge-${contact.id}.vcf"
    }

    /**
     * Returns the display name of the user's addressbook.
     * This is the name that is shown in the CardDAV client.
     * @param user The user.
     */
    fun getUsersAddressbookDisplayName(user: PFUserDO): String {
        return translateMsg("address.cardDAV.addressbook.displayName", user.firstname)
    }

    /**
     * Simply calls [getEtag] with the contact list.
     */
    fun getCtag(contactList: List<Contact>?): String {
        return getEtag(contactList)
    }

    /**
     * Returns the ETag for the given contact list.
     * The ETag is the timestamp of the last updated contact.
     * @param contactList The contact list. If empty, the ETag is the timestamp of now.
     * @return The ETag.
     */
    fun getEtag(contactList: List<Contact>?): String {
        val lastUpdated = getLastUpdated(contactList)
        return "\"${PFDateTime.fromOrNow(lastUpdated).format(DateFormatType.ISO_TIMESTAMP_MILLIS)}\""
    }

    fun getSyncToken(): String {
        return "\"sync-${System.currentTimeMillis()}\""
    }

    fun getMillisFromSyncToken(syncToken: String?): Long? {
        return syncToken?.removePrefix("sync-")?.toLongOrNull()
    }

    fun getLastUpdated(contactList: List<Contact>?): Date? {
        val oldDate = Date(0)
        return contactList?.maxByOrNull { it.lastUpdated ?: oldDate }?.lastUpdated
    }

    fun extractContactId(path: String): Long? {
        return EXTRACT_ADDRESS_ID_REGEX.find(path)?.groups?.get(1)?.value?.toLong()
    }

    /**
     * Handles the PROPFIND request for the current user principal.
     * If no properties were found, a bad request response is set and null returned.
     * @param requestWrapper The request wrapper.
     * @param response The response.
     * @param user The user.
     * @return The list of properties or null if no properties were found.
     */
    fun handleProps(requestWrapper: RequestWrapper, response: HttpServletResponse): List<Prop>? {
        val props = Prop.extractProps(requestWrapper.body)
        if (props.isEmpty()) {
            ResponseUtils.setValues(
                response, HttpStatus.BAD_REQUEST, contentType = MediaType.TEXT_PLAIN_VALUE,
                content = "No properties found in ${requestWrapper.method} request."
            )
            return null
        }
        return props
    }

    fun setMultiStatusResponse(response: HttpServletResponse, content: String) {
        ResponseUtils.setValues(
            response,
            HttpStatus.MULTI_STATUS,
            contentType = MediaType.APPLICATION_XML_VALUE,
            content = content,
        )
    }

    /**
     * Returns the normalized URI without the CardDAV base path and without leading and trailing slashes.
     * For better comparison, the URI is normalized.
     * @param requestUri The request URI.
     * @return The normalized URI.
     */
    fun normalizedUri(requestUri: String): String {
        return requestUri.removePrefix("/carddav").removePrefix("/").removeSuffix("/")
    }

    /**
     * Returns the URL for the given path.
     * If the path starts with the CardDAV base path, the CardDAV base path is returned.
     * Otherwise, the path is returned with a leading slash.
     * @param requestUri The request URI.
     * @param path The path with a leading slash, but without /carddav (e.g. /users/joe/).
     */
    fun getUrl(requestUri: String, path: String): String {
        require(path.startsWith("/")) { "Path must start with a slash: $path" }
        return if (requestUri.startsWith(CardDavInit.CARD_DAV_BASE_PATH)) {
            "${CardDavInit.CARD_DAV_BASE_PATH}$path"
        } else {
            path
        }
    }

    /**
     * Generate UUID of type 5 (deterministic based on [org.projectforge.business.configuration.DomainService.plainDomain] and userId)
     * @param user The user with ID.
     * @return The generated UUID.
     */
    fun generateDeterministicUUID(user: PFUserDO): String {
        val uuid = UUID.nameUUIDFromBytes("${CardDavService.domain}:${user.id}".toByteArray())
        return "urn:uuid:$uuid"
    }

    /**
     * Returns the URL for the given user: /principals/users/{username}/
     * @param requestUri The request URI.
     * @param user The user.
     * @return The URL for the user.
     */
    fun getPrincipalsUsersUrl(requestUri: String, user: PFUserDO): String {
        return getUrl(requestUri, "/principals/users/${user.username}/")
    }

    /**
     * Returns the URL for the given user: /users/{username}/{subPath}
     * @param requestUri The request URI.
     * @param user The user.
     * @param subPath The sub path to append to the user URL. Mustn't start with a slash.
     * @return The URL for the user.
     */
    fun getUsersUrl(requestUri: String, user: PFUserDO, subPath: String = ""): String {
        require(!subPath.startsWith("/")) { "subPath mustn't start with a slash: $subPath" }
        return getUrl(requestUri, "/users/${user.username}/$subPath")
    }
}
