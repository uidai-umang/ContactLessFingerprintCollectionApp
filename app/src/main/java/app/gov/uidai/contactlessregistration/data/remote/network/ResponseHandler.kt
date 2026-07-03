package app.gov.uidai.contactlessregistration.data.remote.network

import org.json.JSONObject
import retrofit2.Response

object ResponseHandler {

    // Wraps a Retrofit API call into ApiResult.
    // Handles HTTP errors, null bodies, and network exceptions in one place.
    suspend fun <T> safeApiCall(
        call: suspend () -> Response<T>
    ): ApiResult<T> {
        return try {
            val response = call()
            handleResponse(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unexpected error occurred")
        }
    }

    // Converts Retrofit Response into ApiResult.Success or ApiResult.Error
    private fun <T> handleResponse(response: Response<T>): ApiResult<T> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Empty response body", response.code())
            }
        } else {
            val errorMessage = parseErrorBody(
                code = response.code(),
                errorBody = response.errorBody()?.string()
            )
            ApiResult.Error(errorMessage, response.code())
        }
    }

    // Parses backend error JSON into a readable message.
    // Backend sends: { "error": "message" } or { "message": "..." }
    private fun parseErrorBody(code: Int, errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return getDefaultMessage(code)

        return try {
            val json = JSONObject(errorBody)
            json.optString("error")
                .ifBlank { json.optString("message") }
                .ifBlank { getDefaultMessage(code) }
        } catch (e: Exception) {
            getDefaultMessage(code)
        }
    }

    // Maps HTTP status codes to user-friendly messages
    private fun getDefaultMessage(code: Int): String = when (code) {
        400 -> "Bad request"
        401 -> "Unauthorised"
        403 -> "Access denied"
        404 -> "Not found"
        409 -> "Conflict — resource already exists"
        429 -> "Too many requests, try again later"
        500 -> "Internal server error"
        503 -> "Service unavailable"
        else -> "Something went wrong (code $code)"
    }
}