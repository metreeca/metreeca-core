/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.xml.actions;

import com.metreeca.rest.Xtream;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Site crawling.
 *
 * <p>Maps site root URLs to streams of URLs for HTML site pages.</p>
 */
public final class Crawl implements Function<String, Stream<String>> {

    // !!! honour robots.txt

    private Function<String, Stream<String>> cross=new Cross();

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
     * Configures the cross action (default to {@link Cross}).
     *
     * @param cross a function mapping page URLs to streams of URLs for linked HTML pages
     *
     * @return this action
     *
     * @throws NullPointerException if {@code cross} is null
     */
    public Crawl cross(final Function<String, Stream<String>> cross) {

        if ( cross == null ) {
            throw new NullPointerException("null cross");
        }

        this.cross=cross;

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
        return url == null || url.isEmpty() ? Xtream.empty()
                : Xtream.of(url).loop(cross).filter(link -> prune.test(url, link));
    }

}
