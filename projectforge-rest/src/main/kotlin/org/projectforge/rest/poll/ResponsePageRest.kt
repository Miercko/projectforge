package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDO
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.poll.types.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


@RestController
@RequestMapping("${Rest.URL}/response")
class ResponsePageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var userService: UserService

    private var pollId: Int? = null
    private var questionOwnerId: Int? = null

    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("pollId") pollStringId: String?,
        @RequestParam("questionOwner") delUser: String?,
        @RequestParam("returnToCaller") returnToCaller: String?,
    ): FormLayoutData {
        if (pollId === null || (pollStringId != null && pollId != null)) {
            pollId = NumberHelper.parseInteger(pollStringId) ?: throw IllegalArgumentException("id not given.")
        }
        // used to load answers, is an attendee chosen by a fullAccessUser in order to answer for them or the ThreadLocal User
        val pollData = pollDao.internalGetById(pollId) ?: PollDO()

        var answerTitle = ""
        if (delUser != null && pollDao.hasFullAccess(pollData) && pollDao.isAttendee(pollData, delUser.toInt())) {
            questionOwnerId = delUser.toInt()
            answerTitle = translateMsg("poll.delegationAnswers") + userService.getUser(questionOwnerId).displayName
        } else {
            questionOwnerId = ThreadLocalUserContext.userId
            answerTitle = translateMsg("poll.yourAnswers")
        }


        val pollDto = transformPollFromDB(pollData)


        val layout = UILayout("poll.response.title")


        if (pollDao.hasFullAccess(pollData)) {
            layout.add(
                MenuItem(
                    "EDIT",
                    i18nKey = "poll.title.edit",
                    url = PagesResolver.getEditPageUrl(PollPageRest::class.java, pollDto.id),
                    type = MenuItemTargetType.REDIRECT
                )
            )

            if (!pollDto.isFinished() && ThreadLocalUserContext.userId === questionOwnerId) {
                val fieldSetDelegationUser = UIFieldset(title = "poll.userDelegation")
                fieldSetDelegationUser.add(
                    UIInput(
                        id = "delegationUser",
                        label = "user",
                        dataType = UIDataType.USER
                    )
                )
                    .add(UISpacer())
                    .add(
                        UIButton.createDefaultButton(
                            id = "response-poll-button",
                            responseAction = ResponseAction(
                                RestResolver.getRestUrl(
                                    this::class.java,
                                    "showDelegatedUser"
                                ),
                                targetType = TargetType.GET
                            ),
                            title = "poll.selectUser"
                        ),
                    )
                layout.add(fieldSetDelegationUser)
            }
        }

        val fieldSet = UIFieldset(12, title = pollDto.title + " - " + answerTitle)
        fieldSet.add(UIReadOnlyField(value = pollDto.description, label = translateMsg("poll.description")))
            .add(UIReadOnlyField(value = pollDto.location, label = translateMsg("poll.location")))
            .add(UIReadOnlyField(value = pollDto.owner?.displayName, label = translateMsg("poll.owner")))
            .add(UIReadOnlyField(value = pollDto.deadline.toString(), label = translateMsg("poll.deadline")))
            .add(UISpacer())
            .add(UISpacer())
        layout.add(fieldSet)


        val pollResponse = PollResponse()
        pollResponse.poll = pollData

        pollResponseDao.internalLoadAll().firstOrNull { response ->
            response.owner?.id == questionOwnerId
                    && response.poll?.id == pollData.id
        }?.let {
            pollResponse.copyFrom(it)
        }

        pollDto.inputFields?.forEachIndexed { index, field ->
            val fieldSetQuestions = UIFieldset(title = field.question)
            val questionAnswer = QuestionAnswer()
            questionAnswer.uid = UUID.randomUUID().toString()
            questionAnswer.questionUid = field.uid
            pollResponse.responses?.firstOrNull {
                it.questionUid == field.uid
            }.let {
                if (it == null) pollResponse.responses?.add(questionAnswer)
            }

            val col = UICol()

            if (field.type == BaseType.TextQuestion) {
                col.add(
                    PollPageRest.getUiElement(
                        pollDto.isFinished(),
                        "responses[$index].answers[0]",
                        "poll.question.textQuestion",
                        UIDataType.STRING
                    )
                )
            }

            if (field.type == BaseType.MultiResponseQuestion || field.type === BaseType.SingleResponseQuestion) {
                field.answers?.forEachIndexed { index2, _ ->
                    if (pollResponse.responses?.get(index)?.answers?.getOrNull(index2) == null) {
                        pollResponse.responses?.get(index)?.answers?.add(index2, false)
                    }
                    if (field.type == BaseType.MultiResponseQuestion) {
                        col.add(
                            UICheckbox(
                                "responses[$index].answers[$index2]",
                                label = field.answers?.get(index2) ?: ""
                            )
                        )
                    } else {
                        col.add(
                            UIRadioButton(
                                "responses[$index].answers[0]",
                                value = field.answers?.get(index2) ?: "",
                                label = field.answers?.get(index2) ?: ""
                            )
                        )
                    }
                }
            }
            fieldSetQuestions.add(UIRow().add(col))
            fieldSet.add(fieldSetQuestions)
        }

        val backUrl = if (returnToCaller.isNullOrEmpty()) {
            PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true)
        } else {
            // Fix doubled encoding:
            returnToCaller.replace("%2F", "/")
        }
        layout.add(
            UIButton.createBackButton(
                responseAction = ResponseAction(
                    backUrl,
                    targetType = TargetType.REDIRECT
                ),
                default = true
            )
        )

        if (!pollDto.isFinished()) {
            layout.add(
                UIButton.createDefaultButton(
                    id = "addResponse",
                    title = translateMsg("poll.respond"),
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(
                            this::class.java,
                            "addResponse"
                        ) + "/?questionOwner=${questionOwnerId}", targetType = TargetType.POST
                    )
                )
            )
        }

        layout.watchFields.add("delegationUser")
        LayoutUtils.process(layout)
        return FormLayoutData(pollResponse, layout, createServerData(request))
    }


    @PostMapping("addResponse")
    fun addResponse(
        request: HttpServletRequest,
        @RequestBody postData: PostData<PollResponse>, @RequestParam("questionOwner") questionOwner: Int?
    ): ResponseEntity<ResponseAction>? {
        val pollResponseDO = PollResponseDO()
        postData.data.copyTo(pollResponseDO)

        pollResponseDO.owner = userService.getUser(questionOwner)
        pollResponseDao.internalLoadAll().firstOrNull { pollResponse ->
            pollResponse.owner?.id == questionOwner
                    && pollResponse.poll?.id == postData.data.poll?.id
        }?.let {
            it.responses = pollResponseDO.responses
            pollResponseDao.update(it)
            return ResponseEntity.ok(
                ResponseAction(
                    targetType = TargetType.REDIRECT,
                    url = PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true)
                )
            )
        }


        pollResponseDao.saveOrUpdate(pollResponseDO)

        return ResponseEntity.ok(
            ResponseAction(
                targetType = TargetType.REDIRECT,
                url = PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true)
            )
        )
    }


    @GetMapping("showDelegatedUser")
    fun showDelegatedUser(
        request: HttpServletRequest
    ): ResponseEntity<ResponseAction>? {
        var attendees = pollDao.internalGetById(pollId).attendeesIds
        if (attendees != null && attendees.split(",").any { it.toIntOrNull() == questionOwnerId }) {
            return ResponseEntity.ok(
                ResponseAction(
                    url = "/react/response/dynamic?pollId=${pollId}&questionOwner=${questionOwnerId}",
                    targetType = TargetType.REDIRECT
                )
            )
        } else throw AccessException("poll.exception.noAttendee")
    }


    @PostMapping(RestPaths.WATCH_FIELDS)
    fun watchFields(@Valid @RequestBody postData: PostData<Poll>): ResponseEntity<ResponseAction> {
        questionOwnerId = postData.data.delegationUser?.id
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.UPDATE))
    }


    private fun transformPollFromDB(obj: PollDO): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        if (obj.inputFields != null) {
            val a = ObjectMapper().readValue(obj.inputFields, MutableList::class.java)
            poll.inputFields = a.map { Question().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
        return poll
    }

}