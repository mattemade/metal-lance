package io.itch.mattekudasai.metallance.shader

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import ktx.graphics.use

object OneBitShader {

    /*
    * Render modes:
    * 0 - black and white patterns
    * 1 - full color
    * 2 - stage-based RGB
    * 3 - black and white normalized
    * 4 - black and white randomized
    * 5 - single color
    * */

    fun ShaderProgram.update(
        renderColorMode: Int? = null,
        stage: Int? = null,
        scaleFactor: Float? = null,
        resolution: Vector2? = null,
        time: Float? = null,
        tint: Color? = null,
    ) {
        use {
            renderColorMode?.let { setUniformi("renderColor", it) }
            stage?.let { setUniformi("stage", it) }
            resolution?.let { setUniform3fv("resolution", floatArrayOf(it.x, it.y, 1.0f), 0, 3) }
            scaleFactor?.let { setUniformf("scaleFactor", it) }
            time?.let { setUniformf("time", it) }
            tint?.let { setUniform4fv("tint", floatArrayOf(it.r, it.g, it.b, 1f), 0, 4) }
        }
    }

    fun createOneBitShader(): ShaderProgram {
        val vertexShader = """
            attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
            attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
            attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
            uniform mat4 u_projTrans;
            uniform float     scaleFactor;           // viewport resolution (in pixels)
            varying vec4 v_color;
            varying vec2 v_texCoords;

            void main()
            {
               v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
               v_color.a = v_color.a * (255.0/254.0);
               v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
               gl_Position =  u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
            }
            """.trimIndent()
        val fragmentShader = """
            #ifdef GL_ES
            #define LOWP lowp
            precision mediump float;
            #else
            #define LOWP
            #endif
            varying LOWP vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform float scaleFactor;
            uniform int stage;
            uniform vec3 resolution;
            uniform int renderColor;
            uniform float time;
            uniform vec4 tint;

            float stripes(float value, float position) {
              float factor = scaleFactor + 4.0; // allow 4 extra gradient values
              float modulus = mod(position, factor) / factor;
              float distance = min(modulus, 1.0 - modulus); // 0 to 1

              return value < sqrt(distance) ? 0.0 : 1.0;
            }

            float verticalStripes(float value, vec2 coord) {
              return stripes(value, coord.y);
            }

            float horizontalStripes(float value, vec2 coord) {
              return stripes(value, coord.x);
            }

            float slashStripes(float value, vec2 coord) {
              return stripes(value, coord.x + coord.y);
            }

            float backslashStripes(float value, vec2 coord) {
              return stripes(value, coord.x - coord.y);
            }

            float normalize(float value, vec2 fragCoord) {
              return verticalStripes(value, fragCoord) + horizontalStripes(value, fragCoord) > 0.5 ? 1.0 : 0.0;
            }

            float rand(vec2 co, float extra) {
                return fract(sin(dot(co * mod(time * extra, 0.1), vec2(12.9898, 78.233))) * 43758.5453);
            }

            float rand(vec2 co) {
                return rand(co, 1.0);
            }

            float normalizeBySum(float value, vec2 fragCoord) {
              return value > 0.0 ? (mod(fragCoord.x + fragCoord.y, 1.0/value) > 1.0 ? 0.0 : 1.0) : 0.0;
            }

            float normalizeByFactor(float value, vec2 fragCoord) {
              return value > 0.0 ? (mod(fragCoord.x * fragCoord.y, 1.0/value) > 1.0 ? 0.0 : 1.0) : 0.0;
            }

            void main()
            {
              vec4 color = texture2D(u_texture, v_texCoords);
              vec2 fragCoord = vec2(v_texCoords.x*resolution.x, v_texCoords.y*resolution.y);
              float gray = (color.r + color.g + color.b) / 3.0;
              if (renderColor == 1) {
                gl_FragColor = color;
                return;
              } else if (renderColor == 2) {
                if (color.r == 0.0 && color.g == 0.0 && color.b == 0.0) {
                  gl_FragColor = color;
                } else if (stage == 0) {
                  gl_FragColor = vec4(normalizeBySum(color.r, fragCoord), 0.0, 0.0, 1.0);
                } else if (stage == 1) {
                  gl_FragColor = vec4(0.0, normalizeBySum(color.g, fragCoord), 0.0, 1.0);
                } else if (stage == 2) {
                  gl_FragColor = vec4(0.0, 0.0, normalizeBySum(color.b, fragCoord), 1.0);
                }
                return;
              } else if (renderColor == 3) {
                float bwColor = normalize(gray, fragCoord);
                gl_FragColor = vec4(bwColor, bwColor, bwColor, 1.0);
                return;
              } else if (renderColor == 4) {
                if (color.r == 0.0 && color.g == 0.0 && color.b == 0.0 || color.r == 1.0 && color.g == 1.0 && color.b == 1.0) {
                  gl_FragColor = color * tint;
                } else {
                  float result = rand(fragCoord) > (1.0 - gray*gray) ? 1.0 : 0.0;
                  gl_FragColor = vec4(result, result, result, 1.0) * tint;
                }
                return;
              }
              float bwColor = time;
              if (color.r == 0.0 && color.g == 0.0 && color.b == 0.0) {
                bwColor = 0.0;
              } else if (color.r == 1.0 && color.g == 1.0 && color.b == 1.0) {
                bwColor = 1.0;
              } else if (color.r == color.g && color.g == color.b) {
                bwColor = normalizeByFactor(gray, fragCoord);
              } else if (color.r > color.g && color.r > color.b) {
                bwColor = verticalStripes(gray, fragCoord);
              } else if (color.g > color.r && color.g > color.b) {
                bwColor = horizontalStripes(gray, fragCoord);
              } else if (color.b > color.r && color.b > color.g) {
                bwColor = slashStripes(gray, fragCoord);
              } else if (color.r == color.g) {
                bwColor = backslashStripes(gray, fragCoord);
              } else {
                bwColor = 0.0;
              }

              gl_FragColor = vec4(bwColor, bwColor, bwColor, 1.0);
            }
            """.trimIndent()

        val shader = ShaderProgram(vertexShader, fragmentShader)
        require(shader.isCompiled) { "Error compiling shader: " + shader.log }
        shader.update(renderColorMode = 4, stage = 0, tint = Color.WHITE)
        return shader
    }

}
