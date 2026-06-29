package com.progressiveblur

internal object ProgressiveBlurShaders {

    val HORIZONTAL_BLUR_SKSL = """
        uniform shader content;
        uniform float blurRadius;
        uniform vec4 crop;
        uniform shader mask;

        const half maxRadius = 150.0;

        float gaussian(float x, float sigma) {
          return exp(-(x * x) / (2.0 * sigma * sigma));
        }

        vec4 blur(vec2 coord, float radius) {
          half r = floor(radius + 0.5);

          float sigma = max(radius / 2.0, 1.0);
          float weightSum = 1.0;
          vec4 result = content.eval(coord);

          for (half i = 1.0; i < maxRadius; i += 2.0) {
            if (i >= r) { break; }

            float weightL = gaussian(i, sigma);
            float weightH = gaussian(i + 1.0, sigma);
            float weight = weightL + weightH;
            vec2 offset = vec2(i + weightH / weight, 0.0);

            vec2 newCoord = coord - offset;
            if (newCoord.x >= crop[0] && newCoord.y >= crop[1]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop[2] && newCoord.y <= crop[3]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }
          }

          if (r < maxRadius && mod(r, 2.0) == 1.0) {
            float weight = gaussian(r, sigma);
            vec2 offset = vec2(r, 0.0);

            vec2 newCoord = coord - offset;
            if (newCoord.x >= crop[0] && newCoord.y >= crop[1]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop[2] && newCoord.y <= crop[3]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }
          }

          return result / weightSum;
        }

        vec4 main(vec2 coord) {
          vec2 maskCoord = max(coord - crop.xy, vec2(0.0, 0.0));
          float intensity = mask.eval(maskCoord).a;
          return blur(coord, mix(0.0, blurRadius, intensity));
        }
    """.trimIndent()

    val VERTICAL_BLUR_SKSL = """
        uniform shader content;
        uniform float blurRadius;
        uniform vec4 crop;
        uniform shader mask;

        const half maxRadius = 150.0;

        float gaussian(float x, float sigma) {
          return exp(-(x * x) / (2.0 * sigma * sigma));
        }

        vec4 blur(vec2 coord, float radius) {
          half r = floor(radius + 0.5);

          float sigma = max(radius / 2.0, 1.0);
          float weightSum = 1.0;
          vec4 result = content.eval(coord);

          for (half i = 1.0; i < maxRadius; i += 2.0) {
            if (i >= r) { break; }

            float weightL = gaussian(i, sigma);
            float weightH = gaussian(i + 1.0, sigma);
            float weight = weightL + weightH;
            vec2 offset = vec2(0.0, i + weightH / weight);

            vec2 newCoord = coord - offset;
            if (newCoord.x >= crop[0] && newCoord.y >= crop[1]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop[2] && newCoord.y <= crop[3]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }
          }

          if (r < maxRadius && mod(r, 2.0) == 1.0) {
            float weight = gaussian(r, sigma);
            vec2 offset = vec2(0.0, r);

            vec2 newCoord = coord - offset;
            if (newCoord.x >= crop[0] && newCoord.y >= crop[1]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop[2] && newCoord.y <= crop[3]) {
              result += weight * content.eval(newCoord);
              weightSum += weight;
            }
          }

          return result / weightSum;
        }

        vec4 main(vec2 coord) {
          vec2 maskCoord = max(coord - crop.xy, vec2(0.0, 0.0));
          float intensity = mask.eval(maskCoord).a;
          return blur(coord, mix(0.0, blurRadius, intensity));
        }
    """.trimIndent()
}
