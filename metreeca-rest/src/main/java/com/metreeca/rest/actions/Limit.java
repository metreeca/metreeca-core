/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.actions;

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
