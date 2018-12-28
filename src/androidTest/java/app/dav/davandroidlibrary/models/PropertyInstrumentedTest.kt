package app.dav.davandroidlibrary.models

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import app.dav.davandroidlibrary.Dav
import app.dav.davandroidlibrary.common.*
import app.dav.davandroidlibrary.data.DavDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertyInstrumentedTest {
    val context = InstrumentationRegistry.getTargetContext()
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
}