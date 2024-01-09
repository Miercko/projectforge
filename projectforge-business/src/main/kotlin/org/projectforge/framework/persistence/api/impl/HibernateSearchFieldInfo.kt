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

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.search.annotations.*
import org.hibernate.search.bridge.builtin.NumberBridge
import kotlin.reflect.full.isSubclassOf

class HibernateSearchFieldInfo(val javaProp: String, val type: Class<*>) {
    @JsonIgnore
    private var annotations: MutableList<Annotation>? = null
    var idProperty: Boolean = false
        internal set
    @JsonIgnore
    var dateBridgeAnn: DateBridge? = null
        internal set
    @JsonIgnore
    var fieldBridgeAnn: FieldBridge? = null
        internal set(value) {
            field = value
            if (value?.impl?.isSubclassOf(NumberBridge::class) == true) {
                numberBridge = true
            }
        }
    var numberBridge: Boolean = false
        internal set
    var luceneField: String = javaProp
        internal set

    fun getDateBridgeEncodingType(): EncodingType? {
        return dateBridgeAnn?.encoding
    }

    fun add(annotation: Annotation?) {
        if (annotation == null) {
            return
        }
        if (annotations == null) {
            annotations = mutableListOf()
        }
        annotations!!.add(annotation)
        if (annotation is Field && annotation.name.isNotBlank()) {
            luceneField = annotation.name
        }
    }

    fun hasAnnotations(): Boolean {
        return !annotations.isNullOrEmpty()
    }

    /**
     * If field is of type string or field is a class bridge.
     */
    fun isStringSearchSupported(): Boolean {
        return String::class.java.isAssignableFrom(type)
                || isClassBridge()
    }

    /**
     * @return true if field is an Int/Integer field or idProperty and has no FieldBridge with NumberBridge impl.
     */
    fun isNumericSearchSupported(): Boolean {
        return !numberBridge &&
                (Integer::class.java.isAssignableFrom(type)
                        || Int::class.java.isAssignableFrom(type)
                        || idProperty)
    }

    fun isClassBridge(): Boolean {
        return ClassBridge::class.java.isAssignableFrom(type)
    }
}
