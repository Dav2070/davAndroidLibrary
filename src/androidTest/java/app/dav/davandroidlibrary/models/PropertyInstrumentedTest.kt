package app.dav.davandroidlibrary.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.dav.davandroidlibrary.Dav
import app.dav.davandroidlibrary.common.*
import app.dav.davandroidlibrary.data.DavDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertyInstrumentedTest {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val database = DavDatabase.getInstance(context)

    init {
        Dav.init(context)

        // Drop the database
        database.tableObjectDao().deleteAllTableObjects()

        ProjectInterface.localDataSettings = LocalDataSettings()
        ProjectInterface.retrieveConstants = RetrieveConstants()
        ProjectInterface.triggerAction = TriggerAction()
        ProjectInterface.generalMethods = GeneralMethods()
    }

    // setPropertyValue tests
    @Test
    fun setPropertyValueShouldUpdateThePropertyWithTheNewValueInTheDatabase(){
        // Arrange
        val propertyName = "page1"
        val oldPropertyValue = "Hello World"
        val newPropertyValue = "Hallo Welt"
        val tableObject = runBlocking {
            TableObject.create(12)
        }
        val property = runBlocking {
            Property.create(tableObject.id, propertyName, oldPropertyValue)
        }

        // Act
        runBlocking { property.setPropertyValue(newPropertyValue) }

        // Assert
        val propertyFromDatabase = database.propertyDao().getProperty(property.id)
        Assert.assertEquals(propertyName, propertyFromDatabase.name)
        Assert.assertEquals(newPropertyValue, property.value)
        Assert.assertEquals(newPropertyValue, propertyFromDatabase.value)
        Assert.assertEquals(tableObject.id, propertyFromDatabase.tableObjectId)
    }
    // End setPropertyValue tests

    // toPropertyEntity tests
    @Test
    fun toPropertyEntityShouldReturnValidPropertyEntityObject(){
        // Arrange
        val tableObject = runBlocking { TableObject.create(32) }
        val propertyName = "test"
        val propertyValue = "blabla"
        val property = runBlocking {
            Property.create(tableObject.id, propertyName, propertyValue)
        }

        // Act
        val propertyEntity = property.toPropertyEntity()

        // Assert
        Assert.assertEquals(property.id, propertyEntity.id)
        Assert.assertEquals(tableObject.id, propertyEntity.tableObjectId)
        Assert.assertEquals(propertyName, propertyEntity.name)
        Assert.assertEquals(propertyValue, propertyEntity.value)
    }
    // End toPropertyEntity tests

    // create tests
    @Test
    fun createShouldCreateANewPropertyAndSaveItInTheDatabase(){
        // Arrange
        val tableObject = runBlocking { TableObject.create(4) }
        val propertyName = "test"
        val propertyValue = "blabla"

        // Act
        val property = runBlocking {
            Property.create(tableObject.id, propertyName, propertyValue)
        }

        // Assert
        Assert.assertEquals(tableObject.id, property.tableObjectId)
        Assert.assertEquals(propertyName, property.name)
        Assert.assertEquals(propertyValue, property.value)

        val propertyFromDatabase = database.propertyDao().getProperty(property.id)
        Assert.assertEquals(tableObject.id, propertyFromDatabase.tableObjectId)
        Assert.assertEquals(propertyName, propertyFromDatabase.name)
        Assert.assertEquals(propertyValue, propertyFromDatabase.value)
    }
    // End create tests

    // convertPropertyEntityToProperty tests
    @Test
    fun convertPropertyEntityToPropertyShouldReturnAValidProperty(){
        // Arrange
        val tableObjectId = 23L
        val propertyName = "test"
        val propertyValue = "blabla"
        val propertyEntity = PropertyEntity(tableObjectId, propertyName, propertyValue)

        // Act
        val property = Property.convertPropertyEntityToProperty(propertyEntity)

        // Assert
        Assert.assertEquals(tableObjectId, property.tableObjectId)
        Assert.assertEquals(propertyName, property.name)
        Assert.assertEquals(propertyValue, property.value)
    }
    // End convertPropertyEntityToProperty tests
}