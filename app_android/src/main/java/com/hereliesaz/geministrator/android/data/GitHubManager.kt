package com.hereliesaz.geministrator.android.data

import org.kohsuke.github.GitHub

class GitHubManager {

    private var github: GitHub? = null

    fun authenticate(token: String) {
        github = GitHub.connectUsingOAuth(token)
    }

    fun isAuthenticated(): Boolean {
        return github != null
    }
}
