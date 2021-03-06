package app.dav.davandroidlibrary.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.dav.davandroidlibrary.Constants
import app.dav.davandroidlibrary.Dav
import app.dav.davandroidlibrary.common.*
import app.dav.davandroidlibrary.data.DavDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DavUserInstrumentedTest {
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

    // Constructor tests
    @Test
    fun constructorShouldCreateNewDavUserWithNotLoggedInUserWhenNoJWTIsSaved(){
        // Arrange
        ProjectInterface.localDataSettings?.setStringValue(Dav.jwtKey, "")

        // Act
        val user = DavUser()

        // Assert
        Assert.assertFalse(user.isLoggedIn)
    }

    @Test
    fun constructorShouldCreateNewDavUserWithLoggedInUserWhenJWTIsSaved(){
        // Arrange
        ProjectInterface.localDataSettings?.setStringValue(Dav.jwtKey, Constants.jwt)

        // Act
        val user = DavUser()

        // Assert
        Assert.assertTrue(user.isLoggedIn)
        Assert.assertEquals(Constants.jwt, user.jwt)
    }
    // End constructor tests

    // Login tests
    @Test
    fun loginWithValidJwtShouldLogTheUserInAndDownloadTheUserInformation(){
        // Arrange
        ProjectInterface.localDataSettings?.setStringValue(Dav.jwtKey, "")
        val user = DavUser()

        // Act
        runBlocking { user.login(Constants.jwt) }

        // Assert
        Assert.assertTrue(user.isLoggedIn)
        Assert.assertEquals(Constants.jwt, user.jwt)
        Assert.assertEquals(Constants.testuserEmail, user.email)
        Assert.assertEquals(Constants.testuserUsername, user.username)
        Assert.assertEquals(Constants.testuserPlan, user.plan)

        // Check if the avatar was downloaded
        Assert.assertTrue(user.avatar.exists())
    }

    @Test
    fun loginWithInvalidJwtShouldNotLogTheUserIn(){
        // Arrange
        ProjectInterface.localDataSettings?.setStringValue(Dav.jwtKey, "")
        val user = DavUser()

        // Act
        runBlocking { user.login("blablabla") }

        // Assert
        Assert.assertFalse(user.isLoggedIn)
        Assert.assertTrue(user.jwt.isEmpty())
    }
    // End login tests

    // Logout tests
    @Test
    fun logoutShouldRemoveAllUserDataAndDeleteTheAvatar(){
        // Arrange
        val user = DavUser()
        runBlocking { user.login(Constants.jwt) }
        Assert.assertTrue(user.isLoggedIn)

        // Act
        user.logout()

        // Assert
        Assert.assertFalse(user.isLoggedIn)
        Assert.assertTrue(user.email.isEmpty())
        Assert.assertTrue(user.username.isEmpty())
        Assert.assertTrue(user.jwt.isEmpty())
        Assert.assertFalse(user.avatar.exists())
    }
    // End logout tests

    // convertIntToDavPlan tests
    @Test
    fun convertIntToDavPlanShouldConvert0ToFree(){
        // Arrange
        val planInt = 0

        // Act
        val plan = DavUser.convertIntToDavPlan(planInt)

        // Assert
        Assert.assertEquals(DavPlan.Free, plan)
    }

    @Test
    fun convertIntToDavPlanShouldConvert1ToPlus(){
        // Arrange
        val planInt = 1

        // Act
        val plan = DavUser.convertIntToDavPlan(planInt)

        // Assert
        Assert.assertEquals(DavPlan.Plus, plan)
    }
    // End convertIntToDavPlan tests
}