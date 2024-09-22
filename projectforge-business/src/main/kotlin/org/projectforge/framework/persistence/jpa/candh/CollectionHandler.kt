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

package org.projectforge.framework.persistence.jpa.candh

import jakarta.persistence.JoinColumn
import mu.KotlinLogging
import org.hibernate.collection.spi.PersistentSet
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.projectforge.common.AnnotationsUtils
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.jpa.candh.CandHMaster.copyValues
import org.projectforge.framework.persistence.jpa.candh.CandHMaster.setModificationStatusOnChange
import java.io.Serializable
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

private val log = KotlinLogging.logger {}

/**
 * Used for mutable collections. TreeSet, ArrayList, HashSet and PersistentSet are supported.
 */
open class CollectionHandler : CandHIHandler {
    override fun accept(property: KMutableProperty1<*, *>): Boolean {
        return property.returnType.jvmErasure.isSubclassOf(Collection::class)
    }

    override fun process(
        propertyContext: PropertyContext<*>,
        context: CandHContext,
    ): Boolean {
        propertyContext.apply {
            @Suppress("UNCHECKED_CAST")
            property as KMutableProperty1<BaseDO<*>, Any?>
            @Suppress("UNCHECKED_CAST")
            val srcCollection = srcPropertyValue as? MutableCollection<Any?>

            @Suppress("UNCHECKED_CAST")
            var destCollection = destPropertyValue as? MutableCollection<Any?>
            if (srcCollection.isNullOrEmpty() && destCollection.isNullOrEmpty()) {
                // Both collections are null or empty, so nothing to do.
                return true
            }
            if (srcCollection.isNullOrEmpty()) {
                property.set(dest, null)
                context.debugContext?.add(
                    "$kClass.$propertyName",
                    srcVal = srcPropertyValue,
                    destVal = "<not empty collection>"
                )
                if (collectionManagedBySrcClazz(kClass, propertyName)) {
                    context.addHistoryEntry(propertyName, "", "collection removed")
                } else {
                    context.addHistoryEntry(propertyName, "", "collection removed")
                }
                setModificationStatusOnChange(context, propertyContext)
                return true
            }
            val toRemove = mutableListOf<Any>()
            if (destCollection == null) {
                if (srcPropertyValue is TreeSet<*>) {
                    destCollection = TreeSet()
                    context.debugContext?.add(
                        "$kClass.$propertyName",
                        srcVal = srcPropertyValue,
                        msg = "Creating TreeSet as destPropertyValue.",
                    )
                } else if (srcPropertyValue is HashSet<*>) {
                    destCollection = HashSet()
                    context.debugContext?.add(
                        "$kClass.$propertyName",
                        srcVal = srcPropertyValue,
                        msg = "Creating HashSet as destPropertyValue.",
                    )
                } else if (srcPropertyValue is List<*>) {
                    destCollection = ArrayList()
                    context.debugContext?.add(
                        "$kClass.$propertyName",
                        srcVal = srcPropertyValue,
                        msg = "Creating ArrayList as destPropertyValue.",
                    )
                } else if (srcPropertyValue is PersistentSet<*>) {
                    destCollection = HashSet()
                    context.debugContext?.add(
                        "$kClass.$propertyName",
                        srcVal = srcPropertyValue,
                        msg = "Creating HashSet as destPropertyValue. srcPropertyValue is PersistentSet.",
                    )
                } else {
                    log.error("Unsupported collection type: " + srcPropertyValue?.javaClass?.name)
                    return true
                }
                property.set(dest, destCollection)
            }
            destCollection.filterNotNull().forEach { destColEntry ->
                if (srcCollection.none { it == destColEntry }) {
                    toRemove.add(destColEntry)
                }
            }
            toRemove.forEach { removeEntry ->
                log.debug { "Removing collection entry: $removeEntry" }
                destCollection.remove(removeEntry)
                context.debugContext?.add(
                    "$kClass.$propertyName",
                    msg = "Removing entry $removeEntry from destPropertyValue.",
                )
                setModificationStatusOnChange(context, propertyContext)
            }
            srcCollection.forEach { srcCollEntry ->
                if (!destCollection.contains(srcCollEntry)) {
                    log.debug { "Adding new collection entry: $srcCollEntry" }
                    destCollection.add(srcCollEntry)
                    context.debugContext?.add(
                        "$kClass.$propertyName",
                        msg = "Adding entry $srcCollEntry to destPropertyValue.",
                    )
                    setModificationStatusOnChange(context, propertyContext)
                } else if (srcCollEntry is BaseDO<*>) {
                    val behavior = AnnotationsUtils.getAnnotation(property, PFPersistancyBehavior::class.java)
                    context.debugContext?.add(
                        "$kClass.$propertyName",
                        msg = "srcEntry of src-collection is BaseDO. autoUpdateCollectionEntres = ${behavior?.autoUpdateCollectionEntries == true}"
                    )
                    if (behavior != null && behavior.autoUpdateCollectionEntries) {
                        var destEntry: BaseDO<*>? = null
                        for (entry in destCollection) {
                            if (entry == srcCollEntry) {
                                destEntry = entry as BaseDO<*>
                                break
                            }
                        }
                        requireNotNull(destEntry)
                        @Suppress("UNCHECKED_CAST")
                        copyValues(
                            srcCollEntry as BaseDO<Serializable>,
                            destEntry as BaseDO<Serializable>,
                            context
                        )
                    }
                }
            }
        }
        return true
    }

    /**
     * If collection is declared as OneToMany and not marked as @NoHistory, the collection is managed by the source class.
     */
    private fun collectionManagedBySrcClazz(srcClass: KClass<*>, propertyName: String): Boolean {
        val annotations = AnnotationsUtils.getAnnotations(srcClass, propertyName)
        if (annotations.any { it.annotationClass == JoinColumn::class } &&
            annotations.none { it.annotationClass == NoHistory::class }) {
            return true
        }
        return false
    }
}
