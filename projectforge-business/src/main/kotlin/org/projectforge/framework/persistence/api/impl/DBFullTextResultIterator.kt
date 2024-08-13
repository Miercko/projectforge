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

package org.projectforge.framework.persistence.api.impl

import jakarta.persistence.EntityManager
import org.apache.commons.lang3.builder.CompareToBuilder
import org.apache.jackrabbit.oak.query.QueryParser
import org.hibernate.search.mapper.orm.Search
import org.projectforge.common.BeanHelper
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory
import java.text.Collator

private const val MAX_RESULTS = 100

internal class DBFullTextResultIterator<O : ExtendedBaseDO<Int>>(
        val baseDao: BaseDao<O>,
        private val entityManager: EntityManager,
        private val resultMatchers: List<DBPredicate>,
        private val filter: QueryFilter,
        val sortProperties: Array<SortProperty>,
        val fullTextQuery: org.apache.lucene.search.Query? = null, // Full text query
        val multiFieldQuery: List<String>? = null)     // MultiField query
    : DBResultIterator<O> {
    private val log = LoggerFactory.getLogger(DBFullTextResultIterator::class.java)
    private var result: List<O>
    private var resultIndex = -1
    private var firstIndex = 0
    private val searchClassInfo = HibernateSearchMeta.getClassInfo(baseDao)
    private val searchFields: Array<String>

    init {
        if (log.isDebugEnabled && !multiFieldQuery.isNullOrEmpty()) {
            val queryString = multiFieldQuery.joinToString(" ")
            log.debug("Using multifieldQuery (${baseDao.doClass.simpleName}): $queryString")
        }
        val fullTextSearchFields = filter.fullTextSearchFields
        searchFields = if (fullTextSearchFields.isNullOrEmpty()) searchClassInfo.stringFieldNames else fullTextSearchFields
        log.debug("Using search fields: ${searchFields.joinToString(", ")}")
        result = nextResultBlock()
    }

    override fun next(): O? {
        while (true) {
            val next = internalNext() ?: return null
            if (!resultMatchers.isNullOrEmpty()) {
                var matches = true
                for (matcher in resultMatchers) {
                    if (!matcher.match(next)) {
                        matches = false // No match.
                        break
                    }
                }
                if (!matches)
                    continue
                return next
            }
            return next
        }
    }

    override fun sort(list: List<O>): List<O> {
        val collator = Collator.getInstance(ThreadLocalUserContext.locale)
        val errorProperties = mutableListOf<String>()
        return list.sortedWith(object : Comparator<O> {
            override fun compare(o1: O, o2: O): Int {
                if (sortProperties.isNullOrEmpty()) {
                    return 0
                }
                val ctb = CompareToBuilder()
                for (sortProperty in sortProperties) {
                    try {
                        val val1 = BeanHelper.getNestedProperty(o1, sortProperty.property)
                        val val2 = BeanHelper.getNestedProperty(o2, sortProperty.property)
                        if (val1 is String) {
                            // Strings should be compared by using locale dependent collator (especially for german Umlaute)
                            if (sortProperty.ascending) {
                                ctb.append(val1, val2, collator)
                            } else {
                                ctb.append(val2, val1, collator)
                            }
                        } else if (val1 is Comparable<*>) {
                            if (sortProperty.ascending) {
                                ctb.append(val1, val2)
                            } else {
                                ctb.append(val2, val1)
                            }
                        } else {
                            if (sortProperty.ascending) {
                                ctb.append(val1?.toString(), val2?.toString())
                            } else {
                                ctb.append(val2?.toString(), val1?.toString())
                            }
                        }
                    } catch (ex: Exception) {
                        if (!errorProperties.contains(ex.message)) {
                            errorProperties.add("${ex.message}")
                            log.warn("Ignore sort property (OK): ${ex.message}")
                        }
                    }
                }
                return ctb.toComparison()
            }
        })
    }

    private fun internalNext(): O? {
        if (result.isEmpty()) {
            return null
        }
        if (++resultIndex >= result.size) {
            if (result.size < MAX_RESULTS)
                return null
            result = nextResultBlock()
            if (result.isEmpty()) {
                return null
            }
            resultIndex = 0
        }
        return result[resultIndex]
    }

    private fun nextResultBlock(): List<O> {
        val searchSession = Search.session(entityManager)
        val fullTextQuery = if (fullTextQuery != null) {
           /* val results: List<MyEntity> = searchSession.search(MyEntity::class.java)
                .where { f ->
                    f.match()
                        .fields("fieldName") // Name des indizierten Feldes
                        .matching("searchTerm")
                } // Der Suchbegriff
                .fetchHits(20) // Erhalte die ersten 20 Ergebnisse*/
            fullTextEntityManager.createFullTextQuery(fullTextQuery, baseDao.doClass)
        } else {
            val queryString = multiFieldQuery?.joinToString(" ") ?: ""
            val parser = MultiFieldQueryParser(searchFields, ClassicAnalyzer())
            parser.defaultOperator = QueryParser.Operator.AND
            parser.allowLeadingWildcard = true
            var query: org.apache.lucene.search.Query? = null
            try {
                query = parser.parse(queryString)
            } catch (ex: org.apache.lucene.queryparser.classic.ParseException) {
                val errorMsg = ("Lucene error message: '${ex.message}'  (for ${baseDao.doClass.getSimpleName()}: '$queryString').")
                // TODO feedback
                log.error(errorMsg)
            }
            fullTextEntityManager.createFullTextQuery(query, baseDao.doClass)
        }
        fullTextQuery.firstResult = firstIndex
        fullTextQuery.maxResults = MAX_RESULTS

        firstIndex += MAX_RESULTS
        @Suppress("UNCHECKED_CAST")
        return fullTextQuery.resultList as List<O> // return a list of managed objects
    }
}
