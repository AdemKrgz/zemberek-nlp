package zemberek.corpus;

import com.google.common.hash.Hashing;
import zemberek.core.text.TextConsumer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WebCorpus {
    String source;
    String id;

    List<WebDocument> pages = new ArrayList<>();

    public WebCorpus(String source, String id, List<WebDocument> pages) {
        this.source = source;
        this.id = id;
        this.pages = pages;
    }

    public WebCorpus(String source, String id) {
        this.source = source;
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public String getId() {
        return id;
    }

    public List<WebDocument> getPages() {
        return pages;
    }

    public void addDocuments(Collection<WebDocument> documents) {
        pages.addAll(documents);
    }

    public static List<WebDocument> loadDocuments(Path corpusFile) throws IOException {
        List<String> allLines = Files.readAllLines(corpusFile, StandardCharsets.UTF_8);

        List<WebDocument> pages = new ArrayList<>(allLines.size() / 10);

        TextConsumer textConsumer = new TextConsumer(allLines);
        textConsumer.moveUntil(s -> s.startsWith("<doc id="));
        while (!textConsumer.finished()) {
            String meta = textConsumer.current();
            textConsumer.advance();
            List<String> pageData = textConsumer.moveUntil(s -> s.startsWith("<doc id="));
            WebDocument e = WebDocument.fromText(meta, pageData);
            if (e != null) {
                pages.add(e);
            }
        }
        return pages;
    }

    public int count() {
        return pages.size();
    }

    @Override
    public String toString() {
        return source + "-" + id;
    }

    public int totalPageLineCount() {
        int total = 0;
        for (WebDocument page : pages) {
            for (String line : page.lines) {
                if (line.length() == 0)
                    continue;
                total++;
            }
        }
        return total;
    }

    public int uniquePageLineCount() {

        Set<Long> hashes = new HashSet<>(100000);
        for (WebDocument page : pages) {
            for (String line : page.lines) {
                hashes.add(Hashing.murmur3_128().hashUnencodedChars(line).asLong());
            }
        }
        return hashes.size();
    }

    public void saveToDir(Path outRoot, boolean onlyContent) throws IOException {

        Path subDir = outRoot.resolve(source);
        Files.createDirectories(subDir);
        save(subDir.resolve(id), onlyContent);
    }

    public void save(Path outFile, boolean onlyContent) throws IOException {

        try (PrintWriter p = new PrintWriter(outFile.toFile(), "utf-8")) {
            for (WebDocument page : pages) {
                if (!onlyContent) {
                    p.println(page.getDocumentHeader());
                }
                p.println(page.content());
                if (!onlyContent) {
                    p.println("</doc>");
                }
            }
        }
    }

}

