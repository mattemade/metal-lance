# Metal Lance

Game made for [1-bit jam #3](https://itch.io/jam/1-bit-jam-three/rate/2633292).

[Music tracks](assets/music) are made by nicole starlight:

* https://nicolestarlight.itch.io/
* https://soundcloud.com/nicolestarlight

Animated assets ([power-up](assets/texture/upgrade/animated)) are made by artist who gave me permission to use them but
didn't give permission to mention their name. Maybe one day they will reveal themselves.

Planned to be a mixture of beat 'em up and bullet hell scroll shooter.

Jam theme is "transformation", and I am going to interpret it without any creativity in mind: the main character is a
robot that can transform, hence such a mix of genres.

Tools used:

* [libGDX](https://github.com/libgdx/libgdx) with [TeaVM](https://github.com/konsoletyper/teavm) for code (all the code
  is GNU)
* [GIMP](https://github.com/GNOME/gimp) for images
* [LMMS](https://github.com/LMMS/lmms) for sound effects

Licenses:

* all the code is covered by GPL-3.0 license (see [LICENSE-CODE](LICENSE-CODE))
* all non-code assets, such as visual and audio resources, are covered by CC-BY-4.0 license (
  see [LICENSE-ASSETS](LICENSE-CODE))

## Section-based progress

| Section    | Dirty | Clean | Music | SFX | Art |
|------------|-------|-------|-------|-----|-----|
| Intro      | 100   | 0     | 0     | 50  | 0   |
| Title      | 100   | 10    | 100   | 0   | 10  |
| Tutorial   | 100   | 95    | 0     | 100 | 100 |
| Stage 1    | 100   | 1     | 100   | 0   | 25  |
| Boss 1     | 100   | 1     | 0     | 0   | 25  |
| Stage 2    | 100   | 0     | 100   | 0   | 0   |
| Boss 2     | 100   | 0     | 100   | 0   | 0   |
| Stage 3    | 100   | 0     | 100   | 0   | 0   |
| Final boss | 100   | 0     | 0     | 0   | 0   |
| Game over  | 100   | 0     | 0     | 0   | 0   |
| Outro      | 100   | 0     | 0     | 0   | 0   |

## Dev log

| Day | Date       | Wkdy | MAX | Plan                                                                                       | Spnt | Done                                                                                                                                                                                                                                                                                                                   |
|-----|------------|------|-----|--------------------------------------------------------------------------------------------|------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | 04/04/2024 | THU  | 1-2 |                                                                                            | 1    | Conceptualized the game                                                                                                                                                                                                                                                                                                |
| 2   | 05/04/2024 | FRI  | 1-2 |                                                                                            | 1    | Explored the math of colliding function graphs a bit, decided to make everything as simple as possible (only use linear frame-time collision)                                                                                                                                                                          |
| 3   | 06/04/2024 | SAT  | 1-2 | Init repo, create the plan                                                                 | 2    | As planned + basic playable character + shooting                                                                                                                                                                                                                                                                       |
| 4   | 07/04/2024 | SUN  | 4-6 | Basic shmup character and controls (move, shoot, bomb, shield), basic moving enemy         | 2    | Fixed shooting, enemies that shoots back at controllable trajectory                                                                                                                                                                                                                                                    |
| 5   | 08/04/2024 | MON  | 4-6 | Character shoots, enemy shoots bullets (t -> vec2), collisions, upgrades                   | 3    | Basic collision checks, some refactorings, power ups, death and bombs                                                                                                                                                                                                                                                  |
| 6   | 09/04/2024 | TUE  | 4-6 | Start screen, intro sequence (add some story), shape and shot upgrades                     | 3    | Changing plans, create artist's build with overridable resources, added dummy intro and title screens, ship shape upgrades, organized assets                                                                                                                                                                           |
| 7   | 10/04/2024 | WED  | 4-6 | First stage of ruined city, enemy shooting patterns, enemy scheduling                      | 5    | Level scripting, tutorial sequence, some refactorings, runtime render mode via shader                                                                                                                                                                                                                                  |
| 8   | 11/04/2024 | THU  | 4-6 | Expand the scheduling engine (add enemies and shooting patterns, template moving patterns) | 8    | Pause and auto-pause, music playback from level script, music for stages 1 and 2, refactorings, testing animated upgrades, refined tutorial and start screens, added some city environment, refactored and declared some shooting patterns, completed the game flow (though transitions are missing), a bunch of fixes |
| 9   | 12/04/2024 | FRI  | 4-6 | Finish the scheduling engine                                                               | 5    | Finished (not really) scheduling engine, refined power-ups, enemies and bombs                                                                                                                                                                                                                                          |
| 10  | 13/04/2024 | SAT  | 4-6 | Layout the first stage                                                                     | 5    | Fixed a nasty shader bug, advancing the level engine, simplified describing shooting patterns, layout for a part of the 1st stage, finished the tutorial                                                                                                                                                               |
| 11  | 14/04/2024 | SUN  | 4-6 | Layout enemy schedule on all three stages, simplified bosses for 1-st and 2-nd stages      | 4    | Tempo sync in level engine and shooting patterns, made half of level 1, but it should be easy from now on; prepared the rhythm for level 2 and 3; basic HUD; easy mode; fade effects for game screen                                                                                                                   |
| 12  | 15/04/2024 | MON  | 1-2 | Finish level 1 layout, start level 2                                                       |      |                                                                                                                                                                                                                                                                                                                        |
| 13  | 16/04/2024 | TUE  | 1-2 | Finish level 2 and level 3 layouts                                                         |      |                                                                                                                                                                                                                                                                                                                        |
| 14  | 17/04/2024 | WED  | 1-2 | Final boss                                                                                 |      |                                                                                                                                                                                                                                                                                                                        |
| 15  | 18/04/2024 | THU  | 1-2 | Final boss                                                                                 |      |                                                                                                                                                                                                                                                                                                                        |
| 16  | 19/04/2024 | FRI  | 1-2 | Final boss                                                                                 |      |                                                                                                                                                                                                                                                                                                                        |
| 17  | 20/04/2024 | SAT  | 1-2 | Final touches, submission at 16:00                                                         |      |                                                                                                                                                                                                                                                                                                                        |

## Behind the schedule

* lance strike
* levels
* HUD
    * Body modificator: normal (one-shot), metal (health bar)
    * Cannon modificator: lance, (charge bar)
    * Shot modificator: blaster, rocket, bomb
* shot upgrades (rocket, bomb) ??
* transitions (at least fade out / fade in)
    * between intro pictures
    * between levels
* level environments (lightning strikes, background/foreground objects, etc.)

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
