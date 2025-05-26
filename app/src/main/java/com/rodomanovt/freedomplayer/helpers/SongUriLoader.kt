//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.media.MediaMetadataRetriever
//import android.net.Uri
//import android.util.Log
//import com.bumptech.glide.Glide
//import com.bumptech.glide.Priority
//import com.bumptech.glide.Registry
//import com.bumptech.glide.annotation.GlideModule
//import com.bumptech.glide.load.DataSource
//import com.bumptech.glide.load.Options
//import com.bumptech.glide.load.ResourceDecoder
//import com.bumptech.glide.load.data.DataFetcher
//import com.bumptech.glide.load.model.Model
//import com.bumptech.glide.load.model.ModelLoader
//import com.bumptech.glide.load.model.ModelLoaderFactory
//import com.bumptech.glide.load.model.MultiModelLoaderFactory
//import com.bumptech.glide.module.AppGlideModule
//import com.bumptech.glide.signature.ObjectKey
//import com.rodomanovt.freedomplayer.model.Song
//import java.io.ByteArrayInputStream
//import java.io.InputStream
//
//// Добавьте в AppGlideModule (если используете Glide v4)
//@GlideModule
//class MyAppGlideModule : AppGlideModule() {
//    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
//        registry.append(
//            Song::class.java,
//            Bitmap::class.java,
//            AudioCoverModelLoader.Factory(context.applicationContext)
//        )
//    }
//}
//
//// Кастомный ModelLoader
//class AudioCoverModelLoader(private val context: Context) : ModelLoader<String, InputStream> {
//
//    override fun buildLoadData(
//        model: String,
//        width: Int,
//        height: Int,
//        options: Options
//    ): ModelLoader.LoadData<Bitmap> {
//        return ModelLoader.LoadData(
//            ObjectKey(model),
//            AudioCoverFetcher(context, model)
//        )
//    }
//
//    override fun handles(model: String): Boolean {
//        return model.endsWith(".mp3", ignoreCase = true)
//    }
//
//    class Factory(private val context: Context) : ModelLoaderFactory<String, InputStream> {
//
//        override fun build(registry: MultiModelLoaderFactory): ModelLoader<String, InputStream> {
//            return AudioCoverModelLoader(context)
//        }
//
//        override fun teardown() {}
//    }
//}
//
//class AudioCoverFetcher(private val context: Context, private val songPath: String) : DataFetcher<Bitmap> {
//
//    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
//        try {
//            val uri = Uri.parse(songPath)
//            val retriever = MediaMetadataRetriever()
//
//            // Открываем файл через ContentResolver
//            val inputStream = context.contentResolver.openInputStream(uri)
//            val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")?.fileDescriptor
//
//            if (fileDescriptor != null) {
//                retriever.setDataSource(fileDescriptor)
//
//                val artBytes = retriever.embeddedPicture
//                if (artBytes != null) {
//                    val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
//                    callback.onDataReady(bitmap)
//                } else {
//                    callback.onLoadFailed(Exception("No album art found"))
//                }
//            } else {
//                callback.onLoadFailed(Exception("Can't open file descriptor for $uri"))
//            }
//
//            retriever.release()
//            inputStream?.close()
//
//        } catch (e: Exception) {
//            Log.e("AudioCoverFetcher", "Error loading album art from ${songPath}", e)
//            callback.onLoadFailed(e)
//        }
//    }
//
//    override fun cleanup() {
//        // Закрытие ресурсов
//    }
//
//    override fun cancel() {
//        // Поддержка отмены
//    }
//
//    override fun getDataClass(): Class<Bitmap> = Bitmap::class.java
//    override fun getDataSource(): DataSource = DataSource.LOCAL
//}