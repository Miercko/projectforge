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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.json.*
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel.getPropertyLength
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUserId
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.userId
import org.projectforge.framework.persistence.user.api.UserPrefArea
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserPrefDO
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper.parseLong
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.Serializable
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.sql.Date
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}


@SuppressWarnings("deprecation")
@Service
class UserPrefDao : BaseDao<UserPrefDO>(UserPrefDO::class.java) {

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var userDao: UserDao

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    init {
        logDatabaseActions = false
    }

    /**
     * Gets all names of entries of the given area for the current logged in user
     */
    fun getPrefNames(area: UserPrefArea): Array<String> {
        return getPrefNames(area.id)
    }

    /**
     * Gets all names of entries of the given area for the current logged in user
     */
    fun getPrefNames(area: String): Array<String> {
        val user = user
        val names = persistenceService.namedQuery(
            UserPrefDO.FIND_NAMES_BY_USER_AND_AREA,
            String::class.java,
            Pair("userId", user!!.id),
            Pair("area", area)
        )
        return names.toTypedArray()
    }

    fun getListWithoutEntries(areaId: String): List<UserPrefDO> {
        val user = user
        val list = persistenceService.namedQuery(
            UserPrefDO.FIND_IDS_AND_NAMES_BY_USER_AND_AREA,
            Array<Any>::class.java,
            Pair("userId", user!!.id),
            Pair("area", areaId)
        )
        return list.map { oa ->
            val userPref = UserPrefDO()
            userPref.user = user
            userPref.area = areaId
            userPref.id = oa[0] as Long
            userPref.name = oa[1] as String
            userPref
        }
    }

    /**
     * Does (another) entry for the given user with the given area and name already exists?
     *
     * @param id of the current data object (null for new objects).
     */
    fun doesParameterNameAlreadyExist(id: Long?, user: PFUserDO, area: UserPrefArea, name: String): Boolean {
        return doesParameterNameAlreadyExist(id, user.id!!, area.id, name)
    }

    /**
     * Does (another) entry for the given user with the given area and name already exists?
     *
     * @param id of the current data object (null for new objects).
     */
    fun doesParameterNameAlreadyExist(id: Long?, userId: Long?, areaId: String, name: String): Boolean {
        userId ?: return false
        val userPref = if (id != null) {
            persistenceService.selectNamedSingleResult(
                UserPrefDO.FIND_OTHER_BY_USER_AND_AREA_AND_NAME,
                UserPrefDO::class.java,
                Pair("id", id),
                Pair("userId", userId),
                Pair("area", areaId),
                Pair("name", name)
            )
        } else {
            persistenceService.selectNamedSingleResult(
                UserPrefDO.FIND_BY_USER_AND_AREA_AND_NAME,
                UserPrefDO::class.java,
                Pair("userId", userId),
                Pair("area", areaId),
                Pair("name", name)
            )
        }
        return userPref != null
    }

    override fun getList(filter: BaseSearchFilter, context: PfPersistenceContext): List<UserPrefDO> {
        val myFilter = filter as UserPrefFilter
        val queryFilter = QueryFilter(filter)
        if (myFilter.area != null) {
            queryFilter.add(eq("area", myFilter.area.id))
        }
        queryFilter.add(eq("user.id", requiredLoggedInUser))
        queryFilter.addOrder(asc("area"))
        queryFilter.addOrder(asc("name"))
        return getList(queryFilter, context)
    }


    @Deprecated("Use getUserPref(String, Long) instead.")
    fun getUserPref(area: UserPrefArea, name: String?): UserPrefDO? {
        return persistenceService.runReadOnly { context ->
            internalQuery(
                requiredLoggedInUserId,
                area.id,
                name,
                context
            )
        }
    }

    /**
     * @param areaId
     * @param id     id of the user pref to search.
     * @return The user pref of the areaId with the given id of the logged in user (from ThreadLocal).
     */
    fun getUserPref(areaId: String, id: Long): UserPrefDO? {
        return getUserPref(requiredLoggedInUserId, areaId, id)
    }

    private fun getUserPref(userId: Long, areaId: String, id: Long): UserPrefDO? {
        return persistenceService.selectNamedSingleResult(
            UserPrefDO.FIND_BY_USER_AND_AREA_AND_ID,
            UserPrefDO::class.java,
            Pair("userId", userId),
            Pair("area", areaId),
            Pair("id", id)
        )
    }

    fun getUserPrefs(area: UserPrefArea): List<UserPrefDO> {
        val userId = userId
        return getUserPrefs(userId, area)
    }

