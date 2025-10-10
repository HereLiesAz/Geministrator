package com.hereliesaz.geministrator.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.hereliesaz.geministrator.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OAuth2RedirectActivity : ComponentActivity() {

    private lateinit var authService: AuthorizationService
    private lateinit var settingsRepository: SettingsRepository
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthorizationService(this)
        settingsRepository = SettingsRepository(this)

        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        if (response != null) {
            // Authorization successful
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, exception ->
                tokenResponse?.accessToken?.let { accessToken ->
                    // Token exchange successful
                    lifecycleScope.launch {
                        settingsRepository.saveGithubAccessToken(accessToken)
                        fetchGithubUsernameAndFinish(accessToken)
                    }
                } ?: run {
                    // Token exchange failed
                    Log.e("OAuth2RedirectActivity", "Token exchange failed", exception)
                    finish()
                }
            }
        } else {
            // Authorization failed
            Log.e("OAuth2RedirectActivity", "Authorization failed", ex)
            finish()
        }
    }

    private fun fetchGithubUsernameAndFinish(accessToken: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/user")
                    .header("Authorization", "token $accessToken")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody)
                    val username = jsonObject.getString("login")
                    settingsRepository.saveGithubUsername(username)
                } else {
                    Log.e("OAuth2RedirectActivity", "Failed to fetch GitHub user")
                }
            } catch (e: Exception) {
                Log.e("OAuth2RedirectActivity", "Error fetching GitHub user", e)
            } finally {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }
}
