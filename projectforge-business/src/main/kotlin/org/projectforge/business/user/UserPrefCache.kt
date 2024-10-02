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

package org.projectforge.business.user

import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserPrefDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * A cache for UserPrefDO, if preferences are modified and accessed very often by the user's normal work
 * (such as current filters in Calendar and list pages etc.)
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
@DependsOn("entityManagerFactory")
class UserPrefCache : AbstractCache() {

    private val allPreferences = HashMap<Long, UserPrefCacheData>()

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Does nothing for demo user.
     * @param persistent If true (default) this user preference will be stored to the data base, otherwise it will
     * be volatile stored in memory and will expire.
     */
    fun putEntry(area: String, name: String, value: Any?, persistent: Boolean = true, userId: Long?) {
        val uid = userId ?: ThreadLocalUserContext.userId!!
        if (accessChecker.isDemoUser(uid)) {
            // Store user pref for demo user only in user's session.
            return
        }
        val data = ensureAndGetUserPreferencesData(uid)
        if (log.isDebugEnabled) {
            log.debug {
                "Put value for area '$area' and name '$name' (persistent=$persistent): ${
                    ToStringUtil.toJsonString(
                        value ?: "null"
                    )
                }"
            }
        }
        data.putEntry(area, name, value, persistent)
        checkRefresh() // Should be called at the end of this method for considering changes inside this method.
    }

    /**
     * Gets all user's entry for given area.
     */
    fun getEntries(area: String): List<UserPrefDO> {
        val userId = ThreadLocalUserContext.userId!!
        val data = ensureAndGetUserPreferencesData(userId)
        checkRefresh()
        return data.getEntries(area).map { it.userPrefDO }
    }

    /**
     * Gets the user's entry.
     */
    fun getEntry(area: String, name: String): Any? {
        val userId = ThreadLocalUserContext.userId!!
        return getEntry(userId, area, name)
    }

    /**
     * Gets the user's entry.
     */
    @JvmOverloads
    fun <T> getEntry(area: String, name: String, clazz: Class<T>, userId: Long? = null): T? {
        return getEntry(userId ?: ThreadLocalUserContext.userId!!, area, name, clazz)
    }

    fun removeEntry(area: String, name: String) {
        val userId = ThreadLocalUserContext.userId!!
        if (accessChecker.isDemoUser(userId)) {
            // Store user pref for demo user only in user's session.
            return
        }
        return removeEntry(userId, area, name)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun <T> getEntry(userId: Long, area: String, name: String, clazz: Class<T>): T? {
        val value = getEntry(userId, area, name)
        try {
            @Suppress("UNCHECKED_CAST")
            return value as T
        } catch (ex: Exception) {
            log.error("Can't deserialize user pref (new version of ProjectForge and old prefs? ${ex.message}", ex)
            return null
        }
    }

    private fun getEntry(userId: Long, area: String, name: String): Any? {
        val data = ensureAndGetUserPreferencesData(userId)
        checkRefresh()
        val userPref = data.getEntry(area, name)?.userPrefDO ?: return null
        return userPref.valueObject ?: userPrefDao.deserizalizeValueObject(userPref)
    }

    private fun removeEntry(userId: Long, area: String, name: String) {
        val data = getUserPreferencesData(userId)
            ?: // Should only occur for the pseudo-first-login-user setting up the system.
            return
        val cacheEntry = data.getEntry(area, name)
        if (cacheEntry == null) {
            log.info("Oups, user preferences object with area '$area' and name '$name' not cached, can't remove it!")
            return
        }
        if (log.isDebugEnabled) {
            log.debug { "Remove entry for area '$area' and name '$name'." }
        }
        data.removeEntry(area, name)
        if (cacheEntry.persistant && cacheEntry.userPrefDO.id != null)
            userPrefDao.deleteInTrans(cacheEntry.userPrefDO)
        checkRefresh()
    }

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @param userId
     * @return
     */
    @Synchronized
    private fun ensureAndGetUserPreferencesData(userId: Long): UserPrefCacheData {
        var data = getUserPreferencesData(userId)
        if (data == null) {
            data = UserPrefCacheData()
            data.userId = userId
            val userPrefs = userPrefDao.getUserPrefs(userId)
            userPrefs?.forEach {
                data.putEntry(it)
            }
            if (log.isDebugEnabled) {
                log.debug { "Created new UserPrefCacheData: ${ToStringUtil.toJsonString(data)}" }
            }
            synchronized(allPreferences) {
                this.allPreferences[userId] = data
            }
        }
        return data
    }

    internal fun getUserPreferencesData(userId: Long): UserPrefCacheData? {
        synchronized(allPreferences) {
            return this.allPreferences[userId]
        }
    }

    internal fun setUserPreferencesData(userId: Long, data: UserPrefCacheData) {
        synchronized(allPreferences) {
            this.allPreferences[userId] = data
        }
    }

    /**
     * Flushes the user settings to the database (independent from the expire mechanism). Should be used after the user's
     * logout. If the user data isn't modified, then nothing will be done.
     */
    fun flushToDB(userId: Long?) {
        flushToDB(userId, true)
    }

    @Synchronized
    private fun flushToDB(userId: Long?, checkAccess: Boolean) {
        if (checkAccess) {
            if (userId != ThreadLocalUserContext.userId) {
                log.error(
                    "User '" + ThreadLocalUserContext.userId
                            + "' has no access to write user preferences of other user '" + userId + "'."
                )
                // No access.
                return
            }
            val user = persistenceService.selectById(PFUserDO::class.java, userId)
            if (AccessChecker.isDemoUser(user)) {
                // Do nothing for demo user.
                return
            }
        }
        val data = allPreferences[userId]
        data?.getModifiedPersistentEntries()?.forEach {
            if (log.isDebugEnabled) {
                log.debug { "Persisting entry to data base: ${ToStringUtil.toJsonString(it.userPrefDO)}" }
            }
            userPrefDao.internalSaveOrUpdateInTrans(it.userPrefDO)
        }
    }

    /**
     * Stores the PersistentUserObjects in the database or on start up restores the persistent user objects from the
     * database.
     *
     * @see AbstractCache.refresh
     */
    override fun refresh() {
        log.info("Flushing all user preferences to data-base....")
        for (userId in allPreferences.keys) {
            if (log.isDebugEnabled) {
                log.debug { "Flushing all user preferences for user $userId." }
            }
            flushToDB(userId, false)
        }
        log.info("Flushing of user preferences to data-base done.")
    }

    /**
     * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
     *
     * @param userId
     */
    fun clear(userId: Long?) {
        synchronized(allPreferences) {
            val data = allPreferences[userId] ?: return
            if (log.isDebugEnabled) {
                log.debug { "Clearing all user preferences in cache for user $userId." }
            }
            allPreferences.remove(userId)
            data.clear()
        }
    }

    override fun setExpireTimeInMinutes(expireTime: Long) {
        this.expireTime = 10 * TICKS_PER_MINUTE
    }

    @PreDestroy
    fun preDestroy() {
        log.info("Syncing all user preferences to database.")
        this.forceReload()
    }
}
