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

package com.metreeca.core.actions;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


/**
 * Event rate limiting.
 *
 * <p>Enforces a user-defined rate-limit to event processing.</p>
 *
 * @param <T> the type of the rate-limited events
 */
public final class Limit<T> implements UnaryOperator<T> {

    private final Duration period;

    private final BlockingQueue<Token> tokens=new DelayQueue<>();


    /**
     * Creates a new per second rate limit.
     *
     * @param events the number of accepted events in a second.
     *
     * @throws IllegalArgumentException if {@code events} is negative
     */
    public Limit(final int events) {
        this(events, Duration.ofSeconds(1));
    }

    /**
     * Creates a new rate limit.
     *
     * @param events the number of accepted events in the given {@code period}; no rate limit is enforced if equal to 0
     * @param period the time window for counting events}; no rate limit is enforced if {@linkplain Duration#isZero()}
     *               equal to 0}
     *
     * @throws NullPointerException     if {@code period} is null
     * @throws IllegalArgumentException if either {@code events} or {@code period} is negative
     */
    public Limit(final int events, final Duration period) {

        if ( events < 0 ) {
            throw new IllegalArgumentException("negative transaction limit");
        }

        if ( period == null ) {
            throw new NullPointerException("null period");
        }

        if ( period.isNegative() ) {
            throw new IllegalArgumentException("negative period");
        }

        this.period=period;

        for (int i=0; i < events; ++i) { tokens.offer(new Token(0)); }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns an event as soon as it is compatible with the enforced rate limits.
     *
     * @param event an event to be accepted
     *
     * @return the input {@code event}
     */
    @Override public T apply(final T event) {
        if ( tokens.isEmpty() || period.isZero() ) { return event; } else {

            while ( true ) {
                try {

                    tokens.take();
                    tokens.offer(new Token(period.toMillis()));

                    return event;

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