    fun getUserPrefs(userId: Long?, area: UserPrefArea): List<UserPrefDO> {
        val list = persistenceService.namedQuery(
            UserPrefDO.FIND_BY_USER_ID_AND_AREA,
            UserPrefDO::class.java,
            Pair("userId", userId),
            Pair("area", area.id)
        )
        return selectUnique(list)
    }

    fun getUserPrefs(userId: Long): List<UserPrefDO> {
        val list = persistenceService.namedQuery(
            UserPrefDO.FIND_BY_USER_ID,
            UserPrefDO::class.java,
            Pair("userId", userId)
        )
        return selectUnique(list)
    }

    /**
     * Adds the object fields as parameters to the given userPref. Fields without the annotation UserPrefParameter will be
     * ignored.
     *
     * @see .fillFromUserPrefParameters
     */
    fun addUserPrefParameters(userPref: UserPrefDO, obj: Any) {
        addUserPrefParameters(userPref, obj.javaClass, obj)
    }

    /**
     * Adds the fields of the bean type represented by the given area as parameters to the given userPref. Fields without
     * the annotation UserPrefParameter will be ignored.
     *
     * @see .fillFromUserPrefParameters
     */
    fun addUserPrefParameters(userPref: UserPrefDO, area: UserPrefArea) {
        addUserPrefParameters(userPref, area.beanType, null)
    }

    private fun addUserPrefParameters(userPref: UserPrefDO, beanType: Class<*>, obj: Any?) {
        val fields = beanType.declaredFields
        AccessibleObject.setAccessible(fields, true)
        var no = 0
        for (field in fields) {
            if (field.isAnnotationPresent(UserPrefParameter::class.java)) {
                val userPrefEntry = UserPrefEntryDO()
                userPrefEntry.parameter = field.name
                if (obj != null) {
                    var value: Any? = null
                    try {
                        value = field[obj]
                        userPrefEntry.value = convertParameterValueToString(value)
                    } catch (ex: IllegalAccessException) {
                        log.error(ex.message, ex)
                    }
                    userPrefEntry.valueAsObject = value
                }
                evaluateAnnotation(userPrefEntry, beanType, field)
                if (userPrefEntry.orderString == null) {
                    userPrefEntry.orderString = "ZZZ" + StringHelper.format2DigitNumber(no++)
                }
                userPref.addOrUpdateUserPrefEntry(userPrefEntry)
            }
        }
    }

    private fun evaluateAnnotations(userPref: UserPrefDO, beanType: Class<*>) {
        if (userPref.userPrefEntries == null) {
            return
        }
        val fields = beanType.declaredFields
        var no = 0
        for (field in fields) {
            if (field.isAnnotationPresent(UserPrefParameter::class.java)) {
                var userPrefEntry: UserPrefEntryDO? = null
                for (entry in userPref.userPrefEntries!!) {
                    if (field.name == entry.parameter) {
                        userPrefEntry = entry
                        break
                    }
                }
                if (userPrefEntry == null) {
                    userPrefEntry = UserPrefEntryDO()
                    evaluateAnnotation(userPrefEntry, beanType, field)
                    userPref.addOrUpdateUserPrefEntry(userPrefEntry)
                } else {
                    evaluateAnnotation(userPrefEntry, beanType, field)
                }
                if (StringUtils.isBlank(userPrefEntry.orderString)) {
                    userPrefEntry.orderString = "ZZZ" + StringHelper.format2DigitNumber(no++)
                }
                userPrefEntry.parameter = field.name
            }
        }
    }

    private fun evaluateAnnotation(userPrefEntry: UserPrefEntryDO, beanType: Class<*>, field: Field) {
        val ann = field.getAnnotation(UserPrefParameter::class.java)
        userPrefEntry.i18nKey = ann.i18nKey
        userPrefEntry.tooltipI18nKey = ann.tooltipI18nKey
        userPrefEntry.dependsOn = if (StringUtils.isNotBlank(ann.dependsOn)) ann.dependsOn else null
        userPrefEntry.isRequired = ann.required
        userPrefEntry.isMultiline = ann.multiline
        userPrefEntry.orderString = if (StringUtils.isNotBlank(ann.orderString)) ann.orderString else null
        if (String::class.java.isAssignableFrom(field.type)) {
            userPrefEntry.maxLength = getPropertyLength(beanType, field.name)
        }
        userPrefEntry.type = field.type
    }

    /**
     * Fill object fields from the parameters of the given userPref.
     *
     * @see .addUserPrefParameters
     */
    fun fillFromUserPrefParameters(userPref: UserPrefDO, obj: Any) {
        fillFromUserPrefParameters(userPref, obj, false)
    }

