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

package com.metreeca.rest._services;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


/**
 * Rate limit.
 */
public final class Limit<T> implements UnaryOperator<T> {

	public static <V> Supplier<Limit<V>> limit() {
		return () -> new Limit<>(0, Duration.ZERO);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Duration period;

	private final BlockingQueue<Token> tokens=new DelayQueue<>();


	public Limit(final int txns) {
		this(txns, Duration.ofSeconds(1));
	}

	public Limit(final int txns, final Duration period) {

		if ( txns < 0 ) {
			throw new IllegalArgumentException("illegal transaction limit {"+txns+"}");
		}

		if ( period == null ) {
			throw new NullPointerException("null period");
		}

		if ( period.isNegative() ) {
			throw new IllegalArgumentException("negative period {"+period+"}");
		}

		this.period=period;

		for (int i=0; i < txns; ++i) { tokens.offer(new Token(0)); }

	}


	public T apply(final T t) {
		if ( period.isZero() ) { return t; } else {

			while ( true ) {
				try {

					tokens.take();
					tokens.offer(new Token(period.toMillis()));

					return t;

				} catch ( final InterruptedException ignored ) {}
			}

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Token implements Delayed {

		private final long time;


		private Token(final long delay) {
			time=currentTimeMillis()+delay;
		}


		@Override public long getDelay(final TimeUnit unit) {
			return unit.convert(time-currentTimeMillis(), MILLISECONDS);
		}


		@Override public int compareTo(final Delayed delayed) {
			return delayed instanceof Token
					? Long.compare(time, ((Token)delayed).time)
					: Long.compare(getDelay(MILLISECONDS), delayed.getDelay(MILLISECONDS));
		}


		@Override public int hashCode() {
			return Long.hashCode(time);
		}

		@Override public boolean equals(final Object object) {
			return object instanceof Token && time == ((Token)object).time;
		}

	}

}
