/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.xml.actions;

import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.*;
import com.metreeca.xml.formats.HTMLFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    private Function<Element, Optional<Element>> pruner=Optional::of;


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
     * Configures the content pruner (default to the identity function).
     *
     * @param pruner a function taking as argument an element and returning an optional partial/restructured target
     *               element, if one was identified, or an empty optional, otherwise
     *
     * @return this action
     *
     * @throws NullPointerException if {@code pruner} is null
     */
    public Crawl pruner(final Function<Element, Optional<Element>> pruner) {

        if ( pruner == null ) {
            throw new NullPointerException("null pruner");
        }

        this.pruner=pruner;

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

                final URI root=new URI(url).normalize();

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

                        .optMap(pruner)

                        .flatMap(XPath(p -> p.links("//html:a/@href")))
                        .map(Regex(r -> r.replace("#.*$", ""))) // remove anchors

                        .filter(link -> { // keep only nested resources
                            try {

                                return !root.relativize(new URI(link)).toString().equals(link);

                            } catch ( final URISyntaxException e ) {

                                return false;

                            }
                        })

                );

            } catch ( final URISyntaxException e ) {

                service(logger()).warning(this, String.format("malformed URL <%s>>", url));

                return Xtream.empty();

            }
        }
    }

}