    /**
     * Fill object fields from the parameters of the given userPref.
     *
     * @param preserveExistingValues If true then existing value will not be overwritten by the user pref object. Default
     * is false.
     * @see .addUserPrefParameters
     */
    fun fillFromUserPrefParameters(
        userPref: UserPrefDO, obj: Any,
        preserveExistingValues: Boolean
    ) {
        val fields = obj.javaClass.declaredFields
        AccessibleObject.setAccessible(fields, true)
        userPref.userPrefEntries?.forEach { entry ->
            var field: Field? = null
            for (f in fields) {
                if (f.name == entry.parameter) {
                    field = f
                    break
                }
            }
            if (field == null) {
                log.error(
                    ("Declared field '" + entry.parameter + "' not found for " + obj.javaClass
                            + ". Ignoring parameter.")
                )
            } else {
                val value = getParameterValue(field.type, entry.value)
                try {
                    if (preserveExistingValues) {
                        val oldValue = field[obj]
                        if (oldValue != null) {
                            if (oldValue is String) {
                                if (oldValue.length > 0) {
                                    // Preserve existing value:
                                    return@forEach
                                }
                            } else {
                                // Preserve existing value:
                                return@forEach
                            }
                        }
                    }
                    field[obj] = value
                } catch (ex: IllegalArgumentException) {
                    log.error(
                        (ex.message
                                + " While setting declared field '"
                                + entry.parameter
                                + "' of "
                                + obj.javaClass
                                + ". Ignoring parameter."), ex
                    )
                } catch (ex: IllegalAccessException) {
                    log.error(
                        (ex.message
                                + " While setting declared field '"
                                + entry.parameter
                                + "' of "
                                + obj.javaClass
                                + ". Ignoring parameter."), ex
                    )
                }
            }
        }
    }

    fun setValueObject(userPrefEntry: UserPrefEntryDO, value: Any?) {
        userPrefEntry.value = convertParameterValueToString(value)
        updateParameterValueObject(userPrefEntry)
    }

    fun convertParameterValueToString(value: Any?): String? {
        if (value == null) {
            return null
        }
        if (value is BaseDO<*>) {
            return value.id.toString()
        }
        return value.toString()
    }

    /**
     * Sets the value object by converting it from the value string. The type of the userPrefEntry must be given.
     */
    fun updateParameterValueObject(userPrefEntry: UserPrefEntryDO) {
        userPrefEntry.type?.let { type ->
            userPrefEntry.valueAsObject = getParameterValue(type, userPrefEntry.value)
        }
    }


    @SuppressWarnings("unchecked")
    fun getParameterValue(type: Class<*>, str: String?): Any? {
        if (str == null) {
            return null
        }
        if (type.isAssignableFrom(String::class.java)) {
            return str
        } else if (type.isAssignableFrom(Int::class.java)) {
            return str.toInt()
        } else if (type.isAssignableFrom(Long::class.java)) {
            return TaskDO()
        } else if (DefaultBaseDO::class.java.isAssignableFrom(type)) {
            val id = parseLong(str)
            if (id != null) {
                if (PFUserDO::class.java.isAssignableFrom(type)) {
                    return userDao.getOrLoad(id)
                } else if (TaskDO::class.java.isAssignableFrom(type)) {
                    return taskDao.getOrLoad(id)
                } else if (Kost2DO::class.java.isAssignableFrom(type)) {
                    return kost2Dao.getOrLoad(id)
                } else if (ProjektDO::class.java.isAssignableFrom(type)) {
                    return projektDao.getOrLoad(id)
                } else {
                    log.warn("getParameterValue: Type '$type' not supported. May-be it does not work.")
                    return persistenceService.getReference(type, id)
                }
            } else {
                return null
            }
        } else if (KundeDO::class.java.isAssignableFrom(type)) {
            val id = parseLong(str)
            return if (id != null) {
                kundeDao.getOrLoad(id)
            } else {
                null
            }
        } else if (type.isEnum()) {
            return java.lang.Enum.valueOf(type as Class<out Enum<*>>, str)
        }
        log.error("UserPrefDao does not yet support parameters from type: $type")
        return null
    }

    override fun internalGetById(id: Serializable?, context: PfPersistenceContext): UserPrefDO? {
        val userPref = super.internalGetById(id, context) ?: return null
        if (userPref.areaObject != null) {
            evaluateAnnotations(userPref, userPref.areaObject!!.beanType)
        }
        return userPref
    }

