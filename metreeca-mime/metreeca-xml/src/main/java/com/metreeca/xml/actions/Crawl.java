/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.xml.actions;

import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.*;
import com.metreeca.xml.formats.HTMLFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.actions.Regex.Regex;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.xml.actions.XPath.XPath;
import static com.metreeca.xml.formats.HTMLFormat.html;

/**
 * Site crawling.
 *
 * <p>Maps site root URLs to streams of HTML site pages.</p>
 */
public final class Crawl implements Function<String, Stream<String>> {

    // !!! honour robots.txt

    private Fetch fetch=new Fetch();
    private Function<Node, Optional<Node>> focus=Optional::of;


    /**
     * Configures the fetch action.
     *
     * @param fetch the action used to fetch site pages
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
    public Crawl focus(final Function<Node, Optional<Node>> focus) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        this.focus=focus;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Crawls a site.
     *
     * @param url the root URL of the site to be crawled; ignored if null
     *
     * @return a stream of links to nested HTML pages reachable from the root {@code url}
     */
    @Override public Xtream<String> apply(final String url) {
        if ( url == null ) { return Xtream.empty(); } else {
            try {

                final URI origin=new URI(url).normalize();

                return Xtream.of(url).loop(xtream -> xtream

                        .filter(link -> Xtream.of(link)

                                .optMap(new Query(request -> request.method(HEAD)))
                                .optMap(fetch)

                                .allMatch(response -> response.header("Content-Type")
                                        .filter(HTMLFormat.MIMEPattern.asPredicate())
                                        .isPresent()
                                )

                        )

                        .optMap(new Query())
                        .optMap(fetch)
                        .optMap(new Parse<>(html())) // !!! support xhtml

                        .map(Document::getDocumentElement)

                        .optMap(focus)

                        .flatMap(node -> Xtream.of(node)

                                .flatMap(XPath(p -> p.links("//html:a/@href")))
                                .map(Regex(r -> r.replace("#.*$", ""))) // remove anchors

                                .filter(link -> { // keep only nested resources
                                    try {

                                        final URI source=new URI(node.getBaseURI()).normalize(); // !!! nulls
                                        final URI target=new URI(link).normalize();

                                        return /* !!! origin.equals(source) ||*/
                                                !origin.relativize(target).equals(target);

                                    } catch ( final URISyntaxException e ) {

                                        return false;

                                    }
                                })
                        )

                );

            } catch ( final URISyntaxException e ) {

                service(logger()).warning(this, String.format("malformed URL <%s>>", url));

                return Xtream.empty();

            }
        }
    }

}
