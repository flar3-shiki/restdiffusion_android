package jp.kira.sdwebuiremote.data

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class BasicAuthInterceptor(private val username: String, private val password: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (username.isNotBlank() && password.isNotBlank()) {
            val credentials = Credentials.basic(username, password)
            val authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials)
                .build()
            return chain.proceed(authenticatedRequest)
        }
        return chain.proceed(request)
    }
}
