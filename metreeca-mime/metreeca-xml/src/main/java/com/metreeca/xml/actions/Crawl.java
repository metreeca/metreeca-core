/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.xml.actions;

import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.*;
import com.metreeca.xml.formats.HTMLFormat;

import org.w3c.dom.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.actions.Regex.Regex;
import static com.metreeca.xml.actions.XPath.XPath;
import static com.metreeca.xml.formats.HTMLFormat.html;

/**
 * Site crawling.
 *
 * <p>Maps site root URLs to streams of URLs for HTML site pages.</p>
 */
public final class Crawl implements Function<String, Stream<String>> {

    // !!! honour robots.txt

    private Fetch fetch=new Fetch();

    private Function<? super Node, Optional<Node>> focus=Optional::of;

    private BiPredicate<String, String> prune=(root, link) -> { // keep only nested resources
        try {

            final URI origin=new URI(root).normalize();
            final URI target=new URI(link).normalize();

            return !origin.relativize(target).equals(target);

        } catch ( final URISyntaxException e ) {

            return false;

        }
    };


    /**
     * Configures the fetch action (default to {@link Fetch}.
     *
     * @param fetch the action used to fetch pages
     *
     * @return this action
     *
     * @throws NullPointerException if {@code fetch} is null
     */
    public Crawl fetch(final Fetch fetch) {

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
    public Crawl focus(final Function<? super Node, Optional<Node>> focus) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        this.focus=focus;

        return this;
    }

    /**
     * Configures the prune action (default to accepting URLs nested under the site root URL).
     *
     * @param prune a bi-predicate taking as arguments the site root URL and a link URL and returning {@code true} if
     *              the link targets a site page or {@code false} otherwise
     *
     * @return this action
     *
     * @throws NullPointerException if {@code prune} is null
     */
    public Crawl prune(final BiPredicate<String, String> prune) {

        if ( prune == null ) {
            throw new NullPointerException("null prune");
        }

        this.prune=prune;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Crawls a site.
     *
     * @param url the root URL of the site to be crawled
     *
     * @return a stream of links to HTML pages reachable from the root {@code url}; empty if {@code url} is null or
     * empty
     */
    @Override public Xtream<String> apply(final String url) {
        return url == null || url.isEmpty() ? Xtream.empty() : Xtream.of(url).loop(page -> Xtream

                .of(page)

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
                .map(Regex(r -> r.replace("#.*$", "")))

                .filter(link -> prune.test(url, link))

        );
    }

}
