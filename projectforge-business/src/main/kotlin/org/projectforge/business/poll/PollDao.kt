package org.projectforge.business.poll

import org.projectforge.business.group.service.GroupService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class PollDao : BaseDao<PollDO>(PollDO::class.java) {

    @Autowired
    private val groupService: GroupService? = null

    override fun newInstance(): PollDO {
        return PollDO()
    }

    override fun hasAccess(
        user: PFUserDO?,
        obj: PollDO?,
        oldObj: PollDO?,
        operationType: OperationType?,
        throwException: Boolean
    ): Boolean {

        if (obj == null && operationType == OperationType.SELECT) {
            return true
        };
        if (obj != null && operationType == OperationType.SELECT){
            if(hasFullAccess(obj) || isAttendee(obj, ThreadLocalUserContext.user?.id!!))
                return true
        }
        if (obj != null) {
            return hasFullAccess(obj)
        }
        return false
    }

    //returns true if current user hast full access, otherwise returns false
    fun hasFullAccess(obj: PollDO): Boolean {
        val loggedInUserId = ThreadLocalUserContext.userId!!
        if (!obj.fullAccessUserIds.isNullOrBlank()) {
            val userIdArray = obj.fullAccessGroupIds!!.split(", ").map { it.toInt() }.toIntArray()
            if (userIdArray.contains(loggedInUserId))
                return true
        }
        if (obj.owner?.id == loggedInUserId)
            return true
        if (!obj.fullAccessGroupIds.isNullOrBlank()) {
            val groupIdArray = obj.fullAccessGroupIds!!.split(", ").map { it.toInt() }.toIntArray()
            val groupUsers = groupService?.getGroupUsers(groupIdArray)
            groupUsers!!.map { it.id }.forEach {
                if (it == loggedInUserId)
                    return true
            }
        }
        return false
    }

    fun isAttendee(obj: PollDO, user: Int?): Boolean {
        if (!obj.attendeeIds.isNullOrBlank() && obj.attendeeIds!!.split(", ").contains(user.toString())
        )
            return true
        return false
    }
}