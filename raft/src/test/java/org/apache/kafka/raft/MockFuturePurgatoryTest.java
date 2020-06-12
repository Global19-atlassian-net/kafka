/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.raft;

import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.utils.MockTime;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.apache.kafka.test.TestUtils.assertFutureThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MockFuturePurgatoryTest {
    private final MockTime time = new MockTime();
    private final MockFuturePurgatory<Long> purgatory = new MockFuturePurgatory<>(time);

    @Test
    public void testCompletion() throws Exception {
        CompletableFuture<Long> future1 = new CompletableFuture<>();
        purgatory.await(future1, 500L);
        assertEquals(1, purgatory.numWaiting());

        CompletableFuture<Long> future2 = new CompletableFuture<>();
        purgatory.await(future2, 1000L);
        assertEquals(2, purgatory.numWaiting());

        purgatory.completeAll(1L);
        assertEquals(0, purgatory.numWaiting());
        assertTrue(future1.isDone());
        assertEquals(1L, future1.get().longValue());
        assertTrue(future2.isDone());
        assertEquals(1L, future2.get().longValue());
    }

    @Test
    public void testExpiration() {
        CompletableFuture<Long> future1 = new CompletableFuture<>();
        purgatory.await(future1, 500L);

        CompletableFuture<Long> future2 = new CompletableFuture<>();
        purgatory.await(future2, 500L);

        CompletableFuture<Long> future3 = new CompletableFuture<>();
        purgatory.await(future3, 1000L);

        assertEquals(3, purgatory.numWaiting());

        time.sleep(500);
        assertTrue(future1.isDone());
        assertFutureThrows(future1, TimeoutException.class);

        assertTrue(future2.isDone());
        assertFutureThrows(future2, TimeoutException.class);

        assertEquals(1, purgatory.numWaiting());

        time.sleep(500);
        assertTrue(future3.isDone());
        assertFutureThrows(future3, TimeoutException.class);
        assertEquals(0, purgatory.numWaiting());
    }

}