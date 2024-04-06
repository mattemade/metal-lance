# Metal Lance

Game made for [1-bit jam #3](https://itch.io/jam/1-bit-jam-three).

Planned to be a mixture of beat 'em up and bullet hell scroll shooter.

Jam theme is "transformation", and I am going to interpret it without any creativity in mind: the main character is a
robot that can transform, hence such a mix of genres.

Tools used:

* [libGDX](https://github.com/libgdx/libgdx) with [TeaVM](https://github.com/konsoletyper/teavm) for code (all the code
  is GNU)
* [GIMP](https://github.com/GNOME/gimp) for images
* [LMMS](https://github.com/LMMS/lmms) for sound effects and music

Licenses:

* all the code is covered by GPL-3.0 license (see [LICENSE-CODE](LICENSE-CODE))
* all non-code assets, such as visual and audio resources, are covered by CC-BY-4.0 license (
  see [LICENSE-ASSETS](LICENSE-CODE))

## Dev log

| Day | Date       | Wkdy | Avlb | Plan                                                                                  | Spnt | Done                                                                                                                                          |
|-----|------------|------|------|---------------------------------------------------------------------------------------|------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | 04/04/2024 | THU  | 1-2  |                                                                                       | 1    | Conceptualized the game                                                                                                                       |
| 2   | 05/04/2024 | FRI  | 1-2  |                                                                                       | 1    | Explored the math of colliding function graphs a bit, decided to make everything as simple as possible (only use linear frame-time collision) |
| 3   | 06/04/2024 | SAT  | 1-2  | Init repo, create the plan                                                            |      | As planned                                                                                                                                    |
| 4   | 07/04/2024 | SUN  | 4-6  | Basic shmup character and controls (move, shoot, bomb, shield), basic moving enemy    |      |                                                                                                                                               |
| 5   | 08/04/2024 | MON  | 4-6  | Character shoots, enemy shoots bullets (t -> vec2), collisions, upgrades              |      |                                                                                                                                               |
| 6   | 09/04/2024 | TUE  | 4-6  | Enemy shooting patterns, enemy scheduling                                             |      |                                                                                                                                               |
| 7   | 10/04/2024 | WED  | 4-6  | Transformation and formation upgrades (shape shift, more controllable ships in fleet) |      |                                                                                                                                               |
| 8   | 11/04/2024 | THU  | 4-6  | Second stage with added difficulty, widescreen upgrade                                |      |                                                                                                                                               |
| 9   | 12/04/2024 | FRI  | 4-6  | 1-st stage boss                                                                       |      |                                                                                                                                               |
| 10  | 13/04/2024 | SAT  | 4-6  | 2-nd stage boss                                                                       |      |                                                                                                                                               |
| 11  | 14/04/2024 | SUN  | 4-6  | Start screen, intro sequence (add some story)                                         |      |                                                                                                                                               |
| 12  | 15/04/2024 | MON  | 1-2  | Sound effects                                                                         |      |                                                                                                                                               |
| 13  | 16/04/2024 | TUE  | 1-2  | Sound effects                                                                         |      |                                                                                                                                               |
| 14  | 17/04/2024 | WED  | 1-2  | Music                                                                                 |      |                                                                                                                                               |
| 15  | 18/04/2024 | THU  | 1-2  | Music                                                                                 |      |                                                                                                                                               |
| 16  | 19/04/2024 | FRI  | 1-2  | Extra day of something                                                                |      |                                                                                                                                               |
| 17  | 20/04/2024 | SAT  | 1-2  | Final touches, submission at 16:00                                                    |      |                                                                                                                                               |

Next section was generated by [gdx-liftoff](https://github.com/libgdx/gdx-liftoff), leaving it as is since it has some
useful stuff.

# metal-lance

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a Kotlin project template that includes Kotlin application launchers
and [KTX](https://libktx.github.io/) utilities.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.
- `teavm`: Experimental web platform using TeaVM and WebGL.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/lib`.
- `lwjgl3:run`: starts the application.
- `teavm:build`: builds the JavaScript application into the build/dist/webapp folder.
- `teavm:run`: serves the JavaScript application at http://localhost:8080 via a local Jetty server.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should
be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
