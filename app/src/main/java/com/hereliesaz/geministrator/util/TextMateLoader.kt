package com.hereliesaz.geministrator.util

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.langs.textmate.registry.provider.FileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.IOException
import java.io.InputStream

object TextMateLoader {

    fun load(context: Context) {
        try {
            // Add a file resolver for assets
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.assets))

            // Load the theme
            val themeRegistry = ThemeRegistry.getInstance()
            val themeName = "quietlight-plus"
            val themePath = "textmate/$themeName.json"
            val themeInputStream = context.assets.open(themePath)
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(themeInputStream, themePath, null),
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
