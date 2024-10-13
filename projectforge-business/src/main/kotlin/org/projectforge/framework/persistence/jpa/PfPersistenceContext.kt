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

package org.projectforge.framework.persistence.jpa

import jakarta.persistence.*
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Root
import mu.KotlinLogging
import org.hibernate.NonUniqueResultException
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceContext.ContextType

private val log = KotlinLogging.logger {}

/**
 * A wrapper for EntityManager with some convenience methods. The EntityManager is created by the given entityManagerFactory.
 */
class PfPersistenceContext internal constructor(
    entityManagerFactory: EntityManagerFactory,
    /**
     * If [ContextType.TRANSACTION] all actions are executed in a transaction.
     * If [ContextType.READONLY], no transaction is used, and it's a readonly context.
     */
    internal var type: ContextType,
) : AutoCloseable {
    internal enum class ContextType { READONLY, TRANSACTION }

    val em: EntityManager = entityManagerFactory.createEntityManager()

    /**
     * This id is used for logging and debugging purposes.
     * It's a UUID (generated on init).
     */
    val contextId = nextContextId // AtomicInteger


    /* init {
        openEntityManagers.add(em)
        log.info { "Created EntityManager: $em (${openEntityManagers.size} opened entity managers)." }
    }*/

    fun <T> selectById(
        entityClass: Class<T>,
        id: Any?,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): T? {
        id ?: return null
        val entity = if (lockModeType != null) {
            em.find(entityClass, id, lockModeType)
        } else {
            em.find(entityClass, id)
        }
        entity ?: return null
        if (!attached && em.contains(entity)) {
            em.detach(entity)
        }
        return entity
    }

    /**
     * @param nullAllowed If false, an exception is thrown if no result is found.
     * @param errorMessage If not null, this message is used in the exception.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    @JvmOverloads
    fun <T> selectNamedSingleResult(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
    ): T? {
        return selectSingleResult(
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            nullAllowed = nullAllowed,
            errorMessage = errorMessage,
            attached = attached,
            namedQuery = true,
        )
    }

    /**
     * @param nullAllowed If false, an exception is thrown if no result is found.
     * @param errorMessage If not null, this message is used in the exception.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    @JvmOverloads
    fun <T> selectSingleResult(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
        namedQuery: Boolean = false,
    ): T? {
        val result = try {
            createQuery(
                sql = sql,
                resultClass = resultClass,
                keyValues = keyValues,
                namedQuery = namedQuery
            ).singleResult
        } catch (ex: NoResultException) {
            if (!nullAllowed) {
                throw InternalErrorException("${ex.message}: $sql ${errorMessage ?: ""}")
            }
            return null
        } catch (ex: NonUniqueResultException) {
            throw InternalErrorException("${ex.message}: $sql ${errorMessage ?: ""}")
        }
        if (!attached && HibernateUtils.isEntity(resultClass) && em.contains(result)) {
            em.detach(result)
        }
        return result
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> queryNullable(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): List<T?> {
        val q = createQuery(sql = sql, resultClass = resultClass, keyValues = keyValues)
        if (lockModeType != null) {
            q.lockMode = lockModeType
        }
        val ret = q.resultList
        if (!attached && HibernateUtils.isEntity(resultClass)) {
            ret.forEach { obj ->
                if (obj != null && em.contains(obj)) {
                    em.detach(obj)
                }
            }
        }
        return ret
    }

    /**
     * No null result values are allowed.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> executeQuery(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
    ): List<T> {
        val q = createQuery(sql = sql, resultClass = resultClass, keyValues = keyValues, namedQuery = namedQuery)
        if (lockModeType != null) {
            q.lockMode = lockModeType
        }
        if (maxResults != null) {
            q.maxResults = maxResults
        }
        val ret = q.resultList
        if (!attached && HibernateUtils.isEntity(resultClass)) {
            ret.forEach { obj ->
                if (obj != null && em.contains(obj)) {
                    em.detach(obj)
                }
            }
        }
        return ret
    }

    /**
     * Convenience call for query() with namedQuery = true.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> executeNamedQuery(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        maxResults: Int? = null,
    ): List<T> {
        return executeQuery(
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            attached = attached,
            namedQuery = true,
            maxResults = maxResults,
        )
    }


    fun <T> createQuery(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): TypedQuery<T> {
        val query: TypedQuery<T> = if (namedQuery) {
            em.createNamedQuery(sql, resultClass)
        } else {
            em.createQuery(sql, resultClass)
        }
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query
    }

    /**
     * em.persist() is used.
     */
    fun insert(
        dbObj: Any,
    ) {
        em.persist(dbObj)
    }

    /**
     * Calls [EntityManager.merge].
     */
    fun update(
        dbObj: Any,
    ) {
        em.merge(dbObj)
    }

    /**
     * Calls [EntityManager.remove].
     */
    fun delete(
        dbObj: Any,
    ) {
        em.remove(dbObj)
    }

    /**
     * First selects the entity by id via [selectById] and then calls [EntityManager.remove].
     */
    fun <T> delete(
        entityClass: Class<T>,
        id: Any,
    ) {
        selectById(entityClass, id, attached = true)?.let { dbObj ->
            em.remove(dbObj)
        }
    }

    fun <T> criteriaUpdate(
        entityClass: Class<T>,
        update: (cb: CriteriaBuilder, root: Root<T>, update: CriteriaUpdate<T>) -> Unit
    ) {
        val cb = em.criteriaBuilder
        val criteriaUpdate = cb.createCriteriaUpdate(entityClass)
        // define root-Instanz
        val root = criteriaUpdate.from(entityClass)
        update(cb, root, criteriaUpdate)
        em.createQuery(criteriaUpdate).executeUpdate()
    }

    /**
     * Calls [EntityManager.createQuery] or, for named queries [EntityManager.createNamedQuery] and then [Query.executeUpdate]
     * after setting all parameters.
     */
    fun executeUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): Int {
        val query = if (namedQuery) {
            em.createNamedQuery(sql)
        } else {
            em.createQuery(sql)
        }
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query.executeUpdate()
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNamedUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): Int {
        return executeUpdate(sql, keyValues = keyValues, namedQuery = true)
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNativeUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): Int {
        val query = em.createNativeQuery(sql)
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query.executeUpdate()
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNativeQuery(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): List<*> {
        val query = em.createNativeQuery(sql)
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query.resultList
    }

    fun <T> getReference(
        entityClass: Class<T>, id: Any
    ): T {
        return em.getReference(entityClass, id)
    }

    /**
     * Calls em.flush().
     */
    fun flush() {
        em.flush()
    }

    override fun close() {
        if (em.isOpen) {
            em.close()
            //openEntityManagers.remove(em)
            //log.info { "Closed EntityManager: $em (${openEntityManagers.size} opened entity managers)." }
        }
    }

    /**
     * Gets the next number for a new entity. The next number is the maximum number of the attribute + 1.
     * @param table The name of the table (e. g. RechnungDO).
     * @param attribute The name of the attribute (e. g. rechnungsnummer).
     * @param startNumber The number to start with if no entry is found.
     */
    fun getNextNumber(table: String, attribute: String, startNumber: Int = 0): Int {
        val maxNumber = selectSingleResult(
            "select max(t.$attribute) from $table t",
            Int::class.java,
        ) ?: run {
            log.info("First entry of $table, starting with number ${startNumber + 1}.")
            startNumber
        }
        return maxNumber + 1
    }

    companion object {
        private val nextContextId: Long
            get() {
                synchronized(this) {
                    return contextCounter++
                }
            }
        private var contextCounter = 0L
        //    private val openEntityManagers = mutableSetOf<EntityManager>()
    }
}
