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

package org.projectforge.business.task;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hibernate.Hibernate;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragsPositionVO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.utils.StackTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

/**
 * Holds the complete task list in a tree. It will be initialized by the values read from the database. Any changes will
 * be written to this tree and to the database.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class TaskTree extends AbstractCache implements Serializable {
  private static final long serialVersionUID = 3748005966442878168L;

  public static final String USER_PREFS_KEY_OPEN_TASKS = "openTasks";

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private AccessDao accessDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private KostCache kostCache;

  @Autowired
  private AuftragDao auftragDao;

  @Autowired
  private TimesheetDao timesheetDao;

  private static final List<TaskNode> EMPTY_LIST = new ArrayList<>();

  private static TaskTree INSTANCE;

  public static TaskTree getInstance() {
    return INSTANCE;
  }

  @PostConstruct
  private void postConstruct() {
    if (INSTANCE != null) {
      log.warn("Oups, shouldn't instantiate TaskTree twice");
      return;
    }
    INSTANCE = this;
    auftragDao.registerTaskTree(this);
  }

  /**
   * For log messages.
   */
  private static final Logger log = LoggerFactory.getLogger(TaskTree.class);

  /**
   * Time of last modification in milliseconds from 1970-01-01.
   */
  private long timeOfLastModification = 0;

  /**
   * For faster searching of entries.
   */
  private Map<Long, TaskNode> taskMap;

  /**
   * The root node of all tasks. The only node with parent null.
   */
  private TaskNode root = null;

  private Map<Long, Set<AuftragsPositionVO>> orderPositionReferences;

  private boolean orderPositionReferencesDirty = true;

  public TaskNode getRootTaskNode() {
    checkRefresh();
    return this.root;
  }

  /**
   * Adds the given node as child of the given parent.
   */
  private synchronized TaskNode addTaskNode(final TaskNode node, final TaskNode parent) {
    checkRefresh();
    if (parent != null) {
      node.setParent(parent);
      parent.addChild(node);
    }
    updateTimeOfLastModification();
    return node;
  }

  /**
   * Adds a new node with the given data. The given Task holds all data and the information (id) of the parent node of
   * the node to add. Will be called by TaskDAO after inserting a new task.
   */
  TaskNode addTaskNode(final TaskDO task) {
    checkRefresh();
    final TaskNode node = new TaskNode();
    node.setTask(task);
    final TaskNode parent = getTaskNodeById(task.getParentTaskId());
    if (parent != null) {
      node.setParent(parent);
    } else if (root == null) {
      // this is the root node:
      root = node;
    } else if (!node.getId().equals(root.getId())) {
      // This node is not the root node:
      node.setParent(root);
    }
    taskMap.put(node.getId(), node);
    final TimesheetDO timesheet = new TimesheetDO();
    timesheet.setTask(task);
    final boolean bookable = timesheetDao.checkTaskBookable(timesheet, null, OperationType.INSERT, false);
    node.bookableForTimesheets = bookable;
    return addTaskNode(node, parent);
  }

  /**
   * @param taskId
   * @param ancestorTaskId
   * @return
   * @see TaskNode#getPathToAncestor(Long)
   */
  public List<TaskNode> getPath(final Long taskId, final Long ancestorTaskId) {
    checkRefresh();
    if (taskId == null) {
      return EMPTY_LIST;
    }
    final TaskNode taskNode = getTaskNodeById(taskId);
    if (taskNode == null) {
      return EMPTY_LIST;
    }
    return taskNode.getPathToAncestor(ancestorTaskId);
  }

  /**
   * Returns the path to the root node in an ArrayList.
   *
   * @see #getPath(Long, Long)
   */
  public List<TaskNode> getPathToRoot(final Long taskId) {
    return getPath(taskId, null);
  }

  public List<Long> getAncestorTaskIs(final Long taskId) {
    final List<TaskNode> resultList = getPathToRoot(taskId);
    final List<Long> result = new ArrayList<>();
    int i = 0;
    for (final TaskNode node : resultList) {
      result.add(node.getId());
    }
    return result;
  }

  public List<Long> getAncestorAndDescendantTaskIs(final Long taskId, final boolean includeSelf) {
    final List<Long> result = getAncestorTaskIs(taskId);
    result.addAll(getDescendantTaskIds(taskId, includeSelf));
    return result;
  }

  public List<Long> getDescendantTaskIds(final Long taskId, final boolean includeSelf) {
    final List<TaskNode> resultList = getDescendants(taskId, includeSelf);
    final List<Long> result = new ArrayList<>();
    int i = 0;
    for (final TaskNode node : resultList) {
      result.add(node.getId());
    }
    return result;
  }

  public List<TaskNode> getDescendants(final Long taskId, final boolean includeSelf) {
    final List<TaskNode> resultList = new ArrayList<>();
    final TaskNode node = getTaskNodeById(taskId);
    if (node == null) {
      return resultList;
    }
    if (includeSelf) {
      resultList.add(node);
    }
    addDescendants(resultList, node);
    return resultList;
  }

  private void addDescendants(final List<TaskNode> resultList, final TaskNode node) {
    for (final TaskNode child : node.getChildren()) {
      resultList.add(child);
    }
  }

  /**
   * All task nodes are stored in an HashMap for faster searching.
   */
  public TaskNode getTaskNodeById(final Long id) {
    if (id == null) {
      return null;
    }
    checkRefresh();
    if (taskMap != null) {
      return taskMap.get(id);
    }
    return null;
  }

  public TaskDO getTaskById(final Long id) {
    checkRefresh();
    final TaskNode node = getTaskNodeById(id);
    if (node != null) {
      return node.getTask();
    }
    return null;
  }

  /**
   * Gets the project, which is assigned to the task or if not found to the parent task or grand parent task etc.
   *
   * @param taskId
   * @return null, if now project is assigned to this task or ancestor tasks.
   */
  public ProjektDO getProjekt(final Long taskId) {
    if (taskId == null) {
      return null;
    }
    final TaskNode node = getTaskNodeById(taskId);
    if (node != null) {
      return node.getProjekt();
    } else {
      return null;
    }
  }

  public void internalSetProject(final Long taskId, final ProjektDO projekt) {
    final TaskNode node = getTaskNodeById(taskId);
    if (node == null) {
      throw new InternalErrorException("Could not found task with id " + taskId + " in internalSetProject");
    }
    node.projekt = projekt;
  }

  /**
   * recursive = true.
   *
   * @param taskId
   * @return
   * @see #getKost2List(Long, boolean)
   */
  public List<Kost2DO> getKost2List(final Long taskId) {
    final TaskNode node = getTaskNodeById(taskId);
    return getKost2List(node, true);
  }

  /**
   * Get the available and active Kost2DOs of the task, or if not available of the first found ancestor tasks if
   * available. Kost2 are defined over assigned projects and kost2s. If project or Kost2DO not assigned for a task, then
   * the project or task of the parent task will be assumed. If the parent task has no project or task the grand parent
   * task will be taken and so on (recursive until root task).
   *
   * @param taskId
   * @param recursive If true then search the ancestor task for cost definitions if current task haven't.
   * @return Available Kost2DOs or null, if no Kost2DO found.
   */
  public List<Kost2DO> getKost2List(final Long taskId, final boolean recursive) {
    final TaskNode node = getTaskNodeById(taskId);
    return getKost2List(node, recursive);
  }

  /**
   * @param projekt          If not initialized then the project is get from the data base.
   * @param task             Only needed for output if an entry (Kost2) of the blackWhiteList cannot be found.
   * @param blackWhiteList
   * @param kost2IsBlackList
   * @return
   */
  public List<Kost2DO> getKost2List(ProjektDO projekt, final TaskDO task, final String[] blackWhiteList,
                                    final boolean kost2IsBlackList) {
    final List<Kost2DO> kost2List = new ArrayList<>();
    final boolean wildcard = blackWhiteList != null && blackWhiteList.length == 1 && "*".equals(blackWhiteList[0]);
    if (projekt != null && !Hibernate.isPropertyInitialized(projekt, "kunde")) {
      projekt = projektDao.internalGetById(projekt.getId());
    }
    if (projekt != null) {
      final List<Kost2DO> list = kostCache.getActiveKost2(projekt.getNummernkreis(), projekt.getBereich(),
          projekt.getNummer());
      if (CollectionUtils.isNotEmpty(list)) {
        for (final Kost2DO kost2 : list) {
          if (wildcard) { // black-white-list is "*".
            if (kost2IsBlackList) {
              break; // Do not add any entry.
            } else {
              kost2List.add(kost2); // Add all entries.
            }
          } else if (blackWhiteList == null || blackWhiteList.length == 0) {
            // Add all (either black nor white entry is given):
            kost2List.add(kost2);
          } else {
            final String no = kost2.getFormattedNumber();
            boolean add = kost2IsBlackList; // false for white list and true for black list at default.
            for (final String item : blackWhiteList) {
              if (no.endsWith(item)) {
                if (kost2IsBlackList) {
                  // Black list entry matches, so do not add entry:
                  add = false;
                  break;
                } else {
                  // White list entry matches, so add entry:
                  add = true;
                  break;
                }
              }
            }
            if (add) {
              kost2List.add(kost2);
            }
          }
        }
      }
    } else if (!kost2IsBlackList && blackWhiteList != null) {
      // Add all given KoSt2DOs.
      boolean infoLogDone = false;
      for (final String item : blackWhiteList) {
        final Kost2DO kost2 = kostCache.getKost2(item);
        if (kost2 != null) {
          kost2List.add(kost2);
        } else if (!infoLogDone) {
          if (item.length() <= 2) {
            log.info("Given kost2 not found: '" + item + "' of white list '" + task.getKost2BlackWhiteList() + "'. Project not linked anymore to task? Task id=" + task.getId() + ", task=" + task);
          } else {
            log.info("Given kost2 not found: '" + item + "' of white list '" + task.getKost2BlackWhiteList() + "'. Specified at task with id " + task.getId() + ": " + task);
          }
          infoLogDone = true;
        }
      }
    }
    if (CollectionUtils.isNotEmpty(kost2List)) {
      Collections.sort(kost2List);
      return kost2List;
    } else {
      return null;
    }
  }

  private List<Kost2DO> getKost2List(final TaskNode node, final boolean recursive) {
    if (node == null) {
      return null;
    }
    final TaskDO task = node.getTask();
    final String[] blackWhiteList = task.getKost2BlackWhiteItems();
    final ProjektDO projekt = node.getProjekt(blackWhiteList != null); // If black-white-list is null then do not search for projekt of
    // ancestor tasks.
    final List<Kost2DO> list = getKost2List(projekt, task, blackWhiteList, task.getKost2IsBlackList());
    if (list != null) {
      return list;
    } else if (node.parent != null && recursive) {
      return getKost2List(node.parent, recursive);
    } else {
      return null;
    }
  }

  /**
   * Should be called after modification of a time sheet assigned to the given task id.
   *
   * @param taskId
   */
  public void resetTotalDuration(final Long taskId) {
    final TaskNode node = getTaskNodeById(taskId);
    if (node == null) {
      log.error("Task id '" + taskId + "' not found.");
      return;
    }
    node.totalDuration = -1;
  }

  /**
   * After changing a task this method will be called by TaskDao for updating the task and the task tree.
   *
   * @param task Updating the existing task in the taskTree. If not exist, a new task will be added.
   */
  TaskNode addOrUpdateTaskNode(final TaskDO task) {
    checkRefresh();
    Validate.notNull(task);
    Validate.notNull(task.getId());
    final TaskNode node = getTaskNodeById(task.getId());
    if (node == null) {
      return addTaskNode(task);
    }
    node.setTask(task);
    if (task.getParentTaskId() != null && !task.getParentTaskId().equals(node.getParent().getId())) {
      if (log.isDebugEnabled()) {
        log.debug("Task hierarchy was changed for task: " + task);
      }
      final TaskNode oldParent = node.getParent();
      Validate.notNull(oldParent);
      oldParent.removeChild(node);
      final TaskNode newParent = getTaskNodeById(task.getParentTaskId());
      node.setParent(newParent);
      newParent.addChild(node);
    }
    updateTimeOfLastModification();
    return node;
  }

  /**
   * Sets an explicit task group access for the given task (stored in the given groupTaskAccess). This method will be
   * called by AccessDao after inserting or updating GroupTaskAccess to the database.
   *
   * @see GroupTaskAccess
   */
  public void setGroupTaskAccess(final GroupTaskAccessDO groupTaskAccess) {
    checkRefresh();
    final Long taskId = groupTaskAccess.getTaskId();
    final TaskNode node = taskMap.get(taskId);
    node.setGroupTaskAccess(groupTaskAccess);
  }

  /**
   * Removes an explicit task group access for the given task (stored in the given groupTaskAccess). This method will be
   * called by AccessDao after deleting GroupTaskAccess from the database.
   *
   * @see GroupTaskAccess
   */
  public void removeGroupTaskAccess(final GroupTaskAccessDO groupTaskAccess) {
    checkRefresh();
    final Long taskId = groupTaskAccess.getTaskId();
    final TaskNode node = taskMap.get(taskId);
    node.removeGroupTaskAccess(groupTaskAccess.getGroupId());
  }

  public long getTimeOfLastModification() {
    return this.timeOfLastModification;
  }

  @Override
  public String toString() {
    if (root == null) {
      return "<empty/>";
    }
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement("root");
    this.root.addXMLElement(root);
    // Pretty print the document to System.out
    final StringWriter sw = new StringWriter();
    String result = "";
    final XMLWriter writer = new XMLWriter(sw, OutputFormat.createPrettyPrint());
    try {
      writer.write(document);
      result = sw.toString();
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
    } finally {
      try {
        writer.close();
      } catch (final IOException ex) {
        log.error("Error while closing xml writer: " + ex.getMessage(), ex);
      }
    }
    return result;
  }

  public TaskTree() {
    super(AbstractCache.TICKS_PER_HOUR);
  }


  /**
   * Has the current logged in user select access to the given task?
   *
   * @param node
   * @return
   */
  public boolean hasSelectAccess(final TaskNode node) {
    return taskDao.hasLoggedInUserSelectAccess(node.getTask(), false);
  }

  /**
   * @see #isRootNode(TaskDO)
   */
  public boolean isRootNode(final TaskNode node) {
    Validate.notNull(node);
    return isRootNode(node.getTask());
  }

  /**
   * @param node
   * @return true, if the given task has the same id as the task tree's root node, otherwise false;
   */
  public boolean isRootNode(final TaskDO task) {
    Validate.notNull(task);
    if (root == null && task.getParentTaskId() == null) {
      // First task, so it should be the root node.
      return true;
    }
    checkRefresh();
    if (task.getId() == null) {
      // Node has no id, so it can't be the root node.
      return false;
    }
    return root.getId().equals(task.getId());
  }

  /**
   * Should be called after manipulations of any order position if a task reference was changed. This method declares
   * the reference map as dirty, therefore before the next usage the map will be rebuild from the database.
   */
  public void refreshOrderPositionReferences() {
    synchronized (this) {
      this.orderPositionReferencesDirty = true;
    }
  }

  /**
   * Does any order position entry with a task reference exist?
   */
  public boolean hasOrderPositionsEntries() {
    checkRefresh();
    return (MapUtils.isNotEmpty(getOrderPositionEntries()));
  }

  private Map<Long, Set<AuftragsPositionVO>> getOrderPositionEntries() {
    synchronized (this) {
      if (this.orderPositionReferencesDirty) {
        this.orderPositionReferences = auftragDao.getTaskReferences();
        if (this.orderPositionReferences != null) {
          resetOrderPersonDays(this.root);
          for (final Map.Entry<Long, Set<AuftragsPositionVO>> entry : this.orderPositionReferences.entrySet()) {
            final TaskNode node = getTaskNodeById(entry.getKey());
            node.orderedPersonDays = null;
            if (CollectionUtils.isNotEmpty(entry.getValue())) {
              for (final AuftragsPositionVO pos : entry.getValue()) {
                if (pos.getPersonDays() == null) {
                  continue;
                }
                if (node.orderedPersonDays == null) {
                  node.orderedPersonDays = BigDecimal.ZERO;
                }
                node.orderedPersonDays = node.orderedPersonDays.add(pos.getPersonDays());
              }
            }
          }
        }
        this.orderPositionReferencesDirty = false;
      }
      return this.orderPositionReferences;
    }
  }

  private void resetOrderPersonDays(final TaskNode node) {
    node.orderedPersonDays = null;
    if (node.hasChildren()) {
      for (final TaskNode child : node.getChildren()) {
        resetOrderPersonDays(child);
      }
    }
  }

  /**
   * @param taskId
   * @return Set of all order positions assigned to the given task.
   */
  public Set<AuftragsPositionVO> getOrderPositionEntries(final Long taskId) {
    checkRefresh();
    return getOrderPositionEntries().get(taskId);
  }

  /**
   * @param taskId
   * @return
   */
  public Set<AuftragsPositionVO> getOrderPositionsUpwards(final Long taskId) {
    final Set<AuftragsPositionVO> set = new TreeSet<>();
    addOrderPositionsUpwards(set, taskId);
    return set;
  }

  private void addOrderPositionsUpwards(final Set<AuftragsPositionVO> set, final Long taskId) {
    final Set<AuftragsPositionVO> set2 = getOrderPositionEntries(taskId);
    if (CollectionUtils.isNotEmpty(set2)) {
      set.addAll(set2);
    }
    final TaskDO task = getTaskById(taskId);
    if (task != null && task.getParentTaskId() != null) {
      addOrderPositionsUpwards(set, task.getParentTaskId());
    }
  }

  /**
   * @param taskId
   * @param recursive if true also all descendant tasks will be searched for assigned order positions.
   * @return
   */
  public boolean hasOrderPositions(final Long taskId, final boolean recursive) {
    if (taskId == null) { // For new tasks.
      return false;
    }
    if (CollectionUtils.isNotEmpty(getOrderPositionEntries(taskId))) {
      return true;
    }
    if (recursive) {
      final TaskNode node = getTaskNodeById(taskId);
      if (node != null && node.hasChildren()) {
        for (final TaskNode child : node.getChildren()) {
          if (hasOrderPositions(child.getId(), recursive)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * @param taskId
   * @return True, if the given task has order positions or any ancestor task has an order position.
   */
  public boolean hasOrderPositionsUpwards(final Long taskId) {
    if (hasOrderPositions(taskId, false)) {
      return true;
    }
    final TaskNode task = getTaskNodeById(taskId);
    if (task != null && task.getParentId() != null) {
      return hasOrderPositionsUpwards(task.getParentId());
    }
    return false;
  }

  /**
   * @param taskId
   * @see #getPersonDays(TaskNode)
   */
  public BigDecimal getPersonDays(final Long taskId) {
    final TaskNode node = getTaskNodeById(taskId);
    return getPersonDays(node);
  }

  /**
   * @param node
   * @return The ordered person days or if not found the defined max hours. If both not found, the get the sum of all
   * diect or null if both not found.
   */
  public BigDecimal getPersonDays(final TaskNode node) {
    checkRefresh();
    if (node == null || node.isDeleted()) {
      return null;
    }
    if (hasOrderPositions(node.getId(), true)) {
      return getOrderedPersonDaysSum(node);
    }
    final Integer maxHours = node.getTask().getMaxHours();
    if (maxHours != null) {
      return new BigDecimal(maxHours).divide(DateHelper.HOURS_PER_WORKING_DAY, 2, BigDecimal.ROUND_HALF_UP);
    }
    if (!node.hasChildren()) {
      return null;
    }
    BigDecimal result = null;
    for (final TaskNode child : node.getChildren()) {
      final BigDecimal childPersonDays = getPersonDays(child);
      if (childPersonDays != null) {
        if (result == null) {
          result = BigDecimal.ZERO;
        }
        result = result.add(childPersonDays);
      }
    }
    return result;
  }

  /**
   * @return The sum of all ordered person days. This method checks the given node and all sub-nodes for assigned order
   * positions.
   */
  public BigDecimal getOrderedPersonDaysSum(final TaskNode node) {
    BigDecimal personDays = null;
    if (node.orderedPersonDays != null) {
      personDays = node.orderedPersonDays;
    }
    if (node.hasChildren()) {
      for (final TaskNode child : node.getChildren()) {
        final BigDecimal childPersonDays = getOrderedPersonDaysSum(child);
        if (childPersonDays != null) {
          if (personDays == null) {
            personDays = childPersonDays;
          } else {
            personDays = personDays.add(childPersonDays);
          }
        }
      }
    }
    return personDays;
  }

  public TaskNode getPersonDaysNode(final TaskNode node) {
    if (node == null) {
      return null;
    }
    if (node.orderedPersonDays != null) {
      return node;
    }
    if (NumberHelper.greaterZero(node.getTask().getMaxHours())) {
      return node;
    }
    return getPersonDaysNode(node.getParent());
  }

  /**
   * Reads the sum of all time sheet durations grouped by task id and set the total duration of found taskNodes.
   */
  private void readTotalDurations() {
    final List<Object[]> list = taskDao.readTotalDurations();
    for (final Object[] res : list) {
      final Long taskId = (Long) res[1];
      final TaskNode node = getTaskNodeById(taskId);
      if (node == null) {
        log.warn("Task not found: " + taskId);
      } else {
        if (res[0] instanceof Integer) {
          node.totalDuration = (Integer) res[0];
        } else {
          node.totalDuration = (Long) res[0];
        }
      }
    }
  }

  /**
   * Reads the sum of all time sheet durations grouped by task id and set the total duration of found taskNodes.
   */
  public void readTotalDuration(final Long taskId) {
    final long duration = taskDao.readTotalDuration(taskId);
    final TaskNode node = getTaskNodeById(taskId);
    if (node == null) {
      log.warn("Task not found: " + taskId);
    } else {
      node.totalDuration = duration;
    }
  }

  /**
   * Should only called by test suite!
   */
  public void clear() {
    this.root = null;
    this.setExpired();
  }

  /**
   * All tasks from database will be read and cached into this TaskTree. Also all explicit group task access' will be
   * read from database and will be cached in this tree (implicit access' will be created too).<br/>
   * The generation of the task tree will be done manually, not by hibernate because the task hierarchy is very
   * sensible. Manipulations of the task tree should be done carefully for single task nodes.
   *
   * @see org.projectforge.framework.cache.AbstractCache#refresh()
   */
  @Override
  protected void refresh() {
    log.info("Initializing task tree ...");
    if (taskDao == null) {
      log.info("Can't initialize task tree, taskDao isn't set yet (shouldn't occur):");
      // Stack trace for debugging refresh() call without TaskDao (does only occur in productive mode):
      final StackTraceHolder sth = new StackTraceHolder();
      log.info(sth.toString());
      return;
    }
    TaskNode newRoot = null;
    taskMap = new HashMap<>();
    final List<TaskDO> taskList;
    taskList = taskDao.internalLoadAll();
    TaskNode node;
    log.debug("Loading list of tasks ...");
    for (final TaskDO task : taskList) {
      node = new TaskNode();
      node.setTask(task);
      taskMap.put(node.getTaskId(), node);
      if (node.isRootNode()) {
        if (newRoot != null) {
          log.error("Duplicate root node found: " + newRoot.getId() + " and " + node.getId());
          node.setParent(newRoot); // Set the second root task as child task of first read root task.
        } else {
          if (log.isDebugEnabled()) {
            log.debug("Root note found: " + node);
          }
          newRoot = node;
        }
      }
    }

    if (newRoot == null) {
      final TaskDO rootTask = new TaskDO();
      rootTask.setShortDescription("ProjectForge root task");
      rootTask.setTitle("root");
      taskDao.internalSaveNewTrans(rootTask);
      newRoot = new TaskNode();
      newRoot.setTask(rootTask);
      taskMap.put(newRoot.getTaskId(), newRoot);
    }
    this.root = newRoot;
    if (log.isDebugEnabled()) {
      log.debug("Creating tree for " + taskList.size() + " tasks ...");
    }
    for (final TaskDO task : taskList) {
      TaskNode parentNode = null;
      node = taskMap.get(task.getId());
      final Long parentId = task.getParentTaskId();
      if (parentId != null) {
        parentNode = taskMap.get(parentId);
      }
      // log.debug("Processing node: " + node.getId() + ", parent: " + parentId);
      if (parentNode != null) {
        node.setParent(parentNode);
        parentNode.addChild(node);
        updateTimeOfLastModification();
      } else {
        log.debug("Processing root node:" + node);
      }
    }

    if (log.isDebugEnabled()) {
      log.debug(this.root.toString());
    }

    // Now read all explicit group task access' from the database:
    final List<GroupTaskAccessDO> accessList = accessDao.internalLoadAll();
    for (final GroupTaskAccessDO access : accessList) {
      node = taskMap.get(access.getTaskId());
      node.setGroupTaskAccess(access);
      if (log.isDebugEnabled()) {
        log.debug(access.toString());
      }
    }
    // Now read all projects with their references to tasks:
    final List<ProjektDO> projects = projektDao.internalLoadAll();
    if (projects != null) {
      for (final ProjektDO project : projects) {
        if (project.getDeleted() || project.getTaskId() == null) {
          continue;
        }
        node = taskMap.get(project.getTaskId());
        if (node == null) {
          log.error("Oups, should not occur: project references a non existing task: " + project);
        } else {
          node.projekt = project;
        }
      }
    }
    if (log.isDebugEnabled()) {
      log.debug(this.toString());
    }
    readTotalDurations();
    refreshOrderPositionReferences();
    // Now update the status: bookable for time sheets:
    final TimesheetDO timesheet = new TimesheetDO();
    for (final TaskDO task : taskList) {
      node = taskMap.get(task.getId());
      timesheet.setTask(task);
      final boolean bookable = timesheetDao.checkTaskBookable(timesheet, null, OperationType.INSERT, false);
      node.bookableForTimesheets = bookable;
    }
    log.info("Initializing task tree done.");
  }

  private void updateTimeOfLastModification() {
    this.timeOfLastModification = new Date().getTime();
  }
}
