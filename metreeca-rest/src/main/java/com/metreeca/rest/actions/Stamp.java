/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.actions;


import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;


/**
 * Object building.
 *
 * <p>Maps input values to streams of derived objects.</p>
 *
 * @param <V> the type of the input values used to build derived objects
 */
public final class Stamp<V> implements Function<V, Stream<String>> {

    private static final Pattern PlaceholderPattern=Pattern.compile("(?<modifier>[\\\\%])?\\{(?<name>\\w+)}");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final String template;

    private final Map<String, Function<V, Stream<String>>> parameters=new LinkedHashMap<>();


    public Stamp(final String template) {

        if ( template == null ) {
            throw new NullPointerException("null template");
        }

        this.template=template;
    }


    public Stamp<V> value(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return values(name, v -> Stream.of(v.toString()));
    }

    public Stamp<V> value(final String name, final Object value) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return values(name, v -> Stream.of(value)
                .filter(Objects::nonNull)
                .map(Object::toString)
        );
    }

    public Stamp<V> value(final String name, final Function<V, String> expression) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        return values(name, v -> Stream.of(expression.apply(v)));
    }


    public Stamp<V> values(final String name, final Object... values) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return values(name, Stream.of(values));
    }

    public Stamp<V> values(final String name, final Collection<Object> values) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return values(name, values.stream());
    }

    public Stamp<V> values(final String name, final Stream<Object> values) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return values(name, v -> values
                .filter(Objects::nonNull)
                .map(Object::toString)
        );
    }

    public Stamp<V> values(final String name, final Function<V, Stream<String>> expression) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        parameters.put(name, expression);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Stream<String> apply(final V item) {

        return stream(item).map(this::fill);

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Produces the cartesian product of parameter values computed on an item.
     */
    private Stream<Function<String, String>> stream(final V item) {

        Stream<Function<String, String>> resolvers=Stream.of(key -> null);

        for (final Map.Entry<String, Function<V, Stream<String>>> entry : parameters.entrySet()) {

            final String name=entry.getKey();
            final Function<V, Stream<String>> expression=entry.getValue();

            resolvers=resolvers.flatMap(resolver -> requireNonNull(expression.apply(item))
                    .filter(Objects::nonNull)
                    .map(value -> key -> key.equals(name) ? value : resolver.apply(key))
            );

        }

        return resolvers;
    }

    /*
     * Fills out the template.
     */
    private String fill(final Function<String, String> resolver) {

        final StringBuilder builder=new StringBuilder(template.length());
        final Matcher matcher=PlaceholderPattern.matcher(template);

        int index=0;

        while ( matcher.find() ) {

            final String modifier=matcher.group("modifier");
            final String name=matcher.group("name");

            final String value=resolver.apply(name);

            try {

                builder.append(template, index, matcher.start()).append(
                        "\\".equals(modifier) ? matcher.group().substring(1)
                                : "%".equals(modifier) ? URLEncoder.encode(value, UTF_8.name())
                                : value != null ? value
                                : ""
                );

            } catch ( final UnsupportedEncodingException unexpected ) {
                throw new UncheckedIOException(unexpected);
            }

            index=matcher.end();
        }

        builder.append(template.substring(index));

        return builder.toString();
    }

}
