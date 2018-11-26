package app.dav.davandroidlibrary.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import app.dav.davandroidlibrary.Dav
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

@Entity(tableName = "TableObject")
data class TableObjectEntity(
        var tableId: Int,
        var uuid: String,
        var visibility: Int,
        var uploadStatus: Int,
        var isFile: Boolean,
        var etag: String
){
    @PrimaryKey var id: Long? = null
}

class TableObject{
    var id: Long = 0
    var tableId: Int = 0
    var uuid: UUID = UUID.randomUUID()
    var visibility: TableObjectVisibility = TableObjectVisibility.Private
    var uploadStatus: TableObjectUploadStatus = TableObjectUploadStatus.New
    var isFile: Boolean = false
    var file: File? = null
    var etag: String = ""
    var properties = ArrayList<Property>()

    constructor()

    constructor(tableId: Int){
        this.tableId = tableId
    }

    constructor(uuid: UUID, tableId: Int){
        this.uuid = uuid
        this.tableId = tableId
    }

    constructor(uuid: UUID, tableId: Int, properties: ArrayList<Property>){
        this.uuid = uuid
        this.tableId = tableId
        for (p in properties) this.properties.add(p)
    }

    private suspend fun save(){
        // Check if the table object already exists
        if(!Dav.Database.tableObjectExists(uuid).await()){
            id = Dav.Database.createTableObject(this).await()
        }else{
            Dav.Database.updateTableObject(this)
        }
    }

    private suspend fun saveWithProperties(){
        // Check if the table object already exists
        if(!Dav.Database.tableObjectExists(uuid).await()){
            id = Dav.Database.createTableObjectWithProperties(this).await()
        }else{
            val tableObject = Dav.Database.getTableObject(uuid).await()

            if(tableObject != null){
                id = tableObject.id

                Dav.Database.updateTableObject(this)
                for(p in properties) tableObject.setPropertyValue(p.name, p.value)
            }
        }

        // TODO SyncPush()
    }

    suspend fun setFile(file: File){
        saveFile(file)
    }

    private suspend fun saveFile(file: File){
        if(uploadStatus == TableObjectUploadStatus.UpToDate) uploadStatus = TableObjectUploadStatus.Updated

        // Save the file in the data folder with the uuid as name (without extension)
        val filename: String = uuid.toString()
        val tableFolder = Dav.Database.getTableFolder(tableId)
        val newFile = File(tableFolder.path + "/" + filename)
        this.file = file.copyTo(newFile, true)

        if(!file.extension.isEmpty()){
            setPropertyValue("ext", file.extension)
        }

        save()
    }

    suspend fun load(){
        loadProperties()
        loadFile()
    }

    private suspend fun loadProperties(){
        properties.clear()
        for (property in Dav.Database.getPropertiesOfTableObject(id).await()){
            properties.add(property)
        }
    }

    private fun loadFile(){
        if(!isFile) return

        val filePath: String = Dav.dataPath + tableId + "/" + uuid
        val file = File(filePath)

        if(file.exists()){
            this.file = file
        }
    }

    fun getPropertyValue(name: String) : String?{
        val property = properties.find { it.name == name }
        return if(property != null) property.value else null
    }

    suspend fun setPropertyValue(name: String, value: String){
        val property = properties.find { it.name == name }

        if(property != null){
            if(property.value == value) return

            // Update the property
            property.setPropertyValue(value)
        }else{
            // Create a new property
            properties.add(Property.create(id, name, value))
        }

        if(uploadStatus == TableObjectUploadStatus.UpToDate && !isFile)
            uploadStatus = TableObjectUploadStatus.Updated

        save()
        // TODO SyncPush()
    }

    suspend fun changeUploadStatus(uploadStatus: TableObjectUploadStatus){
        if(uploadStatus == this.uploadStatus) return

        this.uploadStatus = uploadStatus
        save()
    }

    suspend fun delete(){
        val jwt = null    // TODO DavUser.getJWT()

        if(jwt == null){
            deleteImmediately()
            uploadStatus = TableObjectUploadStatus.Deleted
        }else{
            val file = this.file
            if(file != null){
                if(isFile && file.exists()){
                    file.delete()
                }
            }

            changeUploadStatus(TableObjectUploadStatus.Deleted)
            // TODO SyncPush()
        }
    }

