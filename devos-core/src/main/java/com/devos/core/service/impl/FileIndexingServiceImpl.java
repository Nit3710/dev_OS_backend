package com.devos.core.service.impl;

import com.devos.core.domain.entity.Project;
import com.devos.core.repository.ProjectRepository;
import com.devos.core.service.FileIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileIndexingServiceImpl implements FileIndexingService {

    private final ProjectRepository projectRepository;

    @Value("${devos.indexing.path:.devos/index}")
    private String indexPath;

    @Value("${devos.indexing.enabled:true}")
    private boolean indexingEnabled;

    @Override
    @Transactional
    public void indexProject(Long projectId) {
        if (!indexingEnabled) return;
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        log.info("Starting full indexing for project: {} ({})", project.getName(), projectId);
        
        try (Directory directory = FSDirectory.open(Paths.get(indexPath, projectId.toString()));
             Analyzer analyzer = new StandardAnalyzer()) {
            
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // Re-create index
            
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                File projectDir = new File(project.getLocalPath());
                if (!projectDir.exists()) {
                    log.error("Project path does not exist: {}", project.getLocalPath());
                    return;
                }

                Collection<File> files = FileUtils.listFiles(projectDir, null, true);
                for (File file : files) {
                    if (isIndexable(file)) {
                        indexFile(writer, project.getLocalPath(), file);
                    }
                }
            }
            
            project.setIsIndexed(true);
            project.setLastIndexedAt(LocalDateTime.now());
            projectRepository.save(project);
            
            log.info("Completed indexing for project: {}", projectId);
        } catch (IOException e) {
            log.error("Error indexing project: {}", projectId, e);
        }
    }

    @Override
    @Transactional
    public void indexProjectFiles(Long projectId) {
        // For simplicity, we just do a full index for now as per indexProject
        indexProject(projectId);
    }

    @Override
    public Map<String, Object> searchInProject(Long projectId, String queryStr) {
        log.info("Searching in project: {} with query: {}", projectId, queryStr);
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Directory directory = FSDirectory.open(Paths.get(indexPath, projectId.toString()));
             IndexReader reader = DirectoryReader.open(directory)) {
            
            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(queryStr);
            
            TopDocs topDocs = searcher.search(query, 50);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Map<String, Object> result = new HashMap<>();
                result.put("path", doc.get("path"));
                result.put("score", scoreDoc.score);
                results.add(result);
            }
            
            return Map.of(
                    "projectId", projectId,
                    "query", queryStr,
                    "results", results,
                    "totalHits", topDocs.totalHits.value
            );
        } catch (Exception e) {
            log.error("Error searching in project: {}", projectId, e);
            return Map.of("error", e.getMessage(), "results", List.of());
        }
    }

    @Override
    @Transactional
    public void updateIndex(Long projectId, String filePath, String content) {
        if (!indexingEnabled) return;

        try (Directory directory = FSDirectory.open(Paths.get(indexPath, projectId.toString()));
             Analyzer analyzer = new StandardAnalyzer()) {
            
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                Document doc = new Document();
                doc.add(new StringField("path", filePath, Field.Store.YES));
                doc.add(new TextField("content", content, Field.Store.NO));
                
                writer.updateDocument(new Term("path", filePath), doc);
            }
        } catch (IOException e) {
            log.error("Error updating index for project: {}, file: {}", projectId, filePath, e);
        }
    }

    @Override
    @Transactional
    public void removeFromIndex(Long projectId, String filePath) {
        if (!indexingEnabled) return;

        try (Directory directory = FSDirectory.open(Paths.get(indexPath, projectId.toString()));
             Analyzer analyzer = new StandardAnalyzer()) {
            
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteDocuments(new Term("path", filePath));
            }
        } catch (IOException e) {
            log.error("Error removing from index for project: {}, file: {}", projectId, filePath, e);
        }
    }

    private void indexFile(IndexWriter writer, String projectRoot, File file) throws IOException {
        String relativePath = Paths.get(projectRoot).relativize(file.toPath()).toString();
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        
        Document doc = new Document();
        doc.add(new StringField("path", relativePath, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.NO)); // Content is indexed but not stored to save space
        
        writer.updateDocument(new Term("path", relativePath), doc);
    }

    private boolean isIndexable(File file) {
        String name = file.getName().toLowerCase();
        return !file.isDirectory() && 
               !name.startsWith(".") && 
               !name.contains("node_modules") && 
               !name.contains("target") && 
               !name.contains(".git") &&
               (name.endsWith(".java") || name.endsWith(".js") || name.endsWith(".ts") || 
                name.endsWith(".py") || name.endsWith(".md") || name.endsWith(".txt") || 
                name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".yml") || 
                name.endsWith(".yaml") || name.endsWith(".html") || name.endsWith(".css"));
    }
}
