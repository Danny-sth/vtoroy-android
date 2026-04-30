package com.duq.android.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle

/**
 * AccountAuthenticator for storing Duq credentials.
 *
 * AccountManager data survives app data clear (pm clear),
 * unlike SharedPreferences or EncryptedSharedPreferences.
 *
 * This is the standard Android way to persist auth credentials.
 */
class DuqAccountAuthenticator(
    private val context: Context
) : AbstractAccountAuthenticator(context) {

    companion object {
        const val ACCOUNT_TYPE = "com.duq.android.account"
        const val ACCOUNT_NAME = "DuqUser"

        // Token types stored in AccountManager
        const val TOKEN_TYPE_ACCESS = "access_token"
        const val TOKEN_TYPE_REFRESH = "refresh_token"
        const val TOKEN_TYPE_ID = "id_token"

        // User data keys
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_SUB = "user_sub"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USERNAME = "username"
    }

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle? {
        // We don't support adding accounts from system settings
        return null
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? {
        // Token retrieval is handled directly via AccountManager
        return null
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle? = null

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle? = null

    override fun getAuthTokenLabel(authTokenType: String?): String? = null

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle = Bundle().apply {
        putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? = null
}
