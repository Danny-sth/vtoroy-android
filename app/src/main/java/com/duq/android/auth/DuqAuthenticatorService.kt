package com.duq.android.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Service that provides the AccountAuthenticator to the system.
 * Required for AccountManager integration.
 */
class DuqAuthenticatorService : Service() {

    private lateinit var authenticator: DuqAccountAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = DuqAccountAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}
