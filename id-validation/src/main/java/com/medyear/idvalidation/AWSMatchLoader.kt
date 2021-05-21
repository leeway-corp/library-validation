package com.medyear.idvalidation

import android.content.Context
import android.graphics.Bitmap
import androidx.loader.content.AsyncTaskLoader
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognition
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.CompareFacesRequest
import com.amazonaws.services.rekognition.model.Image
import com.medyear.idvalidation.model.AWSResult
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal class AWSMatchLoader(
    context: Context,
    private val source: Bitmap? = null,
    private val target: Bitmap? = null,
) : AsyncTaskLoader<Any>(context) {

    private val recognition: AmazonRekognition

    init {
        val provider = CognitoCachingCredentialsProvider(
            context,
            "us-east-2:36e153b5-791b-4366-9b8c-478bf9a9e9f4",
            Regions.US_EAST_2
        )
        recognition = AmazonRekognitionClient(provider)
    }

    override fun loadInBackground(): AWSResult {
        if (source == null || target == null) return AWSResult.error("Source and target photos not found")
        try {
            val sourceImage = ByteArrayOutputStream().use { out ->
                source.compress(Bitmap.CompressFormat.JPEG, 90, out)
                Image().withBytes(ByteBuffer.wrap(out.toByteArray()))
            }
            val targetImage = ByteArrayOutputStream().use { out ->
                target.compress(Bitmap.CompressFormat.JPEG, 90, out)
                Image().withBytes(ByteBuffer.wrap(out.toByteArray()))
            }

            val request = CompareFacesRequest()
                .withSourceImage(sourceImage)
                .withTargetImage(targetImage)
                .withSimilarityThreshold(70f)

            val result = recognition.compareFaces(request)
            for (match in result.faceMatches) {
                if (match.face.confidence > 85) {
                    return AWSResult.success()
                }
            }
            return AWSResult.error("ID card not validated")
        } catch (e: Exception) {
            Timber.e(e)
            return AWSResult.error(e.message ?: "AWS error")
        }
    }

    override fun onStartLoading() {
        super.onStartLoading()
        forceLoad()
    }

}