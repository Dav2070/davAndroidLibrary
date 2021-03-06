package app.dav.davandroidlibrary

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.dav.davandroidlibrary.data.DavDatabase
import app.dav.davandroidlibrary.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DavInstrumentedTest{
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val database = DavDatabase.getInstance(context)

    init {
        Dav.init(context)

        // Drop the database
        database.tableObjectDao().deleteAllTableObjects()
    }

    // createTableObject tests
    @Test
    fun createTableObjectShouldSaveTheTableObjectInTheDatabaseAndReturnTheId(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 4
        val tableObject = TableObject(uuid, tableId)

        // Act
        tableObject.id = runBlocking {
            Dav.Database.createTableObject(tableObject)
        }

        // Assert
        Assert.assertNotEquals(0, tableObject.id)

        // Get the table object from the database
        val tableObjectFromDatabase = database.tableObjectDao().getTableObject(tableObject.id)
        Assert.assertEquals(tableId, tableObjectFromDatabase.tableId)
        Assert.assertEquals(tableObject.id, tableObjectFromDatabase.id)
        Assert.assertEquals(uuid, UUID.fromString(tableObjectFromDatabase.uuid))
        Assert.assertFalse(tableObjectFromDatabase.isFile)
    }
    // End createTableObject tests

    // CreateTableObjectWithProperties tests
    @Test
    fun createTableObjectWithPropertiesShouldSaveTheTableObjectAndItsPropertiesInTheDatabase(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 4
        val firstPropertyName = "page1"
        val secondPropertyName = "page2"
        val firstPropertyValue = "Hello World"
        val secondPropertyValue = "Hallo Welt"
        val propertiesArray = arrayListOf<Property>(
                Property(0, firstPropertyName, firstPropertyValue),
                Property(0, secondPropertyName, secondPropertyValue))

        val tableObject = TableObject(uuid, tableId, propertiesArray)

        // Act
        tableObject.id = runBlocking {
            Dav.Database.createTableObjectWithProperties(tableObject)
        }

        // Assert
        val tableObjectFromDatabase = database.tableObjectDao().getTableObject(tableObject.id)
        Assert.assertEquals(tableId, tableObjectFromDatabase.tableId)
        Assert.assertEquals(tableObject.id, tableObjectFromDatabase.id)
        Assert.assertEquals(uuid, UUID.fromString(tableObjectFromDatabase.uuid))

        val firstPropertyFromDatabase = database.propertyDao().getProperty(tableObject.properties[0].id)
        Assert.assertEquals(tableObjectFromDatabase.id, firstPropertyFromDatabase.tableObjectId)
        Assert.assertEquals(firstPropertyName, firstPropertyFromDatabase.name)
        Assert.assertEquals(firstPropertyValue, firstPropertyFromDatabase.value)

        val secondPropertyFromDatabase = database.propertyDao().getProperty(tableObject.properties[1].id)
        Assert.assertEquals(tableObjectFromDatabase.id, secondPropertyFromDatabase.tableObjectId)
        Assert.assertEquals(secondPropertyName, secondPropertyFromDatabase.name)
        Assert.assertEquals(secondPropertyValue, secondPropertyFromDatabase.value)
    }
    // End createTableObjectWithProperties tests

    // GetTableObject tests
    @Test
    fun getTableObjectShouldReturnTheTableObject(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 4
        val tableObject = TableObject(uuid, tableId)
        tableObject.id = runBlocking {
            Dav.Database.createTableObject(tableObject)
        }

        // Act
        val tableObjectFromDatabase = runBlocking {
             Dav.Database.getTableObject(uuid)
        }

        // Assert
        Assert.assertNotNull(tableObjectFromDatabase)
        Assert.assertEquals(tableObject.id, tableObjectFromDatabase?.id)
        Assert.assertEquals(tableObject.tableId, tableObjectFromDatabase?.tableId)
        Assert.assertEquals(tableObject.uuid, tableObjectFromDatabase?.uuid)
        Assert.assertEquals(tableObject.uploadStatus, tableObjectFromDatabase?.uploadStatus)
    }

    @Test
    fun getTableObjectShouldReturnNullWhenTheTableObjectDoesNotExist(){
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val tableObject = runBlocking {
            Dav.Database.getTableObject(uuid)
        }

        // Assert
        Assert.assertNull(tableObject)
    }
    // End getTableObject tests

    // getAllTableObjects(deleted: Boolean) tests
    @Test
    fun getAllTableObjectsShouldReturnAllTableObjects(){
        // Arrange
        val firstTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 12)
        }
        val secondTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 12)
        }
        val thirdTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 13)
        }
        runBlocking { thirdTableObject.saveUploadStatus(TableObjectUploadStatus.Deleted) }

        // Act
        val allTableObjects = runBlocking {
            Dav.Database.getAllTableObjects(true)
        }

        // Assert
        Assert.assertEquals(3, allTableObjects.size)

        Assert.assertEquals(firstTableObject.uuid, allTableObjects[0].uuid)
        Assert.assertEquals(firstTableObject.tableId, allTableObjects[0].tableId)

        Assert.assertEquals(secondTableObject.uuid, allTableObjects[1].uuid)
        Assert.assertEquals(secondTableObject.tableId, allTableObjects[1].tableId)

        Assert.assertEquals(thirdTableObject.uuid, allTableObjects[2].uuid)
        Assert.assertEquals(thirdTableObject.tableId, allTableObjects[2].tableId)
    }

    @Test
    fun getAllTableObjectsShouldReturnAllTableObjectsExceptDeletedOnes(){
        // Arrange
        val firstTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 12)
        }
        val secondTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 12)
        }
        val thirdTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 13)
        }
        runBlocking { thirdTableObject.saveUploadStatus(TableObjectUploadStatus.Deleted) }

        // Act
        val allTableObjects = runBlocking {
            Dav.Database.getAllTableObjects(false)
        }

        // Assert
        Assert.assertEquals(2, allTableObjects.size)

        Assert.assertEquals(firstTableObject.uuid, allTableObjects[0].uuid)
        Assert.assertEquals(firstTableObject.tableId, allTableObjects[0].tableId)

        Assert.assertEquals(secondTableObject.uuid, allTableObjects[1].uuid)
        Assert.assertEquals(secondTableObject.tableId, allTableObjects[1].tableId)
    }
    // End getAllTableObjects(deleted: Boolean) tests

    // getAllTableObjects(tableId: Int, deleted: Boolean) tests
    @Test
    fun getAllTableObjectsWithTableIdShouldReturnAllTableObjectsOfTheTable(){
        // Arrange
        val tableId = 12
        val firstTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), tableId)
        }
        val secondTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), tableId)
        }
        val thirdTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 3)
        }
        val fourthTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), tableId)
        }
        runBlocking { fourthTableObject.saveUploadStatus(TableObjectUploadStatus.Deleted) }

        // Act
        val allTableObjects = runBlocking {
            Dav.Database.getAllTableObjects(tableId, true)
        }

        // Assert
        Assert.assertEquals(3, allTableObjects.size)

        Assert.assertEquals(firstTableObject.uuid, allTableObjects[0].uuid)
        Assert.assertEquals(firstTableObject.tableId, allTableObjects[0].tableId)

        Assert.assertEquals(secondTableObject.uuid, allTableObjects[1].uuid)
        Assert.assertEquals(secondTableObject.tableId, allTableObjects[1].tableId)

        Assert.assertEquals(fourthTableObject.uuid, allTableObjects[2].uuid)
        Assert.assertEquals(fourthTableObject.tableId, allTableObjects[2].tableId)
    }

    @Test
    fun getAllTableObjectsWithTableIdShouldReturnAlltableObjectsOfTheTableExceptDeletedOnes(){
        // Arrange
        val tableId = 12
        val firstTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), tableId)
        }
        val secondTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), tableId)
        }
        val thirdTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), 3)
        }
        val fourthTableObject = runBlocking {
            TableObject.create(UUID.randomUUID(), tableId)
        }
        runBlocking { fourthTableObject.saveUploadStatus(TableObjectUploadStatus.Deleted) }

        // Act
        val allTableObjects = runBlocking {
            Dav.Database.getAllTableObjects(tableId, false)
        }

        // Assert
        Assert.assertEquals(2, allTableObjects.size)

        Assert.assertEquals(firstTableObject.uuid, allTableObjects[0].uuid)
        Assert.assertEquals(firstTableObject.tableId, allTableObjects[0].tableId)

        Assert.assertEquals(secondTableObject.uuid, allTableObjects[1].uuid)
        Assert.assertEquals(secondTableObject.tableId, allTableObjects[1].tableId)
    }
    // End getAllTableObjects(tableId: Int, deleted: Boolean) tests

    // updateTableObject tests
    @Test
    fun updateTableObjectShouldUpdateTheTableObjectInTheDatabase(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 5
        val oldVisibilityInt = 1
        val newVisibilityInt = 2
        val oldUploadStatusInt = 0
        val newUploadStatusInt = 1
        val oldEtag = "oldetag"
        val newEtag = "newetag"

        val oldTableObjectEntity = TableObjectEntity(tableId, uuid.toString(), oldVisibilityInt, oldUploadStatusInt, false, oldEtag)
        oldTableObjectEntity.id = database.tableObjectDao().insertTableObject(oldTableObjectEntity)

        // Create a second table object with the same id and uuid but different values and replace the old table object with this one
        val newTableObjectEntity = TableObjectEntity(tableId, uuid.toString(), newVisibilityInt, newUploadStatusInt, false, newEtag)
        newTableObjectEntity.id = oldTableObjectEntity.id
        val newTableObject = TableObject.convertTableObjectEntityToTableObject(newTableObjectEntity)

        // Act
        runBlocking { Dav.Database.updateTableObject(newTableObject) }

        // Assert
        val tableObjectFromDatabase = database.tableObjectDao().getTableObject(newTableObject.id)
        Assert.assertEquals(newTableObject.id, tableObjectFromDatabase.id)
        Assert.assertEquals(tableId, tableObjectFromDatabase.tableId)
        Assert.assertEquals(newVisibilityInt, tableObjectFromDatabase.visibility)
        Assert.assertEquals(newUploadStatusInt, tableObjectFromDatabase.uploadStatus)
        Assert.assertEquals(newEtag, tableObjectFromDatabase.etag)
    }

    @Test
    fun updateTableObjectShouldNotThrowAnExceptionWhenTheTableObjectDoesNotExist(){
        // Arrange
        val tableObjectEntity = TableObjectEntity(-2, UUID.randomUUID().toString(), 0, 0, false, "")
        tableObjectEntity.id = -3
        val tableObject = TableObject.convertTableObjectEntityToTableObject(tableObjectEntity)

        // Act
        runBlocking { Dav.Database.updateTableObject(tableObject) }
    }
    // End updateTableObject tests

    // tableObjectExists tests
    @Test
    fun tableObjectExistsShouldReturnTrueIfTheTableObjectExists(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableObject = runBlocking {
            TableObject.create(uuid, 1)
        }

        // Act
        val tableObjectExists = runBlocking {
            Dav.Database.tableObjectExists(uuid)
        }

        // Assert
        Assert.assertTrue(tableObjectExists)
    }

    @Test
    fun tableObjectExistsShouldReturnFalseIfTheTableObjectDoesNotExist(){
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val tableObjectExists = runBlocking {
            Dav.Database.tableObjectExists(uuid)
        }

        // Assert
        Assert.assertFalse(tableObjectExists)
    }
    // End tableObjectExists tests

    // deleteTableObject tests
    @Test
    fun deleteTableObjectShouldSetTheUploadStatusToDeleted(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 2
        val tableObject = runBlocking {
            TableObject.create(uuid, tableId)
        }

        // Act
        runBlocking {
            Dav.Database.deleteTableObject(uuid)
        }

        // Assert
        val tableObjectFromDatabase = database.tableObjectDao().getTableObject(tableObject.id)
        Assert.assertEquals(TableObjectUploadStatus.Deleted.uploadStatus, tableObjectFromDatabase.uploadStatus)
    }

    @Test
    fun deleteTableObjectShouldDeleteTheTableObjectAndItsPropertiesIfTheUploadStatusIsDeleted(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 4
        val firstPropertyName = "page1"
        val secondPropertyName = "page2"
        val firstPropertyValue = "Hello World"
        val secondPropertyValue = "Hallo Welt"

        val properties = arrayListOf<Property>(
                Property(0, firstPropertyName, firstPropertyValue),
                Property(0, secondPropertyName, secondPropertyValue))

        val tableObject = runBlocking {
            TableObject.create(uuid, tableId, properties)
        }
        val firstPropertyId = tableObject.properties[0].id
        val secondPropertyId = tableObject.properties[1].id
        runBlocking { tableObject.saveUploadStatus(TableObjectUploadStatus.Deleted) }

        // Check if the table object and the properties were created
        var tableObjectFromDatabase = database.tableObjectDao().getTableObject(tableObject.id)
        Assert.assertNotNull(tableObjectFromDatabase)

        var firstPropertyFromDatabase = database.propertyDao().getProperty(firstPropertyId)
        Assert.assertNotNull(firstPropertyFromDatabase)

        var secondPropertyFromDatabase = database.propertyDao().getProperty(secondPropertyId)
        Assert.assertNotNull(secondPropertyFromDatabase)

        // Act
        runBlocking { Dav.Database.deleteTableObject(uuid) }

        // Assert
        tableObjectFromDatabase = database.tableObjectDao().getTableObject(tableObject.id)
        Assert.assertNull(tableObjectFromDatabase)

        firstPropertyFromDatabase = database.propertyDao().getProperty(firstPropertyId)
        Assert.assertNull(firstPropertyFromDatabase)

        secondPropertyFromDatabase = database.propertyDao().getProperty(secondPropertyId)
        Assert.assertNull(secondPropertyFromDatabase)
    }
    // End deleteTableObject tests

    // deleteTableObjectImmediately tests
    @Test
    fun deleteTableObjectImmediatelyShouldDeleteTheTableObjectAndItsPropertiesImmediately(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 6
        val firstPropertyName = "page1"
        val secondPropertyName = "page2"
        val firstPropertyValue = "Hello World"
        val secondPropertyValue = "Hallo Welt"

        val properties = arrayListOf<Property>(
                Property(0, firstPropertyName, firstPropertyValue),
                Property(0, secondPropertyName, secondPropertyValue))

        val tableObject = runBlocking {
            TableObject.create(uuid, tableId, properties)
        }
        val firstPropertyId = tableObject.properties[0].id
        val secondPropertyId = tableObject.properties[1].id

        // Check if the table object and the properties were created
        var tableObjectFromDatabase = database.tableObjectDao().getTableObject(tableObject.id)
        Assert.assertNotNull(tableObjectFromDatabase)

        var firstPropertyFromDatabase = database.propertyDao().getProperty(firstPropertyId)
        Assert.assertNotNull(firstPropertyFromDatabase)

        var secondPropertyFromDatabase = database.propertyDao().getProperty(secondPropertyId)
        Assert.assertNotNull(secondPropertyFromDatabase)

        // Act
        runBlocking { Dav.Database.deleteTableObjectImmediately(uuid) }

        // Assert
        tableObjectFromDatabase = database.tableObjectDao().getTableObject(tableObject.id)
        Assert.assertNull(tableObjectFromDatabase)

        firstPropertyFromDatabase = database.propertyDao().getProperty(firstPropertyId)
        Assert.assertNull(firstPropertyFromDatabase)

        secondPropertyFromDatabase = database.propertyDao().getProperty(secondPropertyId)
        Assert.assertNull(secondPropertyFromDatabase)
    }
    // End deleteTableObjectImmediately

    // createProperty tests
    @Test
    fun createPropertyShouldSaveThePropertyInTheDatabaseAndReturnThePropertyId(){
        // Arrange
        val tableObject = runBlocking {
            TableObject.create(2)
        }
        val property = Property(tableObject.id, "page1", "Hello World")

        // Act
        val id = runBlocking {
            Dav.Database.createProperty(property)
        }

        // Assert
        Assert.assertNotNull(id)
        val propertyFromDatabase = database.propertyDao().getProperty(id!!)
        Assert.assertEquals(property.tableObjectId, propertyFromDatabase.tableObjectId)
        Assert.assertEquals(property.name, propertyFromDatabase.name)
        Assert.assertEquals(property.value, propertyFromDatabase.value)
    }
    // End createProperty tests

    // getPropertiesOfTableObject tests
    @Test
    fun getPropertiesOfTableObjectShouldReturnAllPropertiesOfTheTableObject(){
        // Arrange
        val uuid = UUID.randomUUID()
        val tableId = 8
        val firstPropertyName = "page1"
        val secondPropertyName = "page2"
        val firstPropertyValue = "Hello World"
        val secondPropertyValue = "Hallo Welt"

        val properties = arrayListOf<Property>(
                Property(0, firstPropertyName, firstPropertyValue),
                Property(0, secondPropertyName, secondPropertyValue))

        val tableObject = runBlocking {
            TableObject.create(uuid, tableId, properties)
        }

        // Act
        val propertiesList = runBlocking {
            Dav.Database.getPropertiesOfTableObject(tableObject.id)
        }

        // Assert
        Assert.assertEquals(propertiesList.size, 2)
        Assert.assertEquals(firstPropertyName, propertiesList[0].name)
        Assert.assertEquals(firstPropertyValue, propertiesList[0].value)
        Assert.assertEquals(secondPropertyName, propertiesList[1].name)
        Assert.assertEquals(secondPropertyValue, propertiesList[1].value)
    }
    // End getPropertiesOfTableObject tests

    // updateProperty tests
    @Test
    fun updatePropertyShouldUpdateThePropertyInTheDatabase(){
        // Arrange
        val oldPropertyName = "page1"
        val newPropertyName = "page2"
        val oldPropertyValue = "Hello World"
        val newPropertyValue = "Hallo Welt"
        val tableObject = runBlocking {
            TableObject.create(5)
        }

        val oldPropertyEntity = PropertyEntity(tableObject.id, oldPropertyName, oldPropertyValue)
        oldPropertyEntity.id = database.propertyDao().insertProperty(oldPropertyEntity)

        // Create a second property with the same id but different values and replace the old property with this one
        val newPropertyEntity = PropertyEntity(tableObject.id, newPropertyName, newPropertyValue)
        newPropertyEntity.id = oldPropertyEntity.id
        val newProperty = Property.convertPropertyEntityToProperty(newPropertyEntity)

        // Act
        runBlocking { Dav.Database.updateProperty(newProperty) }

        // Assert
        val propertyFromDatabase = database.propertyDao().getProperty(newProperty.id)
        Assert.assertEquals(newProperty.id, propertyFromDatabase.id)
        Assert.assertEquals(tableObject.id, propertyFromDatabase.tableObjectId)
        Assert.assertEquals(newPropertyName, propertyFromDatabase.name)
        Assert.assertEquals(newPropertyValue, propertyFromDatabase.value)
    }

    @Test
    fun updatePropertyShouldNotThrowAnExceptionWhenThePropertyDoesNotExist(){
        // Arrange
        val property = Property(-3, "page1", "Hello World")
        property.id = 92

        // Act
        runBlocking { Dav.Database.updateProperty(property) }
    }
    // End updateProperty tests

    // propertyExists tests
    @Test
    fun propertyExistsShouldReturnTrueIfThePropertyExists(){
        // Arrange
        val tableObject = runBlocking {
            TableObject.create(5)
        }
        val property = runBlocking {
            Property.create(tableObject.id, "page1", "Hello World")
        }

        // Act
        val propertyExists = runBlocking {
            Dav.Database.propertyExists(property.id)
        }

        // Assert
        Assert.assertTrue(propertyExists)
    }

    @Test
    fun propertyExistsShouldReturnFalseIfThePropertyDoesNotExist(){
        // Act
        val propertyExists = runBlocking {
            Dav.Database.propertyExists(8)
        }

        // Assert
        Assert.assertFalse(propertyExists)
    }
    // End propertyExists tests
}