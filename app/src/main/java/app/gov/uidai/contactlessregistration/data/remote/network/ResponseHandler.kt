package app.gov.uidai.contactlessregistration.data.remote.network

import app.gov.uidai.contactlessregistration.model.common.BackendErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import retrofit2.Response

object ResponseHandler {

    private val gson = Gson()

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

    private fun <T> handleResponse(response: Response<T>): ApiResult<T> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Empty response body", response.code())
            }
        } else {
            val (message, errorData) = parseErrorBody(
                code = response.code(),
                errorBody = response.errorBody()?.string()
            )
            ApiResult.Error(message, response.code(), errorData)
        }
    }

    // Parses backend error JSON. Tries BackendErrorResponse shape first ({"message","data"}),
    // falls back to legacy {"error"/"message"} parsing for endpoints not yet updated.
    private fun parseErrorBody(code: Int, errorBody: String?): Pair<String, Any?> {
        if (errorBody.isNullOrBlank()) return Pair(getDefaultMessage(code), null)

        // Try new backend contract first
        try {
            val parsed = gson.fromJson(errorBody, BackendErrorResponse::class.java)
            if (!parsed.message.isNullOrBlank()) {
                return Pair(parsed.message, parsed.data)
            }
        } catch (_: JsonSyntaxException) { }

        // Fall back to legacy generic parsing
        return try {
            val json = JSONObject(errorBody)
            val message = json.optString("error")
                .ifBlank { json.optString("message") }
                .ifBlank { getDefaultMessage(code) }
            Pair(message, null)
        } catch (_: Exception) {
            Pair(getDefaultMessage(code), null)
        }
    }

    private fun getDefaultMessage(code: Int): String = when (code) {
        400 -> "Bad request"
        401 -> "Unauthorised"
        403 -> "Access denied"
        404 -> "Not found"
        409 -> "Conflict — resource already exists"
        422 -> "Unprocessable entity"
        429 -> "Too many requests, try again later"
        500 -> "Internal server error"
        503 -> "Service unavailable"
        else -> "Something went wrong (code $code)"
    }
}