    suspend fun deleteImmediately(){
        if(isFile && file != null){
            file?.delete()
        }

        Dav.Database.deleteTableObjectImmediately(uuid)
    }

    companion object {
        suspend fun create(tableId: Int) : TableObject {
            val tableObject = TableObject(tableId)
            tableObject.save()
            return tableObject
        }

        suspend fun create(uuid: UUID, tableId: Int) : TableObject {
            val tableObject = TableObject(uuid, tableId)
            tableObject.save()
            return tableObject
        }

        suspend fun create(uuid: UUID, tableId: Int, file: File) : TableObject {
            val tableObject = TableObject(uuid, tableId)
            tableObject.isFile = true
            tableObject.save()
            tableObject.saveFile(file)
            return tableObject
        }

        suspend fun create(uuid: UUID, tableId: Int, properties: ArrayList<Property>) : TableObject {
            val tableObject = TableObject(uuid, tableId, properties)
            tableObject.saveWithProperties()
            return tableObject
        }

        fun convertIntToVisibility(visibility: Int) : TableObjectVisibility {
            return when(visibility){
                2 -> TableObjectVisibility.Public
                1 -> TableObjectVisibility.Protected
                else -> TableObjectVisibility.Private
            }
        }

        fun convertIntToUploadStatus(uploadStatus: Int) : TableObjectUploadStatus {
            return when(uploadStatus){
                4 -> TableObjectUploadStatus.NoUpload
                3 -> TableObjectUploadStatus.Deleted
                2 -> TableObjectUploadStatus.Updated
                1 -> TableObjectUploadStatus.New
                else -> TableObjectUploadStatus.UpToDate
            }
        }

        fun convertTableObjectEntityToTableObject(obj: TableObjectEntity) : TableObject {
            val tableObject = TableObject()
            tableObject.id = obj.id ?: 0
            tableObject.tableId = obj.tableId
            tableObject.uuid = UUID.fromString(obj.uuid)
            tableObject.visibility = convertIntToVisibility(obj.visibility)
            tableObject.uploadStatus = convertIntToUploadStatus(obj.uploadStatus)
            tableObject.isFile = obj.isFile
            tableObject.etag = obj.etag
            return tableObject
        }

        fun convertTableObjectToTableObjectEntity(tableObject: TableObject) : TableObjectEntity {
            val tableObjectEntity = TableObjectEntity(tableObject.tableId,
                    tableObject.uuid.toString(),
                    tableObject.visibility.visibility,
                    tableObject.uploadStatus.uploadStatus,
                    tableObject.isFile,
                    tableObject.etag)
            tableObjectEntity.id = if(tableObject.id == 0L) null else tableObject.id
            return tableObjectEntity
        }

        internal fun convertTableObjectDataToTableObject(tableObjectData: TableObjectData) : TableObject{
            val tableObject = TableObject()
            tableObject.id = tableObjectData.id
            tableObject.tableId = tableObjectData.table_id
            tableObject.visibility = convertIntToVisibility(tableObjectData.visibility)
            tableObject.uuid = tableObjectData.uuid
            tableObject.isFile = tableObjectData.file
            tableObject.etag = tableObjectData.etag

            val properties = ArrayList<Property>()

            for(key in tableObjectData.properties.keys()){
                val property = Property()
                property.name = key
                property.value = tableObjectData.properties.get(key)
                properties.add(property)
            }

            tableObject.properties = properties
            return tableObject
        }
    }
}

enum class TableObjectVisibility(val visibility: Int){
    Private(0),
    Protected(1),
    Public(2)
}

enum class TableObjectUploadStatus(val uploadStatus: Int){
    UpToDate(0),
    New(1),
    Updated(2),
    Deleted(3),
    NoUpload(4)
}

internal data class TableObjectData(
        var id: Long,
        val table_id: Int,
        val visibility: Int,
        val uuid: UUID,
        val file: Boolean,
        val properties: Dictionary<String, String>,
        val etag: String
)