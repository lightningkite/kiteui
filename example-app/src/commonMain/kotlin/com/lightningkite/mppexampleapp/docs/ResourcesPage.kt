package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.SoundEffectPool
import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.launch

@Routable("docs/resources")
object ResourcesPage: DocPage {
    override val covers: List<String> = listOf("resources", "Resources", "assets", "Assets", "images", "audio", "video", "fonts")

    override fun ViewWriter.render(): Unit = run {
        article {
            h1("Resources")
            text("KiteUI provides a convenient way to access resources (assets) in your multiplatform application. Resources can include images, audio, video, and fonts.")

            h2("Resource Types")
            text("The following resource types are supported:")

            h3("Images")
            text("Images can be accessed through the Resources object and used in image elements.")
            example("""
                // Using a resource from your project
                image {
                    source = Resources.imagesSnowyBackground
                }
            """.trimIndent()) {
                sizeConstraints(height = 10.rem).image {
                    source = Resources.imagesSnowyBackground
                }
            }

            h3("Audio")
            text("Audio files can be played directly or through a sound effect pool. You can use buttons to trigger audio playback.")
            example("""
                // Create a sound effect pool for better performance
                val soundEffectPool = SoundEffectPool()

                // Play audio with buttons
                row {
                    expanding.button { 
                        text("Play with Pool")
                        onClick { soundEffectPool.play(Resources.audioTaunt) } 
                    }
                    expanding.button { 
                        text("Play Directly")
                        onClick { Resources.audioTaunt.load().play() } 
                    }
                }

                // Background audio with volume control
                toggleButton {
                    text("Background Audio")
                    checked bind isPlaying
                }
                backgroundAudio(Resources.audioTaunt, 0.1f) { isPlaying() }
            """.trimIndent()) {
                val soundEffectPool = SoundEffectPool()
                val playing = Property(false)
                val status = Property("Click a button to play audio")
                col {
                    row {
                        expanding.button {
                            text("Play Audio")
                            onClick { soundEffectPool.play(Resources.audioTaunt) }
                        }
                        expanding.button { 
                            text("Play Directly")
                            onClick { Resources.audioTaunt.load().play() }
                        }
                    }
                    toggleButton {
                        text("Background Audio")
                        checked bind playing
                    }
                    text { ::content { status() } }
                    text { ::content { if(playing()) "Background audio playing" else "Background audio stopped" } }
                }
            }

            h3("Video")
            text("Video files can be used in video elements.")
            example("""
                // Using a resource from your project
                video {
                    source = Resources.videoBack
                    loop = true
                    launch{ playing set true }
                }
            """.trimIndent()) {
                sizeConstraints(height = 10.rem).video {
                    source = Resources.videoBack
                    loop = true
                    launch { playing set true }
                }
            }

            h3("Fonts")
            text("Font files can be used to customize the appearance of text in your application.")
            example("""
                // Using a custom font in a theme
                val myTheme = Theme.flat2("custom").customize(
                    "customTheme",
                    body = FontAndStyle(
                        font = Resources.fontsRoboto,
                        size = 1.rem
                    )
                )
            """.trimIndent()) {
                text("Font usage example (not rendered in documentation)")
            }

            h2("Resource Organization")
            text("KiteUI does not enforce any specific organizational requirements for the resources folder. You are free to organize your resources in any way that makes sense for your project. The following is just an example of how resources might be organized:")
            scrollingHorizontally.code { 
                content = """
                    example-app/
                    └── src/
                        └── commonMain/
                            └── resources/
                                ├── audio/     # Audio files (.mp3, .wav, etc.)
                                ├── fonts/     # Font files (.ttf, .otf, etc.)
                                │   └── roboto/  # Font family folder
                                │       ├── normal.ttf
                                │       ├── bold.ttf
                                │       ├── italic.ttf
                                │       ├── bold-italic.ttf
                                │       ├── light.ttf
                                │       └── light-italic.ttf
                                ├── images/    # Image files (.png, .jpg, etc.)
                                └── video/     # Video files (.mp4, etc.)
                """.trimIndent()
            }

            h2("Accessing Resources")
            text("Resources are accessed through the Resources object, which provides properties for each resource file. The Resources object is automatically generated based on the files in your resources directory.")

            scrollingHorizontally.code { 
                content = """
                    // Import the Resources object
                    import com.lightningkite.mppexampleapp.Resources

                    // Access image resources
                    val image = Resources.imagesSnowyBackground

                    // Access audio resources
                    val audio = Resources.audioTaunt

                    // Access video resources
                    val video = Resources.videoBack
                """.trimIndent()
            }

            h2("Adding New Resources")
            text("To add new resources to your project:")
            text("""
                1. Place the resource file in the appropriate directory under src/commonMain/resources/
                2. Rebuild the project to generate the updated Resources object
                3. Access the resource through the Resources object using the generated property name
            """.trimIndent())

            h2("Resource Naming")
            text("Resource properties are named based on the file path and name. For example:")
            text("""
                • images/solera.png becomes Resources.imagesSnowyBackground
                • audio/taunt.mp3 becomes Resources.audioTaunt
                • video/back.mp4 becomes Resources.videoBack
            """.trimIndent())

            h2("Platform Considerations")
            text("KiteUI handles the platform-specific details of loading and using resources, providing a consistent API across all platforms (Android, iOS, JS, JVM, etc.).")

            h2("Font Family Organization")
            text("When you want to use a custom font as a single family with different weights and styles, you need to organize your font files in a specific way:")
            text("""
                1. Create a folder with your font family name in the resources/fonts directory
                2. Place all font files for the family in this folder with specific preset names
                3. The KiteUI plugin will automatically recognize these files as a single font family
            """.trimIndent())

            h3("Required Font File Names")
            text("For a font family to be properly recognized, use these preset file names:")
            scrollingHorizontally.code { 
                content = """
                    normal.ttf       # Regular/Normal weight (400)
                    bold.ttf         # Bold weight (700)
                    italic.ttf       # Regular/Normal weight with italic style
                    bold-italic.ttf  # Bold weight with italic style
                    light.ttf        # Light weight (300)
                    light-italic.ttf # Light weight with italic style
                """.trimIndent()
            }

            text("The plugin also recognizes other weight names in filenames:")
            scrollingHorizontally.code { 
                content = """
                    thin.ttf / hairline.ttf           # Weight 100
                    ultralight.ttf / extralight.ttf   # Weight 200
                    light.ttf                         # Weight 300
                    normal.ttf / regular.ttf          # Weight 400
                    medium.ttf                        # Weight 500
                    semibold.ttf / demibold.ttf       # Weight 600
                    bold.ttf                          # Weight 700
                    extrabold.ttf / ultrabold.ttf     # Weight 800
                    black.ttf / heavy.ttf             # Weight 900
                """.trimIndent()
            }

            text("Add 'italic' to any of these names to create the italic version (e.g., 'medium-italic.ttf').")

            h2("Best Practices")
            text("""
                • Keep resource files as small as possible to minimize app size
                • Consider providing multiple resolutions for images when needed
                • Use sound effect pools for frequently played audio
                • Be mindful of memory usage when working with large resources
                • For fonts, include only the weights and styles you actually need in your application
            """.trimIndent())
        }
    }
}
