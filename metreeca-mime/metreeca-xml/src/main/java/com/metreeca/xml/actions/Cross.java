/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.xml.actions;

import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.*;
import com.metreeca.xml.formats.HTMLFormat;

import org.w3c.dom.Node;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.actions.Regex.Regex;
import static com.metreeca.xml.actions.XPath.XPath;
import static com.metreeca.xml.formats.HTMLFormat.html;

/**
 * Page link crossing.
 *
 * <p>Maps page URLs to streams of URLs for linked HTML pages.</p>
 */
public final class Cross implements Function<String, Stream<String>> {

    private Fetch fetch=new Fetch();
    private Function<? super Node, Optional<Node>> focus=Optional::of;


    /**
     * Configures the fetch action (default to {@link Fetch}.
     *
     * @param fetch the action used to fetch pages
     *
     * @return this action
     *
     * @throws NullPointerException if {@code fetch} is null
     */
    public Cross fetch(final Fetch fetch) {

        if ( fetch == null ) {
            throw new NullPointerException("null fetch");
        }

        this.fetch=fetch;

        return this;
    }

    /**
     * Configures the content focus (default to the identity function).
     *
     * @param focus a function taking as argument an element and returning an optional partial/restructured focus
     *              element, if one was identified, or an empty optional, otherwise
     *
     * @return this action
     *
     * @throws NullPointerException if {@code focus} is null
     */
    public Cross focus(final Function<? super Node, Optional<Node>> focus) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        this.focus=focus;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Crosses a page.
     *
     * @param url the url URL of the page to be crossed
     *
     * @return a stream of URLs for linked HTML pages; empty if {@code url} is null or empty
     */
    public Xtream<String> apply(final String url) {
        return url == null || url.isEmpty() ? Xtream.empty() : Xtream.of(url)

                .filter(link -> Xtream.of(link)

                        .optMap(new Query(request -> request.method(HEAD)))
                        .optMap(fetch)

                        .allMatch(response -> response
                                .header("Content-Type")
                                .filter(HTMLFormat.MIMEPattern.asPredicate())
                                .isPresent()
                        )

                )

                .optMap(new Query())
                .optMap(fetch)

                .optMap(new Parse<>(html())) // !!! support xhtml
                .optMap(focus)

                .flatMap(XPath(p -> p.links("//html:a/@href")))
                .map(Regex(r -> r.replace("#.*$", "")));
    }

}
