package hu.blackbelt.flutter.maven.plugin.api;

/*-
 * #%L
 * flutter-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ArgumentsParser {

    private final List<String> additionalArguments;

    ArgumentsParser() {
        this(Collections.<String>emptyList());
    }

    public ArgumentsParser(List<String> additionalArguments) {
        this.additionalArguments = additionalArguments;
    }

    /**
     * Parses a given string of arguments, splitting it by characters that are whitespaces according to {@link Character#isWhitespace(char)}.
     * <p>
     * This method respects quoted arguments. Meaning that whitespaces appearing phrases that are enclosed by an opening
     * single or double quote and a closing single or double quote or the end of the string will not be considered.
     * <p>
     * All characters excluding whitespaces considered for splitting stay in place.
     * <p>
     * Examples:
     * "foo bar" will be split to ["foo", "bar"]
     * "foo \"bar foobar\"" will be split to ["foo", "\"bar foobar\""]
     * "foo 'bar" will be split to ["foo", "'bar"]
     *
     * @param args a string of arguments
     * @return an mutable copy of the list of all arguments
     */
    public List<String> parse(String args) {
        if (args == null || "null".equals(args) || args.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> arguments = new LinkedList<>();
        final StringBuilder argumentBuilder = new StringBuilder();
        Character quote = null;

        for (int i = 0, l = args.length(); i < l; i++) {
            char c = args.charAt(i);

            if (Character.isWhitespace(c) && quote == null) {
                addArgument(argumentBuilder, arguments);
                continue;
            } else if (c == '"' || c == '\'') {
                // explicit boxing allows us to use object caching of the Character class
                Character currentQuote = Character.valueOf(c);
                if (quote == null) {
                    quote = currentQuote;
                } else if (quote.equals(currentQuote)){
                    quote = null;
                } // else
                // we ignore the case when a quoted argument contains the other kind of quote
            }

            argumentBuilder.append(c);
        }

        addArgument(argumentBuilder, arguments);

        for (String argument : this.additionalArguments) {
            addArgument(argument, arguments);
        }

        return new ArrayList<>(arguments);
    }

    private static void addArgument(StringBuilder argumentBuilder, List<String> arguments) {
        if (argumentBuilder.length() > 0) {
            String argument = argumentBuilder.toString();
            addArgument(argument, arguments);
            argumentBuilder.setLength(0);
        }
    }

    private static void addArgument(String argument, List<String> arguments) {
        if (!arguments.contains(argument)) {
            arguments.add(argument);
        }
    }
}
