/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.banking

import mu.KotlinLogging
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseDaoSupport
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class BankAccountRecordDao : BaseDao<BankAccountRecordDO>(BankAccountRecordDO::class.java) {
  @Autowired
  private lateinit var userRights: UserRightService

  @Autowired
  private lateinit var bankAccountDao: BankAccountDao

  private lateinit var bankAccountRight: BankAccountRight

  @PostConstruct
  private fun postConstruct() {
    bankAccountRight = userRights.getRight(BankAccountRightId.PLUGIN_BANKING_ACCOUNT) as BankAccountRight
  }

  override fun hasAccess(
    user: PFUserDO,
    obj: BankAccountRecordDO?,
    oldObj: BankAccountRecordDO?,
    operationType: OperationType,
    throwException: Boolean
  ): Boolean {
    val bankAccount = obj?.bankAccount
    if (obj != null && bankAccount == null) {
      return BaseDaoSupport.returnFalseOrThrowException(
        throwException,
        msg = "Bank account not given.",
        user,
        operationType,
      )
    }
    if (!accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP)) {
      // Double check, user isn't member of financial staff.
      return BaseDaoSupport.returnFalseOrThrowException(
        throwException,
        msg = "User not member of financial staff.",
        user,
        operationType,
      )
    }
    val oldBankAccount = oldObj?.bankAccount
    return bankAccountRight.hasAccess(user, bankAccount, oldBankAccount, operationType)
  }

  override fun newInstance(): BankAccountRecordDO {
    return BankAccountRecordDO()
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  open fun getByTimePeriod(accountId: Int, from: LocalDate, until: LocalDate): List<BankAccountRecordDO> {
    val account = bankAccountDao.getById(accountId) // For access checking
    log.info("Getting records of account '${account.name}', IBAN=${account.iban}: $from until $until")
    return em.createNamedQuery(BankAccountRecordDO.FIND_BY_TIME_PERIOD, BankAccountRecordDO::class.java)
      .setParameter("bankAccountId", accountId)
      .setParameter("from", from)
      .setParameter("until", until)
      .resultList
  }
}
