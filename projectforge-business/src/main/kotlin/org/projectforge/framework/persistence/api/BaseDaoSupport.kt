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

package org.projectforge.framework.persistence.api

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.hibernate.search.mapper.orm.Search
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.jpa.candh.CandHMaster
import org.projectforge.framework.persistence.user.entities.PFUserDO


private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object BaseDaoSupport {
    class ResultObject<O : ExtendedBaseDO<Long>>(
        var dbObjBackup: O? = null,
        var wantsReindexAllDependentObjects: Boolean = false,
        var modStatus: EntityCopyStatus? = null
    )

    @JvmStatic
    fun <O : ExtendedBaseDO<Long>> internalSave(baseDao: BaseDao<O>, obj: O, context: PfPersistenceContext): Long? {
        preInternalSave(baseDao, obj, context)
        privateInternalSave(baseDao, obj, context)
        postInternalSave(baseDao, obj, context)
        return obj.id
    }

    private fun <O : ExtendedBaseDO<Long>> privateInternalSave(
        baseDao: BaseDao<O>,
        obj: O,
        context: PfPersistenceContext,
    ) {
        val em = context.em
        // BaseDaoJpaAdapter.prepareInsert(em, obj)
        em.persist(obj)
        if (baseDao.logDatabaseActions) {
            log.info("New ${baseDao.doClass.simpleName} added (${obj.id}): $obj")
        }
        baseDao.prepareHibernateSearch(obj, OperationType.INSERT, context)
        em.merge(obj)
        HistoryBaseDaoAdapter.inserted(obj, context)
        try {
            em.flush()
        } catch (ex: Exception) {
            // Exception stack trace:
            // org.postgresql.util.PSQLException: FEHLER: ungültige Byte-Sequenz für Kodierung »UTF8«: 0x00
            log.error("${ex.message} while saving object: ${ToStringUtil.toJsonString(obj)}", ex)
            throw ex
        }
    }

    private fun <O : ExtendedBaseDO<Long>> preInternalSave(baseDao: BaseDao<O>, obj: O, context: PfPersistenceContext) {
        requireNotNull(obj)
        obj.setCreated()
        obj.setLastUpdate()
        baseDao.onSave(obj, context)
        baseDao.onSaveOrModify(obj, context)
    }

    private fun <O : ExtendedBaseDO<Long>> postInternalSave(
        baseDao: BaseDao<O>,
        obj: O,
        context: PfPersistenceContext,
    ) {
        baseDao.afterSaveOrModify(obj, context)
        baseDao.afterSave(obj, context)
    }

    @JvmStatic
    fun <O : ExtendedBaseDO<Long>> internalUpdate(
        baseDao: BaseDao<O>,
        obj: O,
        checkAccess: Boolean,
        context: PfPersistenceContext,
    ): EntityCopyStatus? {
        preInternalUpdate(baseDao, obj, checkAccess, context)
        val res = ResultObject<O>()
        internalUpdate(baseDao, obj, checkAccess, res, context)
        postInternalUpdate(baseDao, obj, res, context)
        return res.modStatus
    }

    private fun <O : ExtendedBaseDO<Long>> preInternalUpdate(
        baseDao: BaseDao<O>,
        obj: O,
        checkAccess: Boolean,
        context: PfPersistenceContext,
    ) {
        baseDao.beforeSaveOrModify(obj, context)
        baseDao.onSaveOrModify(obj, context)
        if (checkAccess) {
            baseDao.accessChecker.checkRestrictedOrDemoUser()
        }
    }

    private fun <O : ExtendedBaseDO<Long>> internalUpdate(
        baseDao: BaseDao<O>,
        obj: O,
        checkAccess: Boolean,
        res: ResultObject<O>,
        context: PfPersistenceContext,
    ) {
        val em = context.em
        val dbObj = em.find(baseDao.doClass, obj.id)
        if (checkAccess) {
            baseDao.checkLoggedInUserUpdateAccess(obj, dbObj)
        }
        baseDao.onChange(obj, dbObj, context)
        if (baseDao.supportAfterUpdate) {
            res.dbObjBackup = baseDao.getBackupObject(dbObj)
        } else {
            res.dbObjBackup = null
        }
        res.wantsReindexAllDependentObjects = baseDao.wantsReindexAllDependentObjects(obj, dbObj)
        // val result = baseDao.copyValues(obj, dbObj)
        val candHContext = CandHMaster.copyValues(src = obj, dest = dbObj)
        val modStatus = candHContext.currentCopyStatus
        res.modStatus = modStatus
        if (modStatus != EntityCopyStatus.NONE) {
            log.error("*********** To fix: BaseDaoJpaAdapter.prepareUpdate(emgr, dbObj) and history")
            //BaseDaoJpaAdapter.prepareUpdate(emgr, dbObj)
            dbObj.setLastUpdate()
            // } else {
            //   log.info("No modifications detected (no update needed): " + dbObj.toString());
            baseDao.prepareHibernateSearch(obj, OperationType.UPDATE, context)
            em.merge(dbObj)
            HistoryBaseDaoAdapter.updated(dbObj, candHContext.historyEntries, context)
            try {
                em.flush()
            } catch (ex: Exception) {
                // Exception stack trace:
                // org.postgresql.util.PSQLException: FEHLER: ungültige Byte-Sequenz für Kodierung »UTF8«: 0x00
                log.error("${ex.message} while updating object: ${ToStringUtil.toJsonString(obj)}", ex)
                throw ex
            }
            if (baseDao.logDatabaseActions) {
                log.info("${baseDao.doClass.simpleName} updated: $dbObj")
            }
            flushSearchSession(em)
        }
        /*
        res.modStatus = HistoryBaseDaoAdapter.wrapHistoryUpdate(em, dbObj) {
          val result = baseDao.copyValues(obj, dbObj)
          if (result != ModificationStatus.NONE) {
            BaseDaoJpaAdapter.prepareUpdate(emgr, dbObj)
            dbObj.setLastUpdate()
            // } else {
            //   log.info("No modifications detected (no update needed): " + dbObj.toString());
            baseDao.prepareHibernateSearch(obj, OperationType.UPDATE)
            em.merge(dbObj)
            try {
              em.flush()
            } catch (ex: Exception) {
              // Exception stack trace:
              // org.postgresql.util.PSQLException: FEHLER: ungültige Byte-Sequenz für Kodierung »UTF8«: 0x00
              log.error("${ex.message} while updating object: ${ToStringUtil.toJsonString(obj)}", ex)
              throw ex
            }
            if (baseDao.logDatabaseActions) {
              log.info("${baseDao.clazz.simpleName} updated: $dbObj")
            }
            baseDao.flushSearchSession(em)
          }
          result
        }*/
    }

    private fun <O : ExtendedBaseDO<Long>> postInternalUpdate(
        baseDao: BaseDao<O>,
        obj: O,
        res: ResultObject<O>,
        context: PfPersistenceContext
    ) {
        baseDao.afterSaveOrModify(obj, context)
        if (baseDao.supportAfterUpdate) {
            baseDao.afterUpdate(obj, res.dbObjBackup, isModified = res.modStatus != EntityCopyStatus.NONE, context)
            baseDao.afterUpdate(obj, res.dbObjBackup, context)
        } else {
            baseDao.afterUpdate(obj, null, res.modStatus != EntityCopyStatus.NONE, context)
            baseDao.afterUpdate(obj, null, context)
        }
        if (res.wantsReindexAllDependentObjects) {
            baseDao.reindexDependentObjects(obj)
        }
    }


    @JvmStatic
    fun <O : ExtendedBaseDO<Long>> internalMarkAsDeleted(baseDao: BaseDao<O>, obj: O, context: PfPersistenceContext) {
        /*if (!HistoryBaseDaoAdapter.isHistorizable(obj)) {
          log.error(
            "Object is not historizable. Therefore, marking as deleted is not supported. Please use delete instead."
          )
          throw InternalErrorException("exception.internalError")
        }*/
        baseDao.onDelete(obj, context)
        val em = context.em
        val dbObj = em.find(baseDao.doClass, obj.id)
        baseDao.onSaveOrModify(obj, context)

        /*
        HistoryBaseDaoAdapter.wrapHistoryUpdate(emgr, dbObj) {
          BaseDaoJpaAdapter.beforeUpdateCopyMarkDelete(dbObj, obj)*/
        log.error("****** TODO: History stuff")
        baseDao.copyValues(obj, dbObj) // If user has made additional changes.
        dbObj.deleted = true
        dbObj.setLastUpdate()
        obj.deleted = true                     // For callee having same object.
        obj.lastUpdate = dbObj.lastUpdate // For callee having same object.
        em.merge(dbObj) //
        em.flush()/*
              baseDao.flushSearchSession(em)
              null
            }*/
        if (baseDao.logDatabaseActions) {
            log.info("${baseDao.doClass.simpleName} marked as deleted: $dbObj")
        }
        baseDao.afterSaveOrModify(obj, context)
        baseDao.afterDelete(obj, context)
    }

    @JvmStatic
    fun <O : ExtendedBaseDO<Long>> internalUndelete(baseDao: BaseDao<O>, obj: O, context: PfPersistenceContext) {
        baseDao.onSaveOrModify(obj, context)
        val em = context.em
        val dbObj = em.find(baseDao.doClass, obj.id)
        log.error("****** TODO: History stuff")
        /* HistoryBaseDaoAdapter.wrapHistoryUpdate(emgr, dbObj) {
          BaseDaoJpaAdapter.beforeUpdateCopyMarkUnDelete(dbObj, obj)*/
        baseDao.copyValues(obj, dbObj) // If user has made additional changes.
        dbObj.deleted = false
        dbObj.setLastUpdate()
        obj.deleted = false                   // For callee having same object.
        obj.lastUpdate = dbObj.lastUpdate // For callee having same object.
        em.merge(dbObj)
        em.flush()
        //baseDao.flushSearchSession(em)
        if (baseDao.logDatabaseActions) {
            log.info("${baseDao.doClass.simpleName} undeleted: $dbObj")
        }
        dbObj
        baseDao.afterSaveOrModify(obj, context)
        baseDao.afterUndelete(obj, context)
    }

    @JvmStatic
    fun <O : ExtendedBaseDO<Long>> internalForceDelete(baseDao: BaseDao<O>, obj: O, context: PfPersistenceContext) {
        /*if (!HistoryBaseDaoAdapter.isHistorizable(obj)) {
          log.error(
            "Object is not historizable. Therefore use normal delete instead."
          )
          throw InternalErrorException("exception.internalError")
        }*/
        if (!baseDao.isForceDeletionSupport) {
            val msg = "Force deletion not supported by '${baseDao.doClass.name}'. Use markAsDeleted instead for: $obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        val id = obj.id
        if (id == null) {
            val msg = "Could not destroy object unless id is not given: $obj"
            log.error(msg)
            throw RuntimeException(msg)
        }
        throw RuntimeException("TODO: History stuff")
        /*
        baseDao.onDelete(obj)
        val userGroupCache = UserGroupCache.getInstance()
        baseDao.persistenceService.runInTransaction { em ->
            val dbObj = em.find(baseDao.dOClass, id)
            em.remove(dbObj)
            val masterClass: Class<out HistoryMasterBaseDO<*, *>?> =
              PfHistoryMasterDO::class.java //HistoryServiceImpl.getHistoryMasterClass()
            emgr.selectAttached(
              masterClass,
              "select h from ${masterClass.name} h where h.entityName = :entityName and h.entityId = :entityId",
              "entityName", baseDao.clazz.name, "entityId", id.toLong()
            ).forEach { historyEntry ->
              em.remove(historyEntry)
              val displayHistoryEntry = if (historyEntry != null) {
                ToStringUtil.toJsonString(DisplayHistoryEntry(userGroupCache, historyEntry))
              } else {
                "???"
              }
              log.info(
                "${baseDao.clazz.simpleName}:$id (forced) deletion of history entry: $displayHistoryEntry"
              )
            }
            if (baseDao.logDatabaseActions) {
                log.info("${baseDao.dOClass.simpleName} (forced) deleted: $dbObj")
            }
        }
        baseDao.afterDelete(obj)*/
    }

    /**
     * Bulk update.
     */
    @JvmStatic
    fun <O : ExtendedBaseDO<Long>> internalSaveOrUpdate(
        baseDao: BaseDao<O>,
        col: Collection<O>,
        context: PfPersistenceContext,
    ) {
        for (obj in col) {
            if (obj.id != null) {
                preInternalUpdate(baseDao, obj, false, context)
                val res = ResultObject<O>()
                internalUpdate(baseDao, obj, false, res, context)
                postInternalUpdate<O>(baseDao, obj, res, context)
            } else {
                preInternalSave(baseDao, obj, context)
                internalSave(baseDao, obj, context)
                postInternalSave(baseDao, obj, context)
            }
        }
    }

    /**
     * Bulk update.
     * @param col Entries to save or update without check access.
     * @param blockSize The block size of commit blocks.
     */
    @JvmStatic
    fun <O : ExtendedBaseDO<Long>> internalSaveOrUpdate(
        baseDao: BaseDao<O>,
        col: Collection<O>,
        blockSize: Int,
        persistenceService: PfPersistenceService,
    ) {
        val list: MutableList<O> = ArrayList<O>()
        var counter = 0
        for (obj in col) {
            list.add(obj)
            if (++counter >= blockSize) {
                counter = 0
                persistenceService.runInTransaction { context ->
                    internalSaveOrUpdate(baseDao, list, context)
                }
                list.clear()
            }
        }
        persistenceService.runInTransaction { context ->
            internalSaveOrUpdate(baseDao, list, context)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun returnFalseOrThrowException(
        throwException: Boolean,
        user: PFUserDO? = null,
        operationType: OperationType? = null,
        msg: String = "access.exception.noAccess",
    ): Boolean {
        if (throwException) {
            val ex = AccessException(user, msg, operationType)
            ex.operationType = operationType
            throw ex
        }
        return false
    }

    private fun flushSearchSession(em: EntityManager?) {
        if (LUCENE_FLUSH_ALWAYS) {
            val searchSession = Search.session(em)
            // Flushing the index changes asynchonously
            searchSession.indexingPlan().execute()
        }
    }

    private const val LUCENE_FLUSH_ALWAYS = false
}