    /**
     * @return Always true, no generic select access needed for user pref objects.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    override fun hasAccess(
        user: PFUserDO, obj: UserPrefDO?, oldObj: UserPrefDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        if (accessChecker.userEquals(user, obj?.user)) {
            return true
        }
        if (throwException) {
            throw AccessException("userPref.error.userIsNotOwner")
        } else {
            return false
        }
    }

    override fun newInstance(): UserPrefDO {
        return UserPrefDO()
    }

    fun deserizalizeValueObject(userPref: UserPrefDO?): Any? {
        userPref ?: return null
        val valueType = userPref.valueType ?: return null
        val valueString = userPref.valueString ?: return null
        if (userPref.valueType == null) return null
        userPref.valueObject = fromJson(valueString, valueType)
        return userPref.valueObject
    }

    override fun onSaveOrModify(obj: UserPrefDO, context: PfPersistenceContext) {
        val valueObject = obj.valueObject
        if (valueObject == null) {
            obj.valueString = null
            obj.valueTypeString = null
        } else {
            obj.valueString = toJson(valueObject)
            obj.valueTypeString = valueObject.javaClass.name
        }
    }

    /**
     * Without check access.
     *
     * @param userId Must be given.
     * @param area   Must be not blank.
     * @param name   Optional, may-be null.
     */
    fun internalQuery(userId: Long, area: String?, name: String?, context: PfPersistenceContext): UserPrefDO? {
        Validate.notBlank(area)
        return if (name == null) {
            context.selectNamedSingleResult(
                UserPrefDO.FIND_BY_USER_ID_AND_AREA_AND_NULLNAME,
                UserPrefDO::class.java,
                Pair("userId", userId),
                Pair("area", area)
            )
        } else {
            context.selectNamedSingleResult(
                UserPrefDO.FIND_BY_USER_AND_AREA_AND_NAME,
                UserPrefDO::class.java,
                Pair("userId", userId),
                Pair("area", area),
                Pair("name", name)
            )
        }
    }

    /**
     * Checks if the user pref already exists in the data base by querying the data base with user id, area and name.
     * The id of the given obj is ignored.
     */
    override fun internalSaveOrUpdate(obj: UserPrefDO, context: PfPersistenceContext): Serializable? {
        val userId = obj.user?.id
        if (userId == null) {
            log.warn("UserId of UserPrefDO is null (can't save it): $obj")
            return null
        }
        synchronized(this) {
            // Avoid parallel insert, update, delete operations.
            val dbUserPref = internalQuery(userId, obj.area, obj.name, context)
            if (dbUserPref == null) {
                obj.id = null // Add new entry (ignore id of any previous existing entry).
                return super.internalSaveOrUpdate(obj, context)
            } else {
                obj.id = dbUserPref.id
                super.internalUpdate(obj, context)
                return obj.id
            }
        }
    }

    /**
     * Only for synchronization with [.internalSaveOrUpdateInTrans].
     *
     * @param obj
     * @throws AccessException
     */
    @Throws(AccessException::class)
    override fun delete(obj: UserPrefDO, context: PfPersistenceContext) {
        synchronized(this) {
            super.delete(obj, context)
        }
    }


    companion object {
        val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "user.username", "user.firstname",
            "user.lastname",
        )

        private const val MAGIC_JSON_START: String = "^JSON:"

        private fun toJson(obj: Any): String {
            try {
                return MAGIC_JSON_START + getObjectMapper().writeValueAsString(obj)
            } catch (ex: JsonProcessingException) {
                log.error("Error while trying to serialze object as json: " + ex.message, ex)
                return ""
            }
        }

        private fun isJsonObject(value: String): Boolean {
            return StringUtils.startsWith(value, MAGIC_JSON_START)
        }

        private fun <T> fromJson(json: String, classOfT: Class<T>): T? {
            var json = json
            if (!isJsonObject(json)) return null
            json = json.substring(MAGIC_JSON_START.length)
            try {
                return getObjectMapper().readValue(json, classOfT)
            } catch (ex: IOException) {
                log.error(
                    "Can't deserialize json object (may-be incompatible ProjectForge versions): " + ex.message + " json=" + json,
                    ex
                )
                return null
            }
        }

        private var objectMapper: ObjectMapper? = null

        fun getObjectMapper(): ObjectMapper {
            objectMapper?.let { return it }
            val mapper = ObjectMapper()
            mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            val module = SimpleModule()
            module.addSerializer(LocalDate::class.java, LocalDateSerializer())
            module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())

            module.addSerializer(PFDateTime::class.java, PFDateTimeSerializer())
            module.addDeserializer(PFDateTime::class.java, PFDateTimeDeserializer())

            module.addSerializer(
                java.util.Date::class.java,
                UtilDateSerializer(UtilDateFormat.ISO_DATE_TIME_SECONDS)
            )
            module.addDeserializer(java.util.Date::class.java, UtilDateDeserializer())

            module.addSerializer(Date::class.java, SqlDateSerializer())
            module.addDeserializer(Date::class.java, SqlDateDeserializer())

            mapper.registerModule(module)
            mapper.registerModule(KotlinModule.Builder().build())
            objectMapper = mapper
            return mapper
        }

    }
}
