/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.xml.actions;

import com.metreeca.rest.*;
import com.metreeca.rest.actions.*;
import com.metreeca.xml.formats.HTMLFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.actions.Regex.Regex;
import static com.metreeca.xml.actions.XPath.XPath;
import static com.metreeca.xml.formats.HTMLFormat.html;
import static java.lang.Runtime.getRuntime;

/**
 * Site crawling.
 *
 * <p>Maps site root URLs to streams of URLs for HTML site pages.</p>
 */
public final class Crawl implements Function<String, Stream<String>> {

	// !!! inline after linking context to threads in the execution service

	private final Function<String, Optional<Request>> head=new Query(request -> request.method(HEAD));
	private final Function<String, Optional<Request>> get=new Query();

	private final Function<Message<?>, Optional<Document>> parse=new Parse<>(html()); // !!! support xhtml


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// !!! honour robots.txt
	// !!! session state


	private Fetch fetch=new Fetch();

	private Function<? super Node, Optional<Node>> focus=Optional::of;
	private BiPredicate<String, String> prune=(root, link) -> true;

	private int threads=0;
	private int timeout=0;

	private TimeUnit unit=TimeUnit.SECONDS;


	/**
	 * Configures the fetch action (defaults to {@link Fetch}.
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
	 * Configures the content focus action (defaults to the identity function).
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
	 * Configures the prune action (defaults to always pass).
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


	/**
	 * Configures the number of concurrent requests (defaults to the number of processors)
	 *
	 * @param threads the maximum number of concurrent resource fetches; limited to the number of system processors if
	 *                equal to zero
	 *
	 * @return this action
	 *
	 * @throws IllegalArgumentException if {@code threads} is negative
	 */
	public Crawl threads(final int threads) {

		if ( threads < 0 ) {
			throw new IllegalArgumentException("negative thread count");
		}

		this.threads=threads;

		return this;
	}

	/**
	 * Configures the request timeout (defaults to no limit)
	 *
	 * @param timeout the request timeout in {@code unit}; no timeout is enforced if equal to 0
	 * @param unit    the time unit for {@code timeout}
	 *
	 * @return this action
	 *
	 * @throws IllegalArgumentException if {@code timeout} is negative
	 * @throws NullPointerException     if {@code unit]} is null
	 */
	public Crawl timeout(final int timeout, final TimeUnit unit) {

		if ( timeout < 0 ) {
			throw new IllegalArgumentException("negative timeout");
		}

		if ( unit == null ) {
			throw new NullPointerException("null unit");
		}

		this.timeout=timeout;
		this.unit=unit;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Crawls a site.
	 *
	 * @param root the root URL of the site to be crawled
	 *
	 * @return a stream of links to nested HTML pages reachable from the root {@code root}; empty if {@code root} is
	 * null or empty
	 */
	@Override public Stream<String> apply(final String root) {
		if ( root == null || root.isEmpty() ) { return Stream.empty(); } else {
			try {

				final Set<String> visited=Collections.newSetFromMap(new ConcurrentHashMap<>());

				// !!! custom thread factory for linking context

				final ExecutorService pool=new ForkJoinPool(threads > 0 ? threads :
						getRuntime().availableProcessors());

				pool.execute(() -> action(root, root, visited).fork());
				pool.shutdown();
				pool.awaitTermination(timeout > 0 ? timeout : Long.MAX_VALUE, unit);

				return visited.stream();

			} catch ( final InterruptedException e ) {

				throw new RuntimeException(e);

				//return Stream.empty(); // !!! review

			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RecursiveAction action(final String root, final String page, final Set<String> visited) {
		return new RecursiveAction() {
			@Override protected void compute() {
				if ( visited.add(page) ) {

					Xtream

							.of(page)

							.filter(link -> Xtream.of(link)

									.optMap(head)
									.optMap(fetch)

									.allMatch(response -> response
											.header("Content-Type")
											.filter(HTMLFormat.MIMEPattern.asPredicate())
											.isPresent()
									)

							)

							.optMap(get)
							.optMap(fetch)

							.optMap(parse)
							.optMap(focus)

							.flatMap(XPath(p -> p.links("//html:a/@href")))

							.map(Regex(r -> r.replace("#.*$", ""))) // remove anchor
							.map(Regex(r -> r.replace("\\?.*$", ""))) // remove query // !!! review

							.filter(link -> { // keep only nested resources
								try {

									final URI origin=new URI(root).normalize();
									final URI target=new URI(link).normalize();

									return !origin.relativize(target).equals(target);

								} catch ( final URISyntaxException e ) {

									return false;

								}
							})

							.filter(link -> prune.test(root, link))

							.forEach(link -> action(root, link, visited).fork());

				}
			}
		};
	}

}
