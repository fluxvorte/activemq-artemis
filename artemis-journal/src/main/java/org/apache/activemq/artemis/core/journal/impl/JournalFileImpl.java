/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.core.journal.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.core.io.SequentialFile;

public class JournalFileImpl implements JournalFile {

   private final SequentialFile file;

   private final long fileID;

   private final int recordID;

   private long offset;

   private final AtomicInteger posCount = new AtomicInteger(0);

   private final AtomicInteger liveBytes = new AtomicInteger(0);

   private boolean canReclaim;

   private final AtomicInteger totalNegativeToOthers = new AtomicInteger(0);

   private final int version;

   private final Map<JournalFile, AtomicInteger> negCounts = new ConcurrentHashMap<JournalFile, AtomicInteger>();

   public JournalFileImpl(final SequentialFile file, final long fileID, final int version) {
      this.file = file;

      this.fileID = fileID;

      this.version = version;

      recordID = (int) (fileID & Integer.MAX_VALUE);
   }

   @Override
   public int getPosCount() {
      return posCount.intValue();
   }

   @Override
   public boolean isCanReclaim() {
      return canReclaim;
   }

   @Override
   public void setCanReclaim(final boolean canReclaim) {
      this.canReclaim = canReclaim;
   }

   @Override
   public void incNegCount(final JournalFile file) {
      if (file != this) {
         totalNegativeToOthers.incrementAndGet();
      }
      getOrCreateNegCount(file).incrementAndGet();
   }

   @Override
   public int getNegCount(final JournalFile file) {
      AtomicInteger count = negCounts.get(file);

      if (count == null) {
         return 0;
      }
      else {
         return count.intValue();
      }
   }

   @Override
   public int getJournalVersion() {
      return version;
   }

   @Override
   public void incPosCount() {
      posCount.incrementAndGet();
   }

   @Override
   public void decPosCount() {
      posCount.decrementAndGet();
   }

   public long getOffset() {
      return offset;
   }

   @Override
   public long getFileID() {
      return fileID;
   }

   @Override
   public int getRecordID() {
      return recordID;
   }

   public void setOffset(final long offset) {
      this.offset = offset;
   }

   @Override
   public SequentialFile getFile() {
      return file;
   }

   @Override
   public String toString() {
      try {
         return "JournalFileImpl: (" + file.getFileName() + " id = " + fileID + ", recordID = " + recordID + ")";
      }
      catch (Exception e) {
         e.printStackTrace();
         return "Error:" + e.toString();
      }
   }

   /**
    * Receive debug information about the journal
    */
   public String debug() {
      StringBuilder builder = new StringBuilder();

      for (Entry<JournalFile, AtomicInteger> entry : negCounts.entrySet()) {
         builder.append(" file = " + entry.getKey() + " negcount value = " + entry.getValue() + "\n");
      }

      return builder.toString();
   }

   private synchronized AtomicInteger getOrCreateNegCount(final JournalFile file) {
      AtomicInteger count = negCounts.get(file);

      if (count == null) {
         count = new AtomicInteger();
         negCounts.put(file, count);
      }

      return count;
   }

   @Override
   public void addSize(final int bytes) {
      liveBytes.addAndGet(bytes);
   }

   @Override
   public void decSize(final int bytes) {
      liveBytes.addAndGet(-bytes);
   }

   @Override
   public int getLiveSize() {
      return liveBytes.get();
   }

   @Override
   public int getTotalNegativeToOthers() {
      return totalNegativeToOthers.get();
   }

}
