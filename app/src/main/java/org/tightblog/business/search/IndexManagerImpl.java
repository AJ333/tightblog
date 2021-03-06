/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.tightblog.business.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tightblog.business.WeblogEntryManager;
import org.tightblog.business.WebloggerStaticConfig;
import org.tightblog.business.search.tasks.AbstractTask;
import org.tightblog.business.search.tasks.IndexEntryTask;
import org.tightblog.business.search.tasks.IndexWeblogTask;
import org.tightblog.pojos.Weblog;
import org.tightblog.pojos.WeblogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lucene implementation of IndexManager. This is the central entry point into
 * the Lucene searching API.
 */
public class IndexManagerImpl implements IndexManager {

    private static Logger log = LoggerFactory.getLogger(IndexManagerImpl.class);

    private DirectoryReader reader;
    private WeblogEntryManager weblogEntryManager;
    private ExecutorService serviceScheduler;

    private boolean searchEnabled = true;
    private boolean indexComments = false;
    private File indexConsistencyMarker;
    private String indexDir = null;
    private boolean inconsistentAtStartup = false;
    private ReadWriteLock rwl = new ReentrantReadWriteLock();

    /**
     * Creates a new lucene index manager. This should only be created once.
     * Creating the index manager more than once will definitely result in
     * errors.
     */
    protected IndexManagerImpl(WeblogEntryManager weManager) {
        weblogEntryManager = weManager;
        searchEnabled = WebloggerStaticConfig.getBooleanProperty("search.enabled");
        if (searchEnabled) {
            indexComments = WebloggerStaticConfig.getBooleanProperty("search.include.comments");
        }

        log.info("Lucene search enabled: {} {}", searchEnabled,
                searchEnabled ? "(If not using internal search capability, can increase performance by disabling in" +
                        " TightBlog properties file)" : "(Can be activated in TightBlog properties file)");
        if (searchEnabled) {
            log.info("Include comment text as part of blog search? {}", indexComments);
        }

        if (searchEnabled) {
            serviceScheduler = Executors.newCachedThreadPool();
            // we also need to know what our index directory is
            // Note: system property expansion is now handled by WebloggerStaticConfig
            String searchIndexDir = WebloggerStaticConfig.getProperty("search.index.dir");

            if (searchIndexDir == null) {
                throw new IllegalStateException("Check tightblog properties file -- If search.enabled = true, " +
                        "search.index.dir must also be provided.");
            }

            this.indexDir = searchIndexDir.replace('/', File.separatorChar);

            String test = indexDir + File.separator + ".index-inconsistent";
            indexConsistencyMarker = new File(test);
            log.info("search index dir: {}", indexDir);
        }
    }

    @Override
    public boolean isIndexComments() {
        return indexComments;
    }

    @Override
    public void initialize() {

        // only initialize the index if search is enabled
        if (this.searchEnabled) {
            try {
                // If inconsistency marker exists, delete index
                if (indexConsistencyMarker.exists()) {
                    log.info("Index was not closed properly with last shutdown; will be rebuilt");
                    inconsistentAtStartup = true;
                } else {
                    File makeIndexDir = new File(indexDir);
                    if (!makeIndexDir.exists()) {
                        if (makeIndexDir.mkdirs()) {
                            inconsistentAtStartup = true;
                            log.info("Index folder path {} created", makeIndexDir.getAbsolutePath());
                        } else {
                            throw new IOException("Folder path " + makeIndexDir.getAbsolutePath() + " could not be " +
                                    "created (file permission rights?)");
                        }
                    }
                }

                if (!inconsistentAtStartup && !DirectoryReader.indexExists(getIndexDirectory())) {
                    log.info("Lucene index not detected, will create");
                    inconsistentAtStartup = true;
                }

                if (inconsistentAtStartup) {
                    log.info("Generating Lucene index in the background...");
                    createIndex(getFSDirectory(true));
                    rebuildWeblogIndex();
                } else {
                    log.info("Lucene search index already available and ready for use.");
                    if (!indexConsistencyMarker.createNewFile()) {
                        throw new IOException("Could not create index consistency marker " +
                                indexConsistencyMarker.getAbsolutePath() + " (file permission rights?)");
                    }
                }

                reader = DirectoryReader.open(getIndexDirectory());

            } catch (IOException e) {
                log.error("Searching is deactivated, could not create index", e);
                searchEnabled = false;
            }
        }

    }

    @Override
    public void rebuildWeblogIndex() {
        scheduleIndexOperation(new IndexWeblogTask(this, weblogEntryManager, null, false));
    }

    @Override
    public void updateIndex(Weblog weblog, boolean remove) {
        scheduleIndexOperation(new IndexWeblogTask(this, weblogEntryManager, weblog, remove));
    }

    @Override
    public void updateIndex(WeblogEntry entry, boolean remove) {
        scheduleIndexOperation(new IndexEntryTask(weblogEntryManager, this, entry, remove));
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return rwl;
    }

    /**
     * This is the analyzer that will be used to tokenize comment text.
     *
     * @return Analyzer to be used in manipulating the database.
     */
    public static Analyzer getAnalyzer() {
        return new StandardAnalyzer();
    }

    private void scheduleIndexOperation(final AbstractTask op) {
        if (this.searchEnabled) {
            log.debug("Starting scheduled index task: {}", op.getClass().getName());
            serviceScheduler.submit(op);
        }
    }

    @Override
    public void executeIndexOperationNow(final AbstractTask op) {
        if (this.searchEnabled) {
            log.debug("Executing index task now: {}", op.getClass().getName());
            op.run();
        }
    }

    private synchronized void closeReader(DirectoryReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public synchronized IndexReader getDirectoryReader() {
        try {
            DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
            if (newReader != null) {
                closeReader(reader);
                reader = newReader;
            }
        } catch (IOException ignored) {
        }
        return reader;
    }

    /**
     * Get the directory that is used by the lucene index. This method will
     * return null if there is no index at the directory location.
     *
     * @return Directory The directory containing the index, or null if error.
     */
    @Override
    public Directory getIndexDirectory() {
        return getFSDirectory(false);
    }

    private Directory getFSDirectory(boolean delete) {
        FSDirectory directory = null;

        try {
            directory = FSDirectory.open(new File(indexDir).toPath());
            if (delete) {
                // clear old files
                String[] files = directory.listAll();
                for (String fileName : files) {
                    File file = new File(indexDir, fileName);
                    if (!file.delete()) {
                        throw new IOException("couldn't delete " + fileName);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Problem accessing index directory", e);
        }

        return directory;

    }

    private void createIndex(Directory dir) {
        IndexWriter writer = null;
        int maxTokenCount = 100;

        try {
            IndexWriterConfig config = new IndexWriterConfig(new LimitTokenCountAnalyzer(
                IndexManagerImpl.getAnalyzer(), maxTokenCount));

            writer = new IndexWriter(dir, config);
        } catch (IOException e) {
            log.error("Error creating index", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        if (searchEnabled) {
            // trigger an immediate shutdown of any backgrounded tasks
            serviceScheduler.shutdownNow();
            try {
                serviceScheduler.awaitTermination(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.debug("Exception: ", e);
            }

            if (!indexConsistencyMarker.delete()) {
                log.warn("Expected index consistency marker {} not present or otherwise could not be deleted",
                        indexConsistencyMarker.getAbsolutePath());
            }
            closeReader(reader);
        }
    }
}
