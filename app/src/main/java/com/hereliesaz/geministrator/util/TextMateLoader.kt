package com.hereliesaz.geministrator.util

import android.content.Context
import io.github.rosemoe.sora.lang.textmate.TextMateColorScheme
import io.github.rosemoe.sora.lang.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.lang.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.lang.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.lang.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.lang.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.text.TextUtils
import java.io.IOException

object TextMateLoader {

    fun load(context: Context) {
        try {
            // Add a file resolver for assets
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.assets))

            // Load the theme
            val themeRegistry = ThemeRegistry.getInstance()
            val themeName = "quietlight-plus"
            val themePath = "textmate/$themeName.json"
            themeRegistry.loadTheme(
                ThemeModel(
                    io.github.rosemoe.sora.lang.textmate.registry.source.FileStreamProvider(themePath),
                    themeName
                )
            )
            themeRegistry.setTheme(themeName)

            // Load the grammars
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

            // Create and apply the color scheme
            TextMateColorScheme.create(ThemeRegistry.getInstance())

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
