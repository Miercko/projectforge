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

package org.projectforge.business.address

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.projectforge.business.image.ImageService
import org.projectforge.framework.persistence.api.impl.EntityManagerUtil
import org.projectforge.framework.persistence.utils.SQLHelper.ensureUniqueResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class AddressImageDao {
    @Autowired
    private lateinit var addressDao: AddressDao

    @PersistenceContext
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var emgrFactory: EntityManagerFactory

    @Autowired
    private lateinit var imageService: ImageService

    /**
     * Does the access checking. The user may only get images, if he has the select access to the given address.
     */
    open fun getImage(addressId: Int): ByteArray? {
        addressDao.getById(addressId) ?: return null // For access checking!
        return ensureUniqueResult(
            em.createNamedQuery(AddressImageDO.SELECT_IMAGE, ByteArray::class.java)
                .setParameter("addressId", addressId)
        )
    }

    /**
     * Does the access checking. The user may only get images, if he has the select access to the given address.
     */
    open fun getPreviewImage(addressId: Int): ByteArray? {
        addressDao.getById(addressId) ?: return null // For access checking!
        return ensureUniqueResult(
            em.createNamedQuery(AddressImageDO.SELECT_IMAGE_PREVIEW, ByteArray::class.java)
                .setParameter("addressId", addressId)
        )
    }

    /**
     * Does the access checking. The user may only modify images, if he has the access to modify the given address.
     */
    open fun saveOrUpdate(addressId: Int, image: ByteArray): Boolean {
        val address = addressDao.getById(addressId)
        if (address == null) {
            log.error("Can't save or update immage of address. Address #$addressId not found.")
            return false
        }
        addressDao.internalModifyImageData(address, true)
        addressDao.update(address) // Throws an exception if the logged-in user has now access.
        val addressImage = get(address.id) ?: AddressImageDO()
        addressImage.address = address
        addressImage.image = image
        addressImage.imagePreview = imageService.resizeImage(image)
        em.transaction.begin()
        EntityManagerUtil.runInTransaction(emgrFactory) {
            if (addressImage.id != null) {
                // Update
                em.merge(addressImage)
            } else {
                // Insert
                em.persist(addressImage)
            }
        }
        log.info("New image for address ${address.id} (${address.fullName}) saved.")
        return true
    }

    /**
     * Does the access checking. The user may only delete images, if he has the access to modify the given address.
     */
    open fun delete(addressId: Int): Boolean {
        val address = addressDao.getById(addressId)
        if (address == null) {
            log.error("Can't save or update immage of address. Address #$addressId not found.")
            return false
        }
        addressDao.internalModifyImageData(address, false)
        addressDao.update(address) // Throws an exception if the logged-in user has now access.
        return EntityManagerUtil.runInTransaction(emgrFactory) {
            val addressImage = em.find(AddressImageDO::class.java, address.id)
            if (addressImage != null) {
                em.remove(addressImage)
                log.info("Image for address ${address.id} (${address.fullName}) deleted.")
                true
            } else {
                false
            }
        }
    }

    private fun get(addressId: Int?): AddressImageDO? {
        val address = addressDao.getById(addressId) ?: return null // For access checking!
        if (address == null) {
            log.error("Can't save or update immage of address. Address #$addressId not found.")
            return null
        }
        try {
            return ensureUniqueResult(
                em.createQuery(
                    "from ${AddressImageDO::class.java.name} t where t.address = :address", AddressImageDO::class.java
                )
                    .setParameter("address", address)
            )
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            return null
        }
    }
}
