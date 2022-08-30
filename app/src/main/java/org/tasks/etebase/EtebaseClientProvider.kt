package org.tasks.etebase

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import com.etebase.client.Account
import com.etebase.client.Client
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.DebugNetworkInterceptor
import org.tasks.caldav.MemoryCookieStore
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
import org.tasks.http.UserAgentInterceptor
import org.tasks.preferences.Preferences
import org.tasks.security.KeyStoreEncryption
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLContext

class EtebaseClientProvider @Inject constructor(
        @ApplicationContext private val context: Context,
        private val encryption: KeyStoreEncryption,
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor,
        private val caldavDao: CaldavDao
) {
    @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
    suspend fun forAccount(account: CaldavAccount): EtebaseClient = forUrl(
            account.url!!,
            account.username!!,
            null,
            account.getPassword(encryption))

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    suspend fun forUrl(url: String, username: String, password: String?, session: String? = null, foreground: Boolean = false): EtebaseClient = withContext(Dispatchers.IO) {
        val httpClient = createHttpClient(foreground)
        val client = Client.create(httpClient, url)
        val etebase = session
                ?.let { Account.restore(client, it, null) }
                ?: Account.login(client, username, password!!)
        EtebaseClient(context, username, etebase, caldavDao)
    }

    private suspend fun createHttpClient(foreground: Boolean): OkHttpClient {
        val customCertManager = withContext(Dispatchers.Default) {
            CustomCertManager(context, foreground)
        }
        val hostnameVerifier = customCertManager.hostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient()
                .newBuilder()
                .addNetworkInterceptor(UserAgentInterceptor)
                .cookieJar(MemoryCookieStore())
                .followRedirects(false)
                .followSslRedirects(true)
                .sslSocketFactory(sslContext.socketFactory, customCertManager)
                .hostnameVerifier(hostnameVerifier)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
        if (preferences.isFlipperEnabled) {
            interceptor.apply(builder)
        }
        return builder.build()
    }
}