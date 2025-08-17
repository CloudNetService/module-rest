/*
 * Copyright 2019-present CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.ext.rest.api.tree;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class DynamicHttpPathNode extends DefaultHttpPathNode {

  // numeric values are custom, all other follow the JS standard
  // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_expressions#advanced_searching_with_flags
  private static final Map<Character, Integer> FLAG_CHAR_TO_FLAG_VALUE = Map.of(
    '1', Pattern.UNIX_LINES,
    '2', Pattern.CANON_EQ,
    '3', Pattern.LITERAL,
    'i', Pattern.CASE_INSENSITIVE,
    'm', Pattern.MULTILINE,
    's', Pattern.DOTALL,
    'u', Pattern.UNICODE_CASE
  );

  private final boolean allowPartialMatch;
  private final Pattern validationPattern;

  public DynamicHttpPathNode(@NonNull String pathId) {
    this(pathId, null, false);
  }

  public DynamicHttpPathNode(@NonNull String pathId, @Nullable Pattern validationPattern, boolean allowPartialMatch) {
    super(pathId);
    this.allowPartialMatch = allowPartialMatch;
    this.validationPattern = validationPattern;
  }

  public static @NonNull DynamicHttpPathNode parse(
    @NonNull String pathId,
    @Nullable String validationRegex,
    @Nullable String regexFlags
  ) {
    // if the validation regex is not given there is nothing to really parse here
    if (validationRegex == null || validationRegex.isBlank()) {
      return new DynamicHttpPathNode(pathId);
    }

    // parse the pattern flags
    var patternFlags = 0;
    var allowPartialMatch = false;
    var patternFlagChars = regexFlags == null ? null : regexFlags.toCharArray();
    if (patternFlagChars != null) {
      for (var flagChar : patternFlagChars) {
        // special handling for 'g' which indicates that we want the "global" mode
        // this is not directly supported by a java pattern flag, so we do this litte workaround
        if (flagChar == 'g') {
          allowPartialMatch = true;
          continue;
        }

        // get the actual pattern flag for the input char
        var flagValue = FLAG_CHAR_TO_FLAG_VALUE.get(flagChar);
        if (flagValue == null) {
          throw new IllegalArgumentException("Invalid pattern flag " + flagChar);
        }

        patternFlags |= flagValue;
      }
    }

    // compile the pattern and return the new node
    var compiledPattern = Pattern.compile(validationRegex, patternFlags);
    return new DynamicHttpPathNode(pathId, compiledPattern, allowPartialMatch);
  }

  @Override
  public @NonNull String displayName() {
    return '{' + this.pathId + '}';
  }

  @Override
  public void unregisterPathPart(@NonNull HttpContext httpContext) {
    httpContext.request().pathParameters().remove(this.pathId);
  }

  @Override
  public boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart) {
    // check if the path part matches the required pattern, if given
    if (this.validationPattern != null) {
      var validationMatcher = this.validationPattern.matcher(pathPart);
      var matcherMatches = this.allowPartialMatch ? validationMatcher.find() : validationMatcher.matches();
      if (!matcherMatches) {
        return false;
      }
    }

    // register the path parameter and continue
    context.request().pathParameters().put(this.pathId, pathPart);
    return true;
  }

  @Override
  public int compareTo(@NonNull HttpPathNode other) {
    if (other instanceof DynamicHttpPathNode) {
      return 0;
    }

    if (other instanceof StaticHttpPathNode) {
      return 1;
    }

    return -1;
  }
}
